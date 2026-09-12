package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * System configuration for payment source listeners (bKash, Nagad, Rocket, Upay),
 * package names, keywords, and backend endpoint details.
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
    val autoSyncEnabled: Boolean = true
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
}
