package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.models.PaymentEvent
import com.example.ui.MainViewModel
import com.example.ui.theme.BkashPink
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.RocketPurple
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusAmbiguous
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusApprovedBg
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusUnmatched
import com.example.ui.theme.UpayBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Fallback color constant for styling
val TeastAccentColor = Color(0xFF0D9488)

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToTest: () -> Unit = {}
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsStateWithLifecycle()
    val events by viewModel.filteredEvents.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val approvedCount by viewModel.approvedCount.collectAsStateWithLifecycle()
    val unmatchedCount by viewModel.unmatchedCount.collectAsStateWithLifecycle()
    val ambiguousCount by viewModel.ambiguousCount.collectAsStateWithLifecycle()
    val errorCount by viewModel.errorCount.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSync by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()

    var isNotifAccessGranted by remember { mutableStateOf(false) }

    // Periodically re-check Notification access
    LaunchedEffect(Unit) {
        isNotifAccessGranted = viewModel.checkNotificationAccess(context)
    }

    val recentEvent = events.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. App Title & System Status Banner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Slate900
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Prostuti Payment Listener",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automated Transaction Verification Service",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        // Connected indicator pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF065F46))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StatusApproved)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LISTENER ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status rows: Notification Access, SMS, Firebase, Last Sync
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusMiniBadge(
                            label = "Notification Access",
                            isActive = isNotifAccessGranted,
                            icon = Icons.Default.Notifications,
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }
                        )
                        StatusMiniBadge(
                            label = "SMS Listener",
                            isActive = true,
                            icon = Icons.Default.Sms,
                            onClick = {}
                        )
                        StatusMiniBadge(
                            label = "Firebase Backend",
                            isActive = true,
                            icon = Icons.Default.CloudDone,
                            onClick = {}
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lastSync != null) {
                                "Last Sync: " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastSync!!))
                            } else {
                                "Sync Mode: Auto-sync on event"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )

                        FilledTonalButton(
                            onClick = { viewModel.syncQueue() },
                            enabled = !isSyncing,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("sync_queue_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Syncing...")
                            } else {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Sync",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Queue")
                            }
                        }
                    }
                }
            }
        }

        // 2. Notification Permission Alert if not granted
        if (!isNotifAccessGranted) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFEF3C7)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Listener Disabled",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF78350F)
                            )
                            Text(
                                text = "Allow listener access in Android Settings to intercept bKash/Nagad app notifications.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E)
                            )
                        }
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB45309)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Enable", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3. Payment Method Toggles
        item {
            Column {
                Text(
                    text = "Supported Payment Methods",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MethodToggleCard(
                        name = "bKash",
                        color = BkashPink,
                        enabled = config.bkashEnabled,
                        onToggle = { viewModel.toggleMethod("bkash", it) },
                        modifier = Modifier.weight(1f)
                    )
                    MethodToggleCard(
                        name = "Nagad",
                        color = NagadOrange,
                        enabled = config.nagadEnabled,
                        onToggle = { viewModel.toggleMethod("nagad", it) },
                        modifier = Modifier.weight(1f)
                    )
                    MethodToggleCard(
                        name = "Rocket",
                        color = RocketPurple,
                        enabled = config.rocketEnabled,
                        onToggle = { viewModel.toggleMethod("rocket", it) },
                        modifier = Modifier.weight(1f)
                    )
                    MethodToggleCard(
                        name = "Upay",
                        color = UpayBlue,
                        enabled = config.upayEnabled,
                        onToggle = { viewModel.toggleMethod("upay", it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Metrics Summary Grid
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction Verification Metrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total: $totalCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Auto Approved",
                        count = approvedCount,
                        color = StatusApproved,
                        bgColor = StatusApprovedBg,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Unmatched",
                        count = unmatchedCount,
                        color = StatusUnmatched,
                        bgColor = Color(0xFFE0F2FE),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Ambiguous",
                        count = ambiguousCount,
                        color = StatusAmbiguous,
                        bgColor = Color(0xFFEDE9FE),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Errors / Retries",
                        count = errorCount,
                        color = StatusError,
                        bgColor = Color(0xFFFEE2E2),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Recent Transaction Hero Card
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Most Recent Transaction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (events.isNotEmpty()) {
                        Text(
                            text = "View All (${events.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryIndigo,
                            modifier = Modifier
                                .clickable { onNavigateToTransactions() }
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (recentEvent != null) {
                    ElevatedCard(
                        onClick = { viewModel.selectEvent(recentEvent) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    MethodLogoBadge(method = recentEvent.method)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = recentEvent.method,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Via ${recentEvent.source.uppercase()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                StatusChip(status = recentEvent.status)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "৳${String.format(Locale.US, "%.2f", recentEvent.amount)}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Sender: ${recentEvent.sender.ifBlank { "N/A" }}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "TrxID: ${recentEvent.transactionId}",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(recentEvent.receivedAt)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate100)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Science,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No transactions detected yet",
                                fontWeight = FontWeight.Medium,
                                color = Slate700
                            )
                            Text(
                                text = "Incoming SMS and notifications from bKash, Nagad, Rocket, and Upay will appear here automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { onNavigateToTest() },
                                modifier = Modifier.testTag("try_test_mode_button")
                            ) {
                                Text("Try Test Mode Sandbox")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusMiniBadge(
    label: String,
    isActive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isActive) Color(0xFF0F766E) else Color(0xFF475569)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isActive) Color(0xFF5EEAD4) else Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFE2E8F0),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (isActive) "Enabled" else "Tap to enable",
            fontSize = 9.sp,
            color = if (isActive) Color(0xFF34D399) else Color(0xFFFCA5A5)
        )
    }
}

@Composable
fun MethodToggleCard(
    name: String,
    color: Color,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) color.copy(alpha = 0.12f) else Slate100
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = if (enabled) color else Color.Gray
            )
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = color,
                    checkedTrackColor = color.copy(alpha = 0.5f)
                ),
                modifier = Modifier.size(width = 36.dp, height = 24.dp)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    count: Int,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = color.copy(alpha = 0.9f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MethodLogoBadge(method: String) {
    val (bgColor, letter) = when (method.lowercase()) {
        "bkash" -> BkashPink to "bK"
        "nagad" -> NagadOrange to "N"
        "rocket" -> RocketPurple to "R"
        "upay" -> UpayBlue to "U"
        else -> Slate800 to "P"
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp
        )
    }
}

@Composable
fun StatusChip(status: String) {
    val (bgColor, textColor, label) = when (status.lowercase()) {
        "approved", "matched" -> Triple(StatusApprovedBg, StatusApproved, "APPROVED")
        "pending", "parsed", "received" -> Triple(Color(0xFFFEF3C7), StatusPending, "PENDING")
        "unmatched" -> Triple(Color(0xFFE0F2FE), StatusUnmatched, "UNMATCHED")
        "ambiguous" -> Triple(Color(0xFFEDE9FE), StatusAmbiguous, "AMBIGUOUS")
        "rejected" -> Triple(Color(0xFFFEE2E2), StatusError, "REJECTED")
        else -> Triple(Color(0xFFFEE2E2), StatusError, status.uppercase())
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}
