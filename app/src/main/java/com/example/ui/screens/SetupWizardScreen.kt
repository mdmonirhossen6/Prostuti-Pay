package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.MainViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending

data class WizardStepData(
    val stepNumber: Int,
    val title: String,
    val category: String,
    val description: String,
    val technicalNote: String,
    val actionLabel: String? = null,
    val actionType: String? = null // "NOTIFICATIONS", "BATTERY", "DIAGNOSTIC", "CONFIG", "SANDBOX"
)

data class FaqItem(
    val question: String,
    val solution: String,
    val category: String
)

@Composable
fun SetupWizardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToConfig: () -> Unit = {},
    onNavigateToSandbox: () -> Unit = {}
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsStateWithLifecycle()
    val isCheckingHealth by viewModel.isCheckingHealth.collectAsStateWithLifecycle()
    val healthResult by viewModel.healthCheckResult.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: 12-Step Setup Wizard, 1: Troubleshooting FAQ, 2: Firebase Connection Guide

    val steps = listOf(
        WizardStepData(
            stepNumber = 1,
            title = "Prepare Dedicated Payment Device",
            category = "Hardware",
            description = "Use a dedicated Android phone permanently connected to WiFi/charger with official SIM cards (bKash, Nagad, Rocket, Upay).",
            technicalNote = "Do NOT use personal phones or phones with aggressive background power savers."
        ),
        WizardStepData(
            stepNumber = 2,
            title = "Grant Notification Listener Access",
            category = "Permissions",
            description = "Allow Prostuti Payment Listener to read incoming push notifications from payment apps.",
            technicalNote = "Uses Android NotificationListenerService to intercept bKash and Nagad payment banners.",
            actionLabel = "Open Notification Settings",
            actionType = "NOTIFICATIONS"
        ),
        WizardStepData(
            stepNumber = 3,
            title = "Grant SMS Permissions",
            category = "Permissions",
            description = "Allow RECEIVE_SMS and READ_SMS to parse incoming telecom transaction confirmation messages.",
            technicalNote = "PaymentSmsReceiver ingests 16247, 16167, 16216, 16268 SMS in real-time."
        ),
        WizardStepData(
            stepNumber = 4,
            title = "Disable Battery Optimization",
            category = "System Stability",
            description = "Disable Doze mode and manufacturer background killing so listeners never sleep.",
            technicalNote = "Exclude from battery restrictions in Android Settings -> Battery -> Unrestricted.",
            actionLabel = "Open Battery Settings",
            actionType = "BATTERY"
        ),
        WizardStepData(
            stepNumber = 5,
            title = "Configure Payment Methods",
            category = "Configuration",
            description = "Enable target payment providers (bKash, Nagad, Rocket, Upay) and set payment tolerance.",
            technicalNote = "Default tolerance is ৳10 (accepts ±৳10 around expected subscription price).",
            actionLabel = "Open Configuration Center",
            actionType = "CONFIG"
        ),
        WizardStepData(
            stepNumber = 6,
            title = "Connect Firebase Backend Endpoint",
            category = "Cloud Integration",
            description = "Enter your deployed Firebase Cloud Function HTTPS URL and pre-shared API Key.",
            technicalNote = "All events are sent to processPaymentListenerEvent for server-authoritative matching.",
            actionLabel = "Test Backend Connectivity",
            actionType = "DIAGNOSTIC"
        ),
        WizardStepData(
            stepNumber = 7,
            title = "Configure Firestore Collections",
            category = "Database",
            description = "Ensure 'payment_requests', 'payment_listener_events', and 'payment_config' exist in Firestore.",
            technicalNote = "Transactions query pending requests with matching TrxID and phone."
        ),
        WizardStepData(
            stepNumber = 8,
            title = "Deploy Cloud Function",
            category = "Backend Logic",
            description = "Deploy processPaymentListenerEvent using Firebase CLI (firebase deploy --only functions).",
            technicalNote = "The Cloud Function enforces tolerance rules, updates subscription in users/{uid}, and writes audit trails."
        ),
        WizardStepData(
            stepNumber = 9,
            title = "Test Backend Connectivity",
            category = "Verification",
            description = "Run an end-to-end ping to verify network latency, SSL certificate, and API key authentication.",
            technicalNote = "Ensures the dedicated payment phone can reliably reach Firebase.",
            actionLabel = "Run Diagnostic Ping",
            actionType = "DIAGNOSTIC"
        ),
        WizardStepData(
            stepNumber = 10,
            title = "Test Parser Sandbox",
            category = "Testing",
            description = "Paste sample SMS or notification text in Test Mode to test regex and normalizer logic.",
            technicalNote = "Simulates TrxID extraction, amount parsing, and Bengali digit translation.",
            actionLabel = "Open Test Sandbox",
            actionType = "SANDBOX"
        ),
        WizardStepData(
            stepNumber = 11,
            title = "Test Sandbox Transaction",
            category = "Testing",
            description = "Send a test transaction and verify it against mock pending subscription requests.",
            technicalNote = "Validates auto-approval with tolerance matching without activating live payments."
        ),
        WizardStepData(
            stepNumber = 12,
            title = "Enable Production Mode",
            category = "Deployment",
            description = "Switch environment to 'production' and leave the device active on the charging stand.",
            technicalNote = "The listener is now active 24/7. Check Transaction Logs regularly for audit monitoring."
        )
    )

    val faqList = listOf(
        FaqItem(
            category = "Permissions",
            question = "Notification access was revoked after reboot or app update",
            solution = "Some Android versions reset notification permissions on app reinstall. Go to Settings > Apps > Special App Access > Notification Access and ensure 'Prostuti Payment Listener' is enabled. The app also prompts automatically if revoked."
        ),
        FaqItem(
            category = "OEM Killers",
            question = "App stops receiving notifications when screen is off on Xiaomi / Samsung / Vivo",
            solution = "Xiaomi MIUI/HyperOS, Samsung OneUI, and Vivo FunTouchOS aggressively kill background processes. Fix:\n1. Lock the app in the recent apps tray.\n2. Set Battery Optimization to 'No Restrictions' / 'Unrestricted'.\n3. Enable 'Auto-start' in device app management."
        ),
        FaqItem(
            category = "Tolerance",
            question = "Student sent ৳995 for a ৳1000 subscription. Will it approve?",
            solution = "YES! With default tolerance of ৳10, the acceptable range is ৳990 to ৳1010. The backend Cloud Function will auto-approve the subscription and record amountDifference = ৳5 in the audit trail."
        ),
        FaqItem(
            category = "Tolerance",
            question = "Student sent ৳980 for a ৳1000 subscription. What happens?",
            solution = "The payment is rejected from auto-approval because ৳980 is below the ৳990 minimum accepted limit. The transaction will appear with status 'UNMATCHED / AMBIGUOUS' in the Transaction Logs for manual admin review."
        ),
        FaqItem(
            category = "Security",
            question = "Can a rogue student spoof an SMS to get free subscription?",
            solution = "NO. The Android listener is merely an evidence collector. The backend Cloud Function strictly verifies that the transaction ID is unique, checks that a pending payment request exists with that exact TrxID, validates sender and amount against server tolerance, and prevents duplicate reuse."
        ),
        FaqItem(
            category = "Network",
            question = "What if the payment phone loses WiFi or internet connectivity?",
            solution = "The app has an offline queue with Room persistence and 9-step exponential backoff (5s, 15s, 30s, 1m, 2m, 5m, 10m, 15m, 30m). All transactions are stored locally and will automatically flush to Firebase as soon as connectivity resumes."
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = PrimaryIndigo
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("12-Step Setup", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Troubleshooting", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Firebase Guide", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        when (selectedTab) {
            0 -> {
                // Setup Stepper
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = PrimaryIndigo.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Setup Wizard Progress",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (config.setupWizardCompleted) "All 12 steps completed! System operational." else "Current step: ${config.setupCurrentStep} of 12",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadge(
                                    status = if (config.setupWizardCompleted) "COMPLETED" else "STEP ${config.setupCurrentStep}"
                                )
                            }
                        }
                    }

                    // Health check banner if triggered
                    if (healthResult != null) {
                        item {
                            val res = healthResult!!
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (res.reachable && res.status == "PASS") MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StatusBadge(status = res.status)
                                        Text(
                                            text = if (res.reachable) "Backend Reachable (${res.responseTimeMs}ms)" else "Backend Unreachable",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = res.message,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    items(steps) { step ->
                        val isCurrent = step.stepNumber == config.setupCurrentStep
                        val isDone = step.stepNumber < config.setupCurrentStep || config.setupWizardCompleted

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wizard_step_${step.stepNumber}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 2.dp else 0.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isDone) StatusApproved else if (isCurrent) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isDone) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = "${step.stepNumber}",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = step.title,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = step.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    StatusBadge(status = if (isDone) "DONE" else if (isCurrent) "ACTIVE" else "PENDING")
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = step.description,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "💡 ${step.technicalNote}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Action Buttons
                                if (step.actionLabel != null && (isCurrent || isDone)) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                when (step.actionType) {
                                                    "NOTIFICATIONS" -> {
                                                        try {
                                                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                                        } catch (_: Exception) {}
                                                    }
                                                    "BATTERY" -> {
                                                        try {
                                                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                            context.startActivity(intent)
                                                        } catch (_: Exception) {}
                                                    }
                                                    "DIAGNOSTIC" -> viewModel.runBackendHealthCheck()
                                                    "CONFIG" -> onNavigateToConfig()
                                                    "SANDBOX" -> onNavigateToSandbox()
                                                }
                                            },
                                            modifier = Modifier.testTag("step_action_${step.stepNumber}")
                                        ) {
                                            if (step.actionType == "DIAGNOSTIC" && isCheckingHealth) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(step.actionLabel, fontSize = 12.sp)
                                        }

                                        if (isCurrent && step.stepNumber < 12) {
                                            Button(
                                                onClick = { viewModel.setWizardStep(step.stepNumber + 1) },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                                            ) {
                                                Text("Next Step", fontSize = 12.sp)
                                            }
                                        } else if (isCurrent && step.stepNumber == 12) {
                                            Button(
                                                onClick = { viewModel.completeWizard() },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusApproved)
                                            ) {
                                                Text("Finish Setup", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.resetWizard() }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restart Setup Wizard")
                            }
                        }
                    }
                }
            }

            1 -> {
                // Troubleshooting FAQ
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "Troubleshooting & Knowledge Base",
                            subtitle = "Common operational scenarios and solutions for the dedicated payment phone"
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    items(faqList) { faq ->
                        var expanded by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = faq.category.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                                        )
                                        Text(
                                            text = faq.question,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                    Icon(
                                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                AnimatedVisibility(visible = expanded) {
                                    Column {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider()
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = faq.solution,
                                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Firebase Connection Guide
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "Firebase Cloud Function Architecture",
                            subtitle = "How Prostuti Payment Listener securely interfaces with Firebase"
                        )
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Security Contract",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "1. The Android app NEVER activates subscriptions directly.\n" +
                                            "2. The Android APK holds NO service account keys.\n" +
                                            "3. The Cloud Function processPaymentListenerEvent is authoritative.\n" +
                                            "4. Tolerance is validated on the backend inside a Firestore transaction.\n" +
                                            "5. Unmatched or ambiguous events are stored safely for manual review.",
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp)
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Cloud Function Deployment Command",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "firebase deploy --only functions:processPaymentListenerEvent",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryIndigo
                                        ),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Firestore Collections Structure",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "• payment_requests: Pending orders submitted from Prostuti web/app with orderId, userId, amount, method, status='pending'.\n\n" +
                                            "• payment_listener_events: Historical audit log of every detected transaction, payload, tolerance evaluation, and match result.\n\n" +
                                            "• users/{uid}: Updated with subscription: { active: true, planId: ... } upon successful auto-approval.",
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
