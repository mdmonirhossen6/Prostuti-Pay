package com.example.parsers

import com.example.models.PaymentParseResult
import java.util.regex.Pattern

/**
 * Parser for bKash SMS and App Notifications.
 * Handles "You have received Tk...", "Cash In Tk...", "Payment received Tk...", "TrxID".
 */
class BkashParser : PaymentParser {
    override val method: String = "bKash"

    companion object {
        // Amount and Sender patterns
        private val RECEIVED_PATTERN = Pattern.compile(
            "(?:You have received|received|Payment received|Cash In)\\s+(?:Tk|BDT)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+from\\s+([+0-9\\-]+)",
            Pattern.CASE_INSENSITIVE
        )

        // Fallback standalone amount pattern (carefully avoiding "Balance Tk" and "Fee Tk")
        private val STANDALONE_AMOUNT = Pattern.compile(
            "(?:Tk|BDT)\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)(?!\\s*(?:balance|fee))",
            Pattern.CASE_INSENSITIVE
        )

        // Transaction ID
        private val TRX_ID_PATTERN = Pattern.compile(
            "(?:TrxID|TxnID|Txn ID|Trx ID)[:\\s]+([A-Za-z0-9]{6,20})",
            Pattern.CASE_INSENSITIVE
        )

        // Reference
        private val REF_PATTERN = Pattern.compile(
            "(?:Ref|Reference)[:\\s]+([^\\.,\\n;]+)",
            Pattern.CASE_INSENSITIVE
        )
    }

    override fun canParse(text: String, packageName: String?, sender: String?): Boolean {
        if (packageName != null && (packageName.contains("bkash", ignoreCase = true))) return true
        if (sender != null && (sender.contains("bkash", ignoreCase = true) || sender == "16247")) return true
        return text.contains("bkash", ignoreCase = true) ||
                (text.contains("TrxID", ignoreCase = true) && text.contains("received", ignoreCase = true))
    }

    override fun parse(text: String, source: String): PaymentParseResult? {
        // 1. Extract Transaction ID
        val trxMatcher = TRX_ID_PATTERN.matcher(text)
        val rawTrxId = if (trxMatcher.find()) trxMatcher.group(1) else null
        val normalizedTrxId = Normalizer.normalizeTransactionId(rawTrxId)

        if (normalizedTrxId.isBlank()) {
            return null // Critical rule: No TrxID = cannot approve
        }

        // 2. Extract Amount & Sender
        var amount = 0.0
        var senderNumber = ""

        val receivedMatcher = RECEIVED_PATTERN.matcher(text)
        if (receivedMatcher.find()) {
            amount = Normalizer.parseAmount(receivedMatcher.group(1))
            senderNumber = Normalizer.normalizePhoneNumber(receivedMatcher.group(2))
        } else {
            // Check standalone amount
            val amountMatcher = STANDALONE_AMOUNT.matcher(text)
            if (amountMatcher.find()) {
                amount = Normalizer.parseAmount(amountMatcher.group(1))
            }
        }

        if (amount <= 0.0) {
            return null
        }

        // 3. Extract Reference if available
        val refMatcher = REF_PATTERN.matcher(text)
        val reference = if (refMatcher.find()) refMatcher.group(1)?.trim() else null

        return PaymentParseResult(
            method = method,
            transactionId = normalizedTrxId,
            amount = amount,
            sender = senderNumber,
            receiver = null,
            reference = reference,
            timestamp = System.currentTimeMillis(),
            source = source,
            rawText = text
        )
    }
}
