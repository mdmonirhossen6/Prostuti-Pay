package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONObject

/**
 * System configuration for payment source listeners (bKash, Nagad, Rocket, Upay),
 * package names, keywords, tolerance rules, and backend endpoint details.
 */
@Entity(tableName = "payment_config")
data class PaymentConfig(
    @PrimaryKey
    val id: Int = 1,
    // Method toggles
    val bkashEnabled: Boolean = true,
    val nagadEnabled: Boolean = true,
    val rocketEnabled: Boolean = true,
    val upayEnabled: Boolean = true,

    // Configurable keywords for SMS sender or notification titles
    val bkashKeywords: String = "bKash, bkash, 16247",
    val nagadKeywords: String = "Nagad, nagad, 16167",
    val rocketKeywords: String = "Rocket, DBBL, 16216",
    val upayKeywords: String = "Upay, upay, 16268",

    // Configurable package names for NotificationListenerService
    val bkashPackages: String = "com.bKash.customerapp, com.bkash.businessapp",
    val nagadPackages: String = "com.konasl.nagad",
    val rocketPackages: String = "com.dbbl.mbs.apps.main, com.dbbl.nexus.pay",
    val upayPackages: String = "bd.com.upay.customer",

    // Backend endpoint configuration (Cloud Function HTTPS callable or custom URL)
    val backendUrl: String = "https://asia-south1-prostuti-app.cloudfunctions.net/processPaymentListenerEvent",
    val apiKey: String = "prostuti_listener_secure_key",
    val autoSyncEnabled: Boolean = true,

    // Payment Price Tolerance configuration
    val toleranceEnabled: Boolean = true,
    val globalTolerance: Double = 10.0, // Default ৳10
    val minTolerance: Double = 0.0,     // 0 = exact matching
    val maxTolerance: Double = 100.0,   // Strict server-enforced safety limit
    val perPlanToleranceJson: String = "{}", // e.g. {"1 Month": 10.0, "3 Months": 15.0}
    val perMethodToleranceJson: String = "{}", // e.g. {"bKash": 10.0, "Nagad": 10.0}

    // Device & Environment settings
    val deviceName: String = "Dedicated Payment Phone 01",
    val deviceId: String = "DEV-PROSTUTI-01",
    val environment: String = "production", // "production" or "sandbox"
    val requestTimeoutSeconds: Int = 15,
    val retryMaxAttempts: Int = 9, // 5s, 15s, 30s, 1m, 2m, 5m, 10m, 15m, 30m
    val firebaseProjectId: String = "prostuti-app",

    // In-app Setup Wizard progress
    val setupWizardCompleted: Boolean = false,
    val setupCurrentStep: Int = 1
) {
    fun isMethodEnabled(method: String): Boolean {
        return when (method.lowercase()) {
            "bkash" -> bkashEnabled
            "nagad" -> nagadEnabled
            "rocket" -> rocketEnabled
            "upay" -> upayEnabled
            else -> false
        }
    }

    fun getKeywordsForMethod(method: String): List<String> {
        val raw = when (method.lowercase()) {
            "bkash" -> bkashKeywords
            "nagad" -> nagadKeywords
            "rocket" -> rocketKeywords
            "upay" -> upayKeywords
            else -> ""
        }
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getPackagesForMethod(method: String): List<String> {
        val raw = when (method.lowercase()) {
            "bkash" -> bkashPackages
            "nagad" -> nagadPackages
            "rocket" -> rocketPackages
            "upay" -> upayPackages
            else -> ""
        }
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getAllListeningPackages(): Set<String> {
        val result = mutableSetOf<String>()
        if (bkashEnabled) result.addAll(getPackagesForMethod("bkash"))
        if (nagadEnabled) result.addAll(getPackagesForMethod("nagad"))
        if (rocketEnabled) result.addAll(getPackagesForMethod("rocket"))
        if (upayEnabled) result.addAll(getPackagesForMethod("upay"))
        return result
    }

    /**
     * Resolves the authoritative tolerance for a payment request.
     * Hierarchy:
     * 1. Per-plan override (if configured)
     * 2. Per-method override (if configured)
     * 3. Global tolerance
     * Always clamped between minTolerance (0.0) and maxTolerance (100.0).
     */
    fun resolveTolerance(planName: String? = null, methodName: String? = null): Double {
        if (!toleranceEnabled) return 0.0

        // 1. Check per-plan override
        if (!planName.isNullOrBlank()) {
            try {
                val json = JSONObject(perPlanToleranceJson)
                if (json.has(planName)) {
                    val planTol = json.optDouble(planName, -1.0)
                    if (planTol >= 0.0) return planTol.coerceIn(minTolerance, maxTolerance)
                }
            } catch (_: Exception) {}
        }

        // 2. Check per-method override
        if (!methodName.isNullOrBlank()) {
            try {
                val json = JSONObject(perMethodToleranceJson)
                val cleanMethod = methodName.trim().lowercase()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key.trim().lowercase() == cleanMethod) {
                        val methodTol = json.optDouble(key, -1.0)
                        if (methodTol >= 0.0) return methodTol.coerceIn(minTolerance, maxTolerance)
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback to global tolerance clamped within safe limits
        return globalTolerance.coerceIn(minTolerance, maxTolerance)
    }

    /**
     * Calculates the accepted range for an expected subscription price.
     * Formula:
     * minimumAcceptedAmount = max(0.0, expectedPrice - tolerance)
     * maximumAcceptedAmount = expectedPrice + tolerance
     */
    fun calculateAcceptedRange(expectedPrice: Double, tolerance: Double): Pair<Double, Double> {
        val min = kotlin.math.max(0.0, expectedPrice - tolerance)
        val max = expectedPrice + tolerance
        return Pair(min, max)
    }

    /**
     * Validates whether a received payment amount falls within the tolerance range.
     */
    fun isAmountWithinTolerance(receivedAmount: Double, expectedPrice: Double, tolerance: Double): Boolean {
        val (min, max) = calculateAcceptedRange(expectedPrice, tolerance)
        return receivedAmount >= min && receivedAmount <= max
    }

    fun getPerPlanToleranceMap(): Map<String, Double> {
        val map = mutableMapOf<String, Double>()
        try {
            val json = JSONObject(perPlanToleranceJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.optDouble(key, globalTolerance)
            }
        } catch (_: Exception) {}
        return map
    }

    fun getPerMethodToleranceMap(): Map<String, Double> {
        val map = mutableMapOf<String, Double>()
        try {
            val json = JSONObject(perMethodToleranceJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.optDouble(key, globalTolerance)
            }
        } catch (_: Exception) {}
        return map
    }
}
