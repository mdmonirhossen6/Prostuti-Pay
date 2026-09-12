package com.example.parsers

import com.example.models.PaymentParseResult
import java.util.regex.Pattern

/**
 * Parser for Dutch-Bangla Bank Rocket SMS and notifications.
 * Handles "Tk500.00 received from 017... to A/C 019... TxnId: ...", "Received Tk 500 from ..."
 */
class RocketParser : PaymentParser {
    override val method: String = "Rocket"

    companion object {
        // Amount received from ...
        private val RECEIVED_PATTERN = Pattern.compile(
            "(?:Tk|BDT)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+received\\s+from\\s+([+0-9\\-]+)",
            Pattern.CASE_INSENSITIVE
        )

        private val ALT_RECEIVED = Pattern.compile(
            "received\\s+(?:Tk|BDT)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+from\\s+([+0-9\\-]+)",
            Pattern.CASE_INSENSITIVE
        )

        // Receiver account "to A/C 01987654321-0"
        private val RECEIVER_PATTERN = Pattern.compile(
            "to\\s+(?:A/C|A\\/C|Account)\\s+([+0-9\\-]+)",
            Pattern.CASE_INSENSITIVE
        )

        private val TXN_ID_PATTERN = Pattern.compile(
            "(?:TxnId|TxnID|Txn ID|TrxID)[:\\s]+([A-Za-z0-9]{6,20})",
            Pattern.CASE_INSENSITIVE
        )
    }

    override fun canParse(text: String, packageName: String?, sender: String?): Boolean {
        if (packageName != null && (packageName.contains("dbbl", ignoreCase = true) || packageName.contains("rocket", ignoreCase = true))) return true
        if (sender != null && (sender.contains("rocket", ignoreCase = true) || sender.contains("dbbl", ignoreCase = true) || sender == "16216")) return true
        return text.contains("rocket", ignoreCase = true) ||
                text.contains("dbbl", ignoreCase = true) ||
                (text.contains("received from", ignoreCase = true) && text.contains("to A/C", ignoreCase = true))
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
        } else {
            val altMatcher = ALT_RECEIVED.matcher(text)
            if (altMatcher.find()) {
                amount = Normalizer.parseAmount(altMatcher.group(1))
                senderNumber = Normalizer.normalizePhoneNumber(altMatcher.group(2))
            }
        }

        if (amount <= 0.0) return null

        val receiverMatcher = RECEIVER_PATTERN.matcher(text)
        val receiver = if (receiverMatcher.find()) Normalizer.normalizePhoneNumber(receiverMatcher.group(1)) else null

        return PaymentParseResult(
            method = method,
            transactionId = normalizedTxnId,
            amount = amount,
            sender = senderNumber,
            receiver = receiver,
            reference = null,
            timestamp = System.currentTimeMillis(),
            source = source,
            rawText = text
        )
    }
}
