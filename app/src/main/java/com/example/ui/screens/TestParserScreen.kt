package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.MainViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusError
import java.util.Locale

@Composable
fun TestParserScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsStateWithLifecycle()
    val testInput by viewModel.testInput.collectAsStateWithLifecycle()
    val parseResult by viewModel.testParseResult.collectAsStateWithLifecycle()
    val matchResult by viewModel.testMatchResult.collectAsStateWithLifecycle()

    val presets = listOf(
        "bKash Standard" to "bkash_valid",
        "Nagad Standard" to "nagad_valid",
        "Tolerance Accept (৳995 for ৳1000)" to "tolerance_accept_under",
        "Tolerance Reject (৳985 for ৳1000)" to "tolerance_reject_under",
        "Rocket Received" to "rocket_valid",
        "Upay Received" to "upay_valid",
        "Ambiguous Match" to "ambiguous_duplicate"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("test_parser_screen")
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title card
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate100),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Science,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Payment Parser & Tolerance Sandbox",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Test extraction, Bengali numeral conversion, and server tolerance decisions safely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }

        // Active Tolerance configuration status
        Surface(
            color = PrimaryIndigo.copy(alpha = 0.08f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Current Tolerance Rule:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = if (config.toleranceEnabled) "±৳${config.globalTolerance.toInt()} (Ceiling ৳${config.maxTolerance.toInt()})" else "Exact (৳0.00)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                )
            }
        }

        // Preset Chips
        Text(
            text = "Select Simulation Preset:",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { (label, key) ->
                FilterChip(
                    selected = false,
                    onClick = { viewModel.loadPresetScenario(key) },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }

        // Input Text Area
        OutlinedTextField(
            value = testInput,
            onValueChange = { viewModel.setTestInput(it) },
            label = { Text("Payment Message Raw Text / SMS / Notification") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("test_message_input"),
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(12.dp)
        )

        // Test Parse Action Button
        Button(
            onClick = { viewModel.runTestParse() },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("test_parse_button"),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("TEST PARSE & EXTRACT", fontWeight = FontWeight.Bold)
        }

        // Parsed Result Card
        if (parseResult != null) {
            ElevatedCard(
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
                            MethodLogoBadge(method = parseResult!!.method)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Parsed Result (${parseResult!!.method})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = StatusApproved,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Amount", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = "৳${String.format(Locale.US, "%.2f", parseResult!!.amount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Transaction ID", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = parseResult!!.transactionId,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PrimaryIndigo
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Sender", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = parseResult!!.sender.ifBlank { "Not specified" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Receiver / Ref", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = listOfNotNull(parseResult!!.receiver, parseResult!!.reference).joinToString(" / ").ifBlank { "N/A" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Backend Match Test Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.runTestBackendMatch() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_backend_match_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                        ) {
                            Text("SIMULATE BACKEND MATCH", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.saveTestEventToQueue()
                                Toast.makeText(context, "Added to live queue", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ingest to Queue", fontSize = 11.sp)
                        }
                    }
                }
            }
        } else if (testInput.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error",
                        tint = StatusError,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Unrecognized Payment Format",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = "No valid TrxID or payable amount found in this text. Unknown messages will be ignored or flagged for manual review.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB91C1C)
                        )
                    }
                }
            }
        }

        // Backend Match Simulation Result
        if (matchResult != null) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Backend Match & Tolerance Decision",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        StatusBadge(status = matchResult!!.status)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = matchResult!!.message ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // Tolerance Breakdown Box if available
                    if (matchResult!!.expectedAmount != null || matchResult!!.tolerance != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = PrimaryIndigo.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Tolerance Breakdown (Audited)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                                )
                                Text(
                                    text = "Expected: ৳${matchResult!!.expectedAmount?.toInt()} | Received: ৳${parseResult?.amount?.toInt()}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    text = "Allowed Range: ৳${matchResult!!.minimumAcceptedAmount?.toInt()} – ৳${matchResult!!.maximumAcceptedAmount?.toInt()} (±৳${matchResult!!.tolerance?.toInt()})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (matchResult!!.amountDifference != null) {
                                    Text(
                                        text = "Variance: ৳${String.format(Locale.US, "%+.2f", matchResult!!.amountDifference)}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (matchResult!!.success) StatusApproved else StatusError
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate100)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Candidate Pending Requests Found: ${matchResult!!.candidateCount}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (matchResult!!.candidateIds.isNotEmpty()) {
                                Text(
                                    text = "Candidate IDs: ${matchResult!!.candidateIds.joinToString(", ")}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Slate700
                                )
                            }
                            if (matchResult!!.matchedRequestId != null) {
                                Text(
                                    text = "Automated Approval: SUCCESSFUL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusApproved
                                )
                                Text(
                                    text = "Triggered: Prostuti Premium Plan Activation",
                                    fontSize = 11.sp,
                                    color = Slate800
                                )
                            } else if (matchResult!!.status == "unmatched") {
                                Text(
                                    text = "Action: Stored in payment_listener_events with status='unmatched'. Ready for delayed matching once student submits payment form.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF0284C7)
                                )
                            } else if (matchResult!!.status == "ambiguous") {
                                Text(
                                    text = "Action: Stored with status='ambiguous'. Flagged for admin manual review because multiple pending requests share this TrxID or variance exceeded limit.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF7C3AED)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
