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
    val testInput by viewModel.testInput.collectAsStateWithLifecycle()
    val parseResult by viewModel.testParseResult.collectAsStateWithLifecycle()
    val matchResult by viewModel.testMatchResult.collectAsStateWithLifecycle()

    val presets = listOf(
        "bKash Receive" to "You have received Tk 500.00 from 01712345678. Fee Tk 0.00. Balance Tk 1,500.00. TrxID BKA8921XYZ at 12/09/2026 14:30",
        "Nagad Cash In" to "Cash In received. Amount: Tk 650.00, Sender: 01812345678, TxnID: 72N0ABCD, Balance: Tk 2,150.00, Time: 12/09/2026 14:32",
        "Rocket Received" to "Tk500.00 received from 01712345678-9 to A/C 01987654321-0. Fee Tk 0.00, Balance Tk 2,500.00, TxnId: 198273645 on 12-Sep-2026",
        "Upay Received" to "Received Tk 500.00 from 01712345678. TxnID: UP789123. Balance Tk 1,200.00. Fee Tk 0.00",
        "Ambiguous Match" to "Received Tk 300.00 from 01912345678. TxnId: DUPLICATE_TRX_999 to A/C 01899999999-1",
        "Unmatched Trx" to "You have received Tk 1,200.00 from 01611223344. TrxID UNKNOWN_TRX_888.",
        "Spam / Non-Payment" to "Your weekly balance bonus of 50 points is ready to claim! Dial *123# now."
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
                        text = "Payment Parser & Matching Sandbox",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Test extraction and verify backend matching logic safely without sending real money.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }

        // Preset Chips
        Text(
            text = "Select Sample Payment SMS:",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { (label, message) ->
                FilterChip(
                    selected = testInput == message,
                    onClick = {
                        viewModel.setTestInput(message)
                        viewModel.runTestParse()
                    },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }

        // Input Text Area
        OutlinedTextField(
            value = testInput,
            onValueChange = { viewModel.setTestInput(it) },
            label = { Text("Payment Message Raw Text") },
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
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("TEST PARSE", fontWeight = FontWeight.Bold)
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
                            Text("TEST BACKEND MATCH", fontSize = 11.sp)
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
                            text = "Backend Simulation Decision",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        StatusChip(status = matchResult!!.status)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = matchResult!!.message ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

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
                                    text = "Action: Stored in payment_listener_events with status='unmatched'. Ready for delayed matching once user submits payment form.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF0284C7)
                                )
                            } else if (matchResult!!.status == "ambiguous") {
                                Text(
                                    text = "Action: Stored with status='ambiguous'. Flagged for admin manual review because multiple pending requests share this TrxID.",
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
