# Prostuti Payment Listener

**Prostuti Payment Listener** is a dedicated Android helper application engineered to run 24/7 on a designated payment phone for the Prostuti educational platform.

> [!IMPORTANT]
> **CRITICAL ARCHITECTURAL PRINCIPLE & SECURITY BOUNDARY:**
> The Android application **NEVER directly approves, activates, extends, or modifies a Prostuti premium subscription**. The Android application's sole responsibility is to detect incoming payment notifications and SMS, parse transaction details (TrxID, amount, sender), and securely transmit raw evidence to an authoritative Firebase Cloud Function.
> 
> All subscription activations, idempotent deduplication, payment tolerance validation, and user database mutations are strictly performed server-side.

---

## 1. System Architecture & Flow

```
+------------------------------------+        +------------------------------------+
|     Incoming SMS (SIM 1 / SIM 2)   |        |    Mobile App Push Notification    |
|   bKash (16247), Nagad (16167),    |        |    bKash, Nagad, Rocket, Upay      |
|   Rocket (16216), Upay (16268)     |        |    (StatusBarNotification event)   |
+-----------------+------------------+        +-----------------+------------------+
                  |                                             |
                  v                                             v
         [ PaymentSmsReceiver ]             [ PaymentNotificationListenerService ]
                  |                                             |
                  +----------------------+----------------------+
                                         |
                                         v
                            [ PaymentParserEngine ]
            - Method Parsers: bKash, Nagad, Rocket, Upay
            - Bengali Numeral Normalization (০-৯ -> 0-9, ৳১,৫০০ -> 1500.00)
            - Phone Number Normalization (+8801X / 8801X / 01X -> 01XXXXXXXXX)
            - Transaction ID Sanitization (Prefix removal, whitespace trim, UPPERCASE)
            - Balance vs. Payable Amount isolation
            - SHA-256 Deduplication Fingerprint generation
                                         |
                                         v
                         [ Room Database Offline Queue ]
            - Event stored immediately with status = 'parsed'
            - 9-step exponential backoff retry scheduler (5s -> 1hr)
            - Chronological JSON audit timeline tracking
                                         |
                                         v
                     [ Firebase Cloud Function Endpoint ]
                     POST /processPaymentListenerEvent
                     Authorization: Bearer <CONFIGURED_API_KEY>
                                         |
                  +----------------------+----------------------+
                  |                                             |
                  v                                             v
       [ Exact / Tolerance Match ]                   [ Ambiguous / Unmatched ]
    - Pending payment_request found               - 0 requests: status = 'unmatched'
    - TrxID exact match                             (Stored for delayed user submit)
    - Sender phone matches or is blank            - >1 requests: status = 'ambiguous'
    - Amount within tolerance:                      (Flagged for admin manual review)
      (expectedPrice - tol) <= amount             - Out of tolerance: status = 'rejected'
      amount <= (expectedPrice + tol)               (Variance exceeded allowed limits)
                  |
                  v
       [ Server-Side Atomic Batch ]
    - payment_requests.status = 'approved'
    - users/{uid}.isPremium = true
    - payment_listener_events recorded
```

---

## 2. Payment Amount Tolerance Feature

Students in Bangladesh frequently face cashout deductions (1.49%–1.85%), minor round-offs, or manual input errors when making manual mobile financial service (MFS) payments. A student purchasing a ৳1000 course might send ৳995 or ৳1000 minus charges.

### Tolerance Specification
* **Absolute Currency Unit**: All tolerance values are configured in absolute Bangladeshi Taka (৳), not percentages.
* **Server-Authoritative**: While the Android app displays tolerance previews for operators in the sandbox and configuration center, the Firebase backend enforces the final ceiling (`maxToleranceCeiling = ৳50.00`) and resolves plan-specific rules.
* **Calculation Formula**:
  $$\text{minAcceptedAmount} = \max(0, \text{expectedAmount} - \text{tolerance})$$
  $$\text{maxAcceptedAmount} = \text{expectedAmount} + \text{tolerance}$$
* **Audit Trail**: Every transaction records `expectedAmount`, `tolerance`, `amountDifference`, and `evaluationDecision` in its immutable audit timeline.

---

## 3. Supported Providers & Canonical Patterns

| Provider | Sender Identity / Shortcode | Example Raw Text |
| :--- | :--- | :--- |
| **bKash** | `16247`, `bKash` | `You have received Tk 500.00 from 01712345678. Fee Tk 0.00. Balance Tk 1,500.00. TrxID BKA8921XYZ at 12/09/2026 14:30` |
| **Nagad** | `16167`, `Nagad` | `Cash In received. Amount: Tk 650.00, Sender: 01812345678, TxnID: 72N0ABCD, Balance: Tk 2,150.00, Time: 12/09/2026 14:32` |
| **Rocket** | `16216`, `Rocket` | `Tk500.00 received from 01712345678-9 to A/C 01987654321-0. Fee Tk 0.00, Balance Tk 2,500.00, TxnId: 198273645 on 12-Sep-2026` |
| **Upay** | `16268`, `Upay` | `Received Tk 500.00 from 01712345678. TxnID: UP789123. Balance Tk 1,200.00. Fee Tk 0.00` |

---

## 4. Production Firebase Cloud Function

Deploy the following production-grade Cloud Function in your Firebase environment (`index.js`):

```javascript
const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();
const db = admin.firestore();

// Maximum safety tolerance allowed on the server
const HARD_MAX_TOLERANCE_CEILING = 50.0;

exports.processPaymentListenerEvent = functions
  .region("asia-south1")
  .runWith({ timeoutSeconds: 30, memory: "256MB" })
  .https.onRequest(async (req, res) => {
    if (req.method !== "POST") {
      return res.status(405).json({ success: false, message: "Method Not Allowed" });
    }

    // 1. Authenticate Request
    const authHeader = req.headers.authorization || "";
    const apiKeyHeader = req.headers["x-api-key"] || "";
    const expectedKey = process.env.PAYMENT_LISTENER_SECRET || "prostuti_listener_secure_key";

    if (!authHeader.includes(expectedKey) && apiKeyHeader !== expectedKey) {
      return res.status(401).json({ success: false, message: "Unauthorized listener device" });
    }

    const {
      fingerprint,
      method,
      transactionId,
      amount,
      sender,
      receiver,
      reference,
      source,
      rawText,
      timestamp,
      toleranceConfig
    } = req.body;

    if (!transactionId || amount == null || isNaN(Number(amount))) {
      return res.status(400).json({ success: false, message: "Invalid payload: missing TrxID or amount" });
    }

    const cleanTrxId = String(transactionId).trim().toUpperCase();
    const cleanSender = (sender || "").replace(/[^0-9]/g, "").slice(-11);
    const parsedAmount = Number(amount);

    // 2. Deduplication Check
    const eventRef = db.collection("payment_listener_events").doc(fingerprint);
    const existingDoc = await eventRef.get();
    if (existingDoc.exists) {
      const data = existingDoc.data();
      return res.json({
        success: true,
        status: data.status,
        transactionId: cleanTrxId,
        matchedRequestId: data.matchedPaymentRequestId,
        message: "Duplicate event acknowledged idempotently."
      });
    }

    // 3. Search Pending Requests
    const pendingQuery = await db.collection("payment_requests")
      .where("transactionId", "==", cleanTrxId)
      .where("status", "==", "pending")
      .get();

    let decisionStatus = "unmatched";
    let matchedRequestId = null;
    let candidateIds = [];
    let expectedAmount = null;
    let appliedTolerance = 0.0;
    let minAccepted = parsedAmount;
    let maxAccepted = parsedAmount;
    let difference = 0.0;
    let decisionNote = "";

    if (pendingQuery.empty) {
      decisionStatus = "unmatched";
      decisionNote = "No pending payment_request found for TrxID. Saved for delayed user reconciliation.";
    } else if (pendingQuery.size > 1) {
      decisionStatus = "ambiguous";
      candidateIds = pendingQuery.docs.map(d => d.id);
      decisionNote = `Found ${pendingQuery.size} pending requests matching TrxID. Escalated to manual admin review.`;
    } else {
      const reqDoc = pendingQuery.docs[0];
      const reqData = reqDoc.data();
      candidateIds = [reqDoc.id];
      expectedAmount = Number(reqData.finalPrice || reqData.price || 0);

      // Resolve Tolerance
      const reqPlan = reqData.plan || reqData.packageId || "default";
      let requestedTol = Number(toleranceConfig?.globalTolerance || 0);
      if (toleranceConfig?.perPlanOverrides && toleranceConfig.perPlanOverrides[reqPlan] != null) {
        requestedTol = Number(toleranceConfig.perPlanOverrides[reqPlan]);
      }
      appliedTolerance = Math.min(requestedTol, HARD_MAX_TOLERANCE_CEILING);

      minAccepted = Math.max(0, expectedAmount - appliedTolerance);
      maxAccepted = expectedAmount + appliedTolerance;
      difference = parsedAmount - expectedAmount;

      const reqSender = (reqData.fromNumber || "").replace(/[^0-9]/g, "").slice(-11);
      const senderMatches = !cleanSender || !reqSender || cleanSender === reqSender;
      const methodMatches = !reqData.method || !method || reqData.method.toLowerCase() === method.toLowerCase();
      const amountMatches = parsedAmount >= minAccepted && parsedAmount <= maxAccepted;

      if (senderMatches && methodMatches && amountMatches) {
        // Execute Atomic Verification Batch
        const batch = db.batch();

        batch.update(reqDoc.ref, {
          status: "approved",
          verificationSource: `auto_${source || "sms"}_listener`,
          verifiedAmount: parsedAmount,
          expectedAmount: expectedAmount,
          toleranceApplied: appliedTolerance,
          amountDifference: difference,
          verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
          listenerFingerprint: fingerprint
        });

        if (reqData.userId) {
          const userRef = db.collection("users").doc(reqData.userId);
          batch.set(userRef, {
            isPremium: true,
            premiumPlan: reqData.plan || "standard",
            premiumActivatedAt: admin.firestore.FieldValue.serverTimestamp(),
            activatedByPaymentRequest: reqDoc.id
          }, { merge: true });
        }

        await batch.commit();
        decisionStatus = "approved";
        matchedRequestId = reqDoc.id;
        decisionNote = `Auto-approved for user ${reqData.userId}. Variance: ৳${difference.toFixed(2)}`;
      } else {
        decisionStatus = "rejected";
        decisionNote = `Verification failed: Sender=${senderMatches}, Method=${methodMatches}, Range=[৳${minAccepted}-৳${maxAccepted}]`;
      }
    }

    // 4. Save Event Evidence & Audit Log
    const auditRecord = {
      timestamp: Date.now(),
      status: decisionStatus,
      decisionNote,
      expectedAmount,
      appliedTolerance,
      receivedAmount: parsedAmount
    };

    await eventRef.set({
      fingerprint,
      method: method || "Unknown",
      transactionId: cleanTrxId,
      amount: parsedAmount,
      sender: cleanSender,
      receiver: receiver || null,
      reference: reference || null,
      source: source || "sms",
      rawText: rawText || "",
      receivedAt: timestamp || Date.now(),
      status: decisionStatus,
      matchedPaymentRequestId: matchedRequestId,
      candidateCount: candidateIds.length,
      candidateIds,
      expectedAmount,
      tolerance: appliedTolerance,
      minimumAcceptedAmount: minAccepted,
      maximumAcceptedAmount: maxAccepted,
      amountDifference: difference,
      auditLog: [auditRecord],
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return res.json({
      success: true,
      status: decisionStatus,
      transactionId: cleanTrxId,
      matchedRequestId,
      candidateCount: candidateIds.length,
      candidateIds,
      expectedAmount,
      receivedAmount: parsedAmount,
      tolerance: appliedTolerance,
      minimumAcceptedAmount: minAccepted,
      maximumAcceptedAmount: maxAccepted,
      amountDifference: difference,
      message: decisionNote
    });
  });
```

---

## 5. Dedicated Payment Phone Setup Guide

Follow these 6 steps to configure the dedicated payment collection device:

1. **Keep Phone Powered & Connected**:
   - Keep device permanently connected to a charger.
   - Insert SIM cards configured with official MFS merchant or personal accounts (bKash, Nagad, Rocket).
2. **Grant Notification Access**:
   - Open **Settings > Apps & Notifications > Special App Access > Notification Access**.
   - Enable **Prostuti Payment Listener**.
3. **Grant SMS Permissions**:
   - Allow `RECEIVE_SMS` and `READ_SMS` at runtime.
4. **Disable Battery Optimization (Crucial)**:
   - Go to **Settings > Battery > Battery Optimization**.
   - Set **Prostuti Payment Listener** to **Don't Optimize / Unrestricted**.
   - On Xiaomi/Oppo/Vivo/Samsung devices: Enable **Auto-start**, lock the app in the recent apps tray, and disable OEM battery killers.
5. **Configure Cloud Backend**:
   - Open the **Config** tab in the app.
   - Enter your Firebase Cloud Function Endpoint URL and Secret API Key.
   - Set your desired payment tolerance limits (e.g. ±৳5.00).
6. **Verify with Sandbox**:
   - Go to **Test Mode** tab.
   - Tap "Test Parse" and "Simulate Backend Match" to verify that parsing, tolerance math, and backend reporting work smoothly.

---

## 6. Troubleshooting FAQ

* **Q: SMS is received on SIM but app doesn't detect it?**
  * *Fix*: Ensure SMS permissions (`RECEIVE_SMS`, `READ_SMS`) are granted in Android Settings. On Android 10+, ensure the app is excluded from battery saver sleep policies.
* **Q: Transaction is marked "ambiguous"?**
  * *Fix*: This occurs when multiple students submitted the same TrxID, or when the payment amount differs from the expected plan price by more than the allowed tolerance ceiling. Inspect the transaction in the **Transactions** tab to review the audit trail and execute a manual override if legitimate.
* **Q: App shows "Network unavailable: queued"?**
  * *Fix*: The app features full offline queueing. Events are safely retained in the local encrypted Room database and will automatically retry with exponential backoff (up to 9 attempts) when connectivity resumes.
