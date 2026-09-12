package com.example.parsers

import com.example.models.PaymentParseResult
import java.util.regex.Pattern

/**
 * Parser for Nagad SMS and Notifications.
 * Handles patterns such as:
 * "Cash In received. Amount: Tk 500.00, Sender: 01712345678, TxnID: 72N0ABCD, Balance: Tk 1,500.00"
 * "You have received Tk 650.00 from 01812345678. TxnID: NAGAD8912. Balance: Tk 2,150.00. Ref: P123"
 */
class NagadParser : PaymentParser {
    override val method: String = "Nagad"

    companion object {
        // Structured Nagad format: Amount: Tk 500.00
        private val STRUCTURED_AMOUNT = Pattern.compile(
            "Amount[:\\s]+(?:Tk|BDT)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        )

        // Narrative format: received Tk 500 from 01...
        private val NARRATIVE_RECEIVED = Pattern.compile(
            "(?:received|Cash In)\\s+(?:Tk|BDT)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+from\\s+([+0-9\\-]+)",
            Pattern.CASE_INSENSITIVE
        )

        private val SENDER_PATTERN = Pattern.compile(
            "Sender[:\\s]+([+0-9\\-]+)",
            Pattern.CASE_INSENSITIVE
        )

        private val TXN_ID_PATTERN = Pattern.compile(
            "(?:TxnID|Txn ID|TrxID|TxID)[:\\s]+([A-Za-z0-9]{6,20})",
            Pattern.CASE_INSENSITIVE
        )

        private val REF_PATTERN = Pattern.compile(
            "(?:Ref|Reference)[:\\s]+([^\\.,\\n;]+)",
            Pattern.CASE_INSENSITIVE
        )
    }

    override fun canParse(text: String, packageName: String?, sender: String?): Boolean {
        if (packageName != null && packageName.contains("nagad", ignoreCase = true)) return true
        if (sender != null && (sender.contains("nagad", ignoreCase = true) || sender == "16167")) return true
        return text.contains("nagad", ignoreCase = true) ||
                (text.contains("TxnID", ignoreCase = true) && text.contains("Sender:", ignoreCase = true))
    }

    override fun parse(text: String, source: String): PaymentParseResult? {
        val txnMatcher = TXN_ID_PATTERN.matcher(text)
        val rawTxnId = if (txnMatcher.find()) txnMatcher.group(1) else null
        val normalizedTxnId = Normalizer.normalizeTransactionId(rawTxnId)

        if (normalizedTxnId.isBlank()) return null

        var amount = 0.0
        var senderNumber = ""

        // Try structured format first
        val structAmountMatcher = STRUCTURED_AMOUNT.matcher(text)
        if (structAmountMatcher.find()) {
            amount = Normalizer.parseAmount(structAmountMatcher.group(1))
            val senderMatcher = SENDER_PATTERN.matcher(text)
            if (senderMatcher.find()) {
                senderNumber = Normalizer.normalizePhoneNumber(senderMatcher.group(1))
            }
        } else {
            // Try narrative format
            val narrativeMatcher = NARRATIVE_RECEIVED.matcher(text)
            if (narrativeMatcher.find()) {
                amount = Normalizer.parseAmount(narrativeMatcher.group(1))
                senderNumber = Normalizer.normalizePhoneNumber(narrativeMatcher.group(2))
            }
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
