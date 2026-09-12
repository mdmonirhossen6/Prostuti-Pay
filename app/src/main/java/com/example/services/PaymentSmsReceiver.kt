package com.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.PaymentListenerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for incoming SMS on the dedicated payment SIM card.
 * Efficiently extracts, combines, and submits candidate payment SMS to parser engine.
 */
class PaymentSmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PaymentSmsReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isNullOrEmpty()) {
                    pendingResult.finish()
                    return@launch
                }

                // Group multi-part SMS by sender
                val fullBody = messages.joinToString("") { it.messageBody ?: "" }
                val sender = messages.first().originatingAddress.orEmpty()

                if (fullBody.isBlank()) {
                    pendingResult.finish()
                    return@launch
                }

                val app = PaymentListenerApplication.instance
                val config = app.database.paymentConfigDao().getConfigSync() ?: com.example.models.PaymentConfig()

                // Fast keyword pre-check to eliminate spam/unrelated SMS
                val isRelevantSender = listOf("bkash", "nagad", "rocket", "upay", "16247", "16167", "16216", "16268", "dbbl")
                    .any { sender.contains(it, ignoreCase = true) }
                val hasPaymentKeywords = listOf("trxid", "txnid", "cash in", "received tk", "tk", "balance")
                    .any { fullBody.contains(it, ignoreCase = true) }

                if (!isRelevantSender && !hasPaymentKeywords) {
                    pendingResult.finish()
                    return@launch
                }

                val parseResult = app.parserEngine.parse(
                    text = fullBody,
                    packageName = null,
                    sender = sender,
                    source = "sms",
                    config = config
                )

                if (parseResult != null) {
                    Log.i(TAG, "Parsed payment from SMS: ${parseResult.method} ৳${parseResult.amount} TrxID: ${parseResult.transactionId}")
                    app.repository.processIncomingPayment(parseResult)
                }
            } catch (e: Exception) {
                // Never crash the receiver
                Log.e(TAG, "Error in PaymentSmsReceiver: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
