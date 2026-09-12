package com.example.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.PaymentListenerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Native Android service intercepting notifications from registered payment apps
 * (bKash, Nagad, Rocket, Upay).
 */
class PaymentNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "PaymentNotifService"
        @Volatile
        var isConnected: Boolean = false
            private set
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        Log.i(TAG, "NotificationListenerService connected successfully.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        Log.w(TAG, "NotificationListenerService disconnected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return

        // Extract candidate text from notification
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val combinedText = listOf(title, text, bigText).filter { it.isNotBlank() }.joinToString(" ")

        if (combinedText.isBlank()) return

        serviceScope.launch {
            try {
                val app = PaymentListenerApplication.instance
                val config = app.database.paymentConfigDao().getConfigSync() ?: return@launch

                // Quick pre-filter: check if notification matches any configured payment package or keywords
                val isRelevantPackage = config.getAllListeningPackages().any {
                    packageName.equals(it, ignoreCase = true) || packageName.contains(it, ignoreCase = true)
                }
                val hasPaymentKeyword = listOf("bkash", "nagad", "rocket", "upay", "trxid", "txnid", "cash in", "tk", "received")
                    .any { combinedText.contains(it, ignoreCase = true) }

                if (!isRelevantPackage && !hasPaymentKeyword) {
                    return@launch // Ignore all unrelated notifications without overhead
                }

                val parseResult = app.parserEngine.parse(
                    text = combinedText,
                    packageName = packageName,
                    sender = title,
                    source = "notification",
                    config = config
                )

                if (parseResult != null) {
                    Log.i(TAG, "Parsed payment from notification: ${parseResult.method} ৳${parseResult.amount} TrxID: ${parseResult.transactionId}")
                    app.repository.processIncomingPayment(parseResult)
                }
            } catch (e: Exception) {
                // Never crash the listener service
                Log.e(TAG, "Error handling notification in listener: ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isConnected = false
        serviceScope.cancel()
    }
}
