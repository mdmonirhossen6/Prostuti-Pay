package com.example.parsers

import com.example.models.PaymentParseResult
import java.util.regex.Pattern

/**
 * Parser for Upay (UCB Fintech) SMS and Notifications.
 */
class UpayParser : PaymentParser {
    override val method: String = "Upay"

    companion object {
        private val RECEIVED_PATTERN = Pattern.compile(
            "(?:Received|Cash In)\\s+(?:Tk|BDT)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+from\\s+([+0-9\\-]+)",
            Pattern.CASE_INSENSITIVE
        )

        private val TXN_ID_PATTERN = Pattern.compile(
            "(?:TxnID|TrxID|TxID|Txn ID)[:\\s]+([A-Za-z0-9]{6,20})",
            Pattern.CASE_INSENSITIVE
        )

        private val REF_PATTERN = Pattern.compile(
            "(?:Ref|Reference)[:\\s]+([^\\.,\\n;]+)",
            Pattern.CASE_INSENSITIVE
        )
    }

    override fun canParse(text: String, packageName: String?, sender: String?): Boolean {
        if (packageName != null && packageName.contains("upay", ignoreCase = true)) return true
        if (sender != null && (sender.contains("upay", ignoreCase = true) || sender == "16268")) return true
        return text.contains("upay", ignoreCase = true)
    }

    override fun parse(text: String, source: String): PaymentParseResult? {
        val txnMatcher = TXN_ID_PATTERN.matcher(text)
        val rawTxnId = if (txnMatcher.find()) txnMatcher.group(1) else null
        val normalizedTxnId = Normalizer.normalizeTransactionId(rawTxnId)

        if (normalizedTxnId.isBlank()) return null

        var amount = 0.0
        var senderNumber = ""

        val receivedMatcher = RECEIVED_PATTERN.matcher(text)
        if (receivedMatcher.find()) {
            amount = Normalizer.parseAmount(receivedMatcher.group(1))
            senderNumber = Normalizer.normalizePhoneNumber(receivedMatcher.group(2))
        }

        if (amount <= 0.0) return null

        val refMatcher = REF_PATTERN.matcher(text)
        val reference = if (refMatcher.find()) refMatcher.group(1)?.trim() else null

        return PaymentParseResult(
            method = method,
            transactionId = normalizedTxnId,
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
