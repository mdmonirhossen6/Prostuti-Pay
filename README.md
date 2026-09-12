# Prostuti Payment Listener

**Prostuti Payment Listener** is a dedicated Android helper application designed to run on a dedicated payment collection phone for the Prostuti educational platform.

> **CRITICAL ARCHITECTURAL PRINCIPLE:**
> The Android app **NEVER directly approves payments**. The app's sole responsibility is to capture, normalize, and securely transmit raw transaction evidence to a backend Firebase Cloud Function. Final verification, idempotent deduplication, and user premium plan activation occur strictly on the backend.

---

## 1. System Architecture & Flow

```
+---------------------------+        +---------------------------+
|  Incoming SMS (SIM Card)  |        |  Mobile App Notification  |
|  bKash / Nagad / Rocket   |        |  bKash, Nagad, Upay, etc. |
+-------------+-------------+        +-------------+-------------+
              |                                    |
              v                                    v
     [ PaymentSmsReceiver ]             [ PaymentNotificationListenerService ]
              |                                    |
              +-----------------+------------------+
                                |
                                v
                   [ PaymentParserEngine ]
       - BkashParser, NagadParser, RocketParser, UpayParser
       - Phone Normalization (+880 / 01X...)
       - TrxID Uppercase Normalization
       - Separate Amount vs. Balance checks
                                |
                                v
                   [ Local Room Database Queue ]
       - Deduplication by SHA-256 fingerprint
       - Offline cache with exponential backoff
                                |
                                v
             [ Firebase Cloud Function Endpoint ]
             `processPaymentListenerEvent`
                                |
              +-----------------+------------------+
              |                                    |
              v                                    v
   [ 1 Candidate Matched ]               [ 0 or >1 Candidates ]
   - TrxID exact match                   - 0 candidates: `unmatched`
   - Sender phone match                    (Saved for delayed matching)
   - Amount >= expectedAmount            - >1 candidates: `ambiguous`
   - Method normalized match               (Flagged for admin review)
              |
              v
   [ Auto-Approve Payment ]
   - `payment_requests.status = 'approved'`
   - `users/{uid}.isPremium = true`
   - Audit trail recorded
```

---

## 2. Supported Payment Methods & Patterns

| Method | Official Shortcode | Pattern Example |
| :--- | :--- | :--- |
| **bKash** | `16247` / `bKash` | `You have received Tk 500.00 from 01712345678. Fee Tk 0.00. Balance Tk 1,500.00. TrxID BKA8921XYZ` |
| **Nagad** | `16167` / `Nagad` | `Cash In received. Amount: Tk 650.00, Sender: 01812345678, TxnID: 72N0ABCD, Balance: Tk 2,150.00` |
| **Rocket** | `16216` / `Rocket` | `Tk500.00 received from 01712345678-9 to A/C 01987654321-0. Balance Tk 2,500.00, TxnId: 198273645` |
| **Upay** | `16268` / `Upay` | `Received Tk 500.00 from 01712345678. TxnID: UP789123. Balance Tk 1,200.00` |

---

## 3. Firebase Cloud Function Reference Implementation

Deploy the following Node.js Cloud Function to your Firebase project:

```javascript
const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();
const db = admin.firestore();

exports.processPaymentListenerEvent = functions
  .region("asia-south1")
  .https.onRequest(async (req, res) => {
    // 1. Validate service authorization
    const authHeader = req.headers.authorization || "";
    const apiKey = req.headers["x-api-key"] || "";
    if (!authHeader.includes("prostuti_listener_secure_key") && apiKey !== "prostuti_listener_secure_key") {
      return res.status(401).json({ success: false, message: "Unauthorized listener device" });
    }

    const { fingerprint, method, transactionId, amount, sender, receiver, reference, source, rawText, timestamp } = req.body;

    if (!transactionId || !amount) {
      return res.status(400).json({ success: false, message: "Missing TrxID or payable amount" });
    }

    const cleanTrxId = transactionId.trim().toUpperCase();
    const cleanSender = (sender || "").replace(/[^0-9]/g, "").slice(-11);

    // 2. Check duplicate in payment_listener_events
    const eventRef = db.collection("payment_listener_events").doc(fingerprint);
    const existingEvent = await eventRef.get();
    if (existingEvent.exists) {
      return res.json({ success: true, status: existingEvent.data().status, message: "Duplicate transaction event" });
    }

    // 3. Search for pending payment_requests
    const pendingQuery = await db.collection("payment_requests")
      .where("transactionId", "==", cleanTrxId)
      .where("status", "==", "pending")
      .get();

    let decisionStatus = "unmatched";
    let matchedRequestId = null;
    let candidateIds = [];

    if (pendingQuery.empty) {
      decisionStatus = "unmatched";
    } else if (pendingQuery.size > 1) {
      decisionStatus = "ambiguous";
      candidateIds = pendingQuery.docs.map(doc => doc.id);
    } else {
      const candidateDoc = pendingQuery.docs[0];
      const requestData = candidateDoc.data();
      const reqSender = (requestData.fromNumber || "").replace(/[^0-9]/g, "").slice(-11);
      const expectedAmount = requestData.finalPrice || requestData.price || 0;

      const senderMatches = !cleanSender || !reqSender || cleanSender === reqSender;
      const methodMatches = (requestData.method || "").toLowerCase() === (method || "").toLowerCase();
      const amountMatches = Number(amount) >= Number(expectedAmount);

      if (senderMatches && methodMatches && amountMatches) {
        // Auto-approve in atomic transaction
        await db.runTransaction(async (transaction) => {
          transaction.update(candidateDoc.ref, {
            status: "approved",
            verificationSource: "auto_sms_listener",
            verifiedAmount: amount,
            verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
            listenerFingerprint: fingerprint
          });

          // Activate user premium plan
          if (requestData.userId) {
            const userRef = db.collection("users").doc(requestData.userId);
            transaction.set(userRef, {
              isPremium: true,
              plan: requestData.plan,
              premiumActivatedAt: admin.firestore.FieldValue.serverTimestamp()
            }, { merge: true });
          }
        });

        decisionStatus = "approved";
        matchedRequestId = candidateDoc.id;
        candidateIds = [candidateDoc.id];
      } else {
        decisionStatus = "rejected";
      }
    }

    // 4. Save audit trail in payment_listener_events
    await eventRef.set({
      fingerprint,
      method,
      transactionId: cleanTrxId,
      amount: Number(amount),
      sender: cleanSender,
      receiver: receiver || null,
      reference: reference || null,
      source,
      rawText,
      receivedAt: timestamp || Date.now(),
      status: decisionStatus,
      matchedPaymentRequestId: matchedRequestId,
      candidateCount: candidateIds.length,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return res.json({
      success: true,
      status: decisionStatus,
      transactionId: cleanTrxId,
      matchedRequestId,
      candidateCount: candidateIds.length,
      candidateIds
    });
  });
```

---

## 4. Android Device Setup (Dedicated Phone)

1. **Install APK** onto the dedicated payment device.
2. **Grant Notification Access**:
   - Go to **Settings > Apps & Notifications > Special App Access > Notification Access**.
   - Enable **Prostuti Payment Listener** (or tap the in-app 1-click shortcut on Dashboard).
3. **Grant SMS Permissions**:
   - Allow `RECEIVE_SMS` and `READ_SMS` when prompted by the app.
4. **Disable Battery Optimization**:
   - In device battery settings, set this app to **"Unrestricted" / "Don't Optimize"** to ensure background listeners remain responsive 24/7.
5. **Configure Endpoint**:
   - Navigate to the **Settings** tab.
   - Enter your deployed Cloud Function URL and Secret API Key.
   - Verify by running the **Test Mode Sandbox**.
