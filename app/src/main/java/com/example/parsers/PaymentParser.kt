package com.example.parsers

import com.example.models.PaymentParseResult

/**
 * Interface implemented by each mobile financial service parser (bKash, Nagad, Rocket, Upay).
 */
interface PaymentParser {
    val method: String

    /**
     * Determines whether this parser can handle the provided text, package name, or sender.
     */
    fun canParse(text: String, packageName: String? = null, sender: String? = null): Boolean

    /**
     * Extracts normalized transaction data from the text.
     * Returns null if transaction ID or amount cannot be reliably extracted.
     */
    fun parse(text: String, source: String = "sms"): PaymentParseResult?
}
