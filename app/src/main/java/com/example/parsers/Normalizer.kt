package com.example.parsers

import java.util.Locale
import java.util.regex.Pattern

/**
 * Normalization utilities for phone numbers, transaction IDs, payment methods, amounts,
 * and Unicode/Bengali numerals.
 */
object Normalizer {

    private val BD_PHONE_REGEX = Pattern.compile("(?:\\+?88)?(01[3-9]\\d{8})")
    private val ROCKET_ACC_REGEX = Pattern.compile("(?:\\+?88)?(01[3-9]\\d{8})(?:-\\d)?")

    private val BENGALI_DIGITS = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    /**
     * Converts Bengali numerals (০-৯) to standard Arabic numerals (0-9).
     */
    fun convertBengaliDigits(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val builder = java.lang.StringBuilder(raw.length)
        for (ch in raw) {
            val converted = BENGALI_DIGITS[ch] ?: ch
            builder.append(converted)
        }
        return builder.toString()
    }

    /**
     * Normalizes Bangladesh phone numbers to canonical 11-digit representation (e.g. "01712345678").
     * Handles +8801..., 8801..., 01... and Rocket 12th check-digit, as well as Bengali numerals.
     */
    fun normalizePhoneNumber(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val asciiDigits = convertBengaliDigits(raw)
        val clean = asciiDigits.replace("[\\s\\-\\(\\)]".toRegex(), "")
        val matcher = BD_PHONE_REGEX.matcher(clean)
        if (matcher.find()) {
            return matcher.group(1) ?: clean
        }
        val rocketMatcher = ROCKET_ACC_REGEX.matcher(clean)
        if (rocketMatcher.find()) {
            return rocketMatcher.group(1) ?: clean
        }
        return clean
    }

    /**
     * Normalizes transaction ID:
     * - trim whitespace
     * - uppercase
     * - remove prefix noise like "TrxID:", "TxnID", colon, trailing period
     * - preserves actual ID characters
     */
    fun normalizeTransactionId(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val asciiDigits = convertBengaliDigits(raw)
        var clean = asciiDigits.trim()
        clean = clean.replace("^(?:trxid|txnid|transid|txid|trx\\s*id|txn\\s*id)[:\\s#]*".toRegex(RegexOption.IGNORE_CASE), "")
        clean = clean.replace("[\\.,;:!]+$".toRegex(), "") // trim trailing punctuation
        return clean.trim().uppercase(Locale.US)
    }

    /**
     * Normalizes payment method name to canonical casing:
     * "bKash", "Nagad", "Rocket", "Upay"
     */
    fun normalizeMethod(raw: String?): String {
        if (raw.isNullOrBlank()) return "Unknown"
        return when (raw.trim().lowercase(Locale.US)) {
            "bkash" -> "bKash"
            "nagad" -> "Nagad"
            "rocket", "dbbl" -> "Rocket"
            "upay" -> "Upay"
            else -> raw.trim()
        }
    }

    /**
     * Safe extraction of currency amount as Double.
     * Converts Bengali digits and removes commas (e.g. "১,৫০০.৫০" -> 1500.50).
     */
    fun parseAmount(raw: String?): Double {
        if (raw.isNullOrBlank()) return 0.0
        val asciiDigits = convertBengaliDigits(raw)
        val clean = asciiDigits.replace(",", "").replace("[^0-9\\.]".toRegex(), "")
        return clean.toDoubleOrNull() ?: 0.0
    }
}
