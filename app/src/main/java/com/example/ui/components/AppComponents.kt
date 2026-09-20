package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BkashPink
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.RocketPurple
import com.example.ui.theme.StatusAmbiguous
import com.example.ui.theme.StatusAmbiguousBg
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusApprovedBg
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorBg
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusUnmatched
import com.example.ui.theme.StatusUnmatchedBg
import com.example.ui.theme.UpayBlue
import org.json.JSONArray

/**
 * Clean, status pill badge for transactions and system health states.
 */
@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, dotColor) = when (status.lowercase()) {
        "approved", "pass", "synced" -> Triple(StatusApprovedBg, StatusApproved, StatusApproved)
        "pending", "received", "parsed" -> Triple(StatusPendingBg, StatusPending, StatusPending)
        "ambiguous", "warning" -> Triple(StatusAmbiguousBg, StatusAmbiguous, StatusAmbiguous)
        "unmatched" -> Triple(StatusUnmatchedBg, StatusUnmatched, StatusUnmatched)
        "error", "fail", "rejected" -> Triple(StatusErrorBg, StatusError, StatusError)
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.outline)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = status.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                ),
                color = textColor
            )
        }
    }
}

/**
 * Metric Card for Dashboard statistics.
 */
@Composable
fun MetricCard(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Payment Provider Card for bKash, Nagad, Rocket, and Upay.
 */
@Composable
fun ProviderCard(
    name: String,
    enabled: Boolean,
    packageCount: Int,
    keywordCount: Int,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val brandColor = when (name.lowercase()) {
        "bkash" -> BkashPink
        "nagad" -> NagadOrange
        "rocket" -> RocketPurple
        "upay" -> UpayBlue
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brandColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = brandColor
                        )
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (enabled) StatusApproved else MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                    Text(
                        text = "$packageCount packages • $keywordCount keywords",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = brandColor
                ),
                modifier = Modifier.testTag("toggle_provider_${name.lowercase()}")
            )
        }
    }
}

/**
 * Interactive Tolerance Preview Card demonstrating:
 * Expected ৳, Configured Tolerance ৳, and Accepted Range with a dynamic simulation slider.
 */
@Composable
fun TolerancePreviewCard(
    expectedPrice: Double,
    tolerance: Double,
    maxTolerance: Double,
    toleranceEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val minAccepted = kotlin.math.max(0.0, expectedPrice - if (toleranceEnabled) tolerance else 0.0)
    val maxAccepted = expectedPrice + if (toleranceEnabled) tolerance else 0.0

    var testReceivedAmount by remember(expectedPrice) { mutableDoubleStateOf(expectedPrice - 5.0) }
    val isWithinTolerance = testReceivedAmount in minAccepted..maxAccepted

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Payment Tolerance Logic",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                StatusBadge(
                    status = if (toleranceEnabled) "±৳${tolerance.toInt()}" else "EXACT"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mathematical Range Box
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Min Accepted", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("৳${minAccepted.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = StatusApproved))
                    }
                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Expected Plan Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("৳${expectedPrice.toInt()}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = PrimaryIndigo))
                    }
                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Max Accepted", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("৳${maxAccepted.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = StatusApproved))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Live Interactive Simulation Sandbox:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customer sends: ৳${testReceivedAmount.toInt()}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (isWithinTolerance) "✓ WITHIN TOLERANCE" else "✗ OUT OF RANGE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isWithinTolerance) StatusApproved else StatusError
                )
            }

            Slider(
                value = testReceivedAmount.toFloat(),
                onValueChange = { testReceivedAmount = it.toDouble() },
                valueRange = (expectedPrice - 25.0).toFloat()..(expectedPrice + 25.0).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("slider_tolerance_test")
            )

            Text(
                text = if (isWithinTolerance) {
                    "System decision: AUTO-APPROVE subscription for student."
                } else {
                    "System decision: HOLD for manual admin review (difference > ৳${tolerance.toInt()})."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isWithinTolerance) StatusApproved else StatusError
            )
        }
    }
}

/**
 * Chronological Audit Trail Timeline viewer.
 */
@Composable
fun AuditTimelineView(
    auditJson: String,
    modifier: Modifier = Modifier
) {
    val items = remember(auditJson) {
        val list = mutableListOf<Triple<Long, String, String>>()
        try {
            val arr = JSONArray(auditJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val ts = obj.optLong("timestamp", 0L)
                val action = obj.optString("action", "EVENT")
                val details = obj.optString("details", obj.optString("message", ""))
                list.add(Triple(ts, action, details))
            }
        } catch (_: Exception) {}
        list.reversed() // Most recent first
    }

    if (items.isEmpty()) {
        Text(
            text = "No audit log entries recorded.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { index, (timestamp, action, details) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo)
                    )
                    if (index < items.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(36.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = action,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryIndigo
                        )
                        if (timestamp > 0) {
                            val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(timestamp))
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (details.isNotBlank()) {
                        Text(
                            text = details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section Header with title and optional action.
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
