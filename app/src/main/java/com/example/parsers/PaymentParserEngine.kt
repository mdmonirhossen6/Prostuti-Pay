package com.example.parsers

import com.example.models.PaymentConfig
import com.example.models.PaymentParseResult

/**
 * Orchestrator coordinating all payment parsers with configuration validation.
 */
class PaymentParserEngine(
    private val parsers: List<PaymentParser> = listOf(
        BkashParser(),
        NagadParser(),
        RocketParser(),
        UpayParser()
    )
) {
    /**
     * Parses raw message or notification text into a validated PaymentParseResult.
     * Respects enabled methods in PaymentConfig.
     */
    fun parse(
        text: String,
        packageName: String? = null,
        sender: String? = null,
        source: String = "sms",
        config: PaymentConfig = PaymentConfig()
    ): PaymentParseResult? {
        if (text.isBlank()) return null

        // 1. Identify which parsers can handle this based on package name, sender, or content keywords
        for (parser in parsers) {
            if (!config.isMethodEnabled(parser.method)) continue

            // Check if sender matches configured keywords
            val senderKeywords = config.getKeywordsForMethod(parser.method)
            val senderMatches = sender != null && senderKeywords.any { kw ->
                sender.contains(kw, ignoreCase = true)
            }

            // Check if package name matches configured packages
            val packages = config.getPackagesForMethod(parser.method)
            val packageMatches = packageName != null && packages.any { pkg ->
                packageName.equals(pkg, ignoreCase = true)
            }

            if (senderMatches || packageMatches || parser.canParse(text, packageName, sender)) {
                val result = parser.parse(text, source)
                if (result != null && result.transactionId.isNotBlank() && result.amount > 0) {
                    return result
                }
            }
        }

        // 2. Fallback: try all enabled parsers sequentially
        for (parser in parsers) {
            if (!config.isMethodEnabled(parser.method)) continue
            val result = parser.parse(text, source)
            if (result != null && result.transactionId.isNotBlank() && result.amount > 0) {
                return result
            }
        }

        return null
    }

    /**
     * Diagnostic parse for Test Mode sandbox: returns parse result regardless of whether method is currently enabled.
     */
    fun testParse(text: String, source: String = "test_mode"): PaymentParseResult? {
        for (parser in parsers) {
            val result = parser.parse(text, source)
            if (result != null && result.transactionId.isNotBlank() && result.amount > 0) {
                return result
            }
        }
        return null
    }
}
