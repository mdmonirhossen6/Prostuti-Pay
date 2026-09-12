package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.PaymentEvent
import com.example.ui.MainViewModel
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailDialog(
    event: PaymentEvent,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MethodLogoBadge(method = event.method)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = event.method,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "TrxID: ${event.transactionId}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = PrimaryIndigo
                        )
                    }
                }
                StatusChip(status = event.status)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Amount hero section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate100)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "৳${String.format(Locale.US, "%.2f", event.amount)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Received at: " + SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(event.receivedAt)),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Breakdown fields
                DetailRow(label = "Sender Number", value = event.sender.ifBlank { "Not provided" })
                if (!event.receiver.isNullOrBlank()) {
                    DetailRow(label = "Receiver A/C", value = event.receiver)
                }
                if (!event.reference.isNullOrBlank()) {
                    DetailRow(label = "Reference", value = event.reference)
                }
                DetailRow(label = "Input Source", value = event.source.uppercase())
                DetailRow(label = "Deduplication Fingerprint", value = event.fingerprint.take(16) + "...", isMonospace = true)

                Spacer(modifier = Modifier.height(10.dp))

                // Verification & Audit Trail Section
                Text(
                    text = "Verification & Audit Trail",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Slate800
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        AuditRow(label = "Verification Source", value = "auto_sms_listener")
                        AuditRow(
                            label = "Matched Request ID",
                            value = event.matchedPaymentRequestId ?: "None (Awaiting matching request)"
                        )
                        AuditRow(
                            label = "Status Decision",
                            value = event.status.uppercase()
                        )
                        if (!event.verificationDetails.isNullOrBlank()) {
                            AuditRow(label = "Backend Notes", value = event.verificationDetails)
                        }
                        if (!event.errorMessage.isNullOrBlank()) {
                            AuditRow(label = "Error Message", value = event.errorMessage)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Raw Message Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Raw Evidence Text",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Slate800
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Payment Raw Text", event.rawText))
                            Toast.makeText(context, "Raw text copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(10.dp)
                ) {
                    Text(
                        text = event.rawText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8),
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Manual Actions
                Text(
                    text = "Manual Operations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Slate800
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            viewModel.manuallyMarkStatus(event.id, "approved", "Manual admin override approval")
                            Toast.makeText(context, "Marked as Approved", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFD1FAE5))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = StatusApproved, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve", color = StatusApproved, fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            viewModel.deleteEvent(event.id)
                            Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFEE2E2))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = StatusError, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String, isMonospace: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}

@Composable
private fun AuditRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Black)
    }
}
