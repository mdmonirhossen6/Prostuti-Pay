package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.TolerancePreviewCard
import com.example.ui.theme.BkashPink
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.RocketPurple
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusError
import com.example.ui.theme.UpayBlue

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentConfig by viewModel.config.collectAsStateWithLifecycle()
    val isCheckingHealth by viewModel.isCheckingHealth.collectAsStateWithLifecycle()
    val healthResult by viewModel.healthCheckResult.collectAsStateWithLifecycle()

    // Form states
    var deviceName by remember(currentConfig) { mutableStateOf(currentConfig.deviceName) }
    var deviceId by remember(currentConfig) { mutableStateOf(currentConfig.deviceId) }
    var environment by remember(currentConfig) { mutableStateOf(currentConfig.environment) }

    var toleranceEnabled by remember(currentConfig) { mutableStateOf(currentConfig.toleranceEnabled) }
    var globalTolerance by remember(currentConfig) { mutableDoubleStateOf(currentConfig.globalTolerance) }
    var maxTolerance by remember(currentConfig) { mutableDoubleStateOf(currentConfig.maxTolerance) }

    var bkashKeywords by remember(currentConfig) { mutableStateOf(currentConfig.bkashKeywords) }
    var nagadKeywords by remember(currentConfig) { mutableStateOf(currentConfig.nagadKeywords) }
    var rocketKeywords by remember(currentConfig) { mutableStateOf(currentConfig.rocketKeywords) }
    var upayKeywords by remember(currentConfig) { mutableStateOf(currentConfig.upayKeywords) }

    var bkashPackages by remember(currentConfig) { mutableStateOf(currentConfig.bkashPackages) }
    var nagadPackages by remember(currentConfig) { mutableStateOf(currentConfig.nagadPackages) }
    var rocketPackages by remember(currentConfig) { mutableStateOf(currentConfig.rocketPackages) }
    var upayPackages by remember(currentConfig) { mutableStateOf(currentConfig.upayPackages) }

    var backendUrl by remember(currentConfig) { mutableStateOf(currentConfig.backendUrl) }
    var apiKey by remember(currentConfig) { mutableStateOf(currentConfig.apiKey) }
    var autoSync by remember(currentConfig) { mutableStateOf(currentConfig.autoSyncEnabled) }

    // Dialog states
    var showResetDialog by remember { mutableStateOf(false) }
    var showClearEventsDialog by remember { mutableStateOf(false) }
    var showAddPlanDialog by remember { mutableStateOf(false) }
    var newPlanName by remember { mutableStateOf("") }
    var newPlanTolerance by remember { mutableStateOf("15") }

    val perPlanMap = remember(currentConfig.perPlanToleranceJson) {
        currentConfig.getPerPlanToleranceMap()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(
            title = "Configuration Center",
            subtitle = "Manage payment price tolerance, listening rules, device identity, and Firebase endpoints"
        )

        // 1. Payment Price Tolerance Configuration
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        Icon(Icons.Default.Tune, contentDescription = null, tint = PrimaryIndigo)
                        Column {
                            Text(
                                text = "Payment Price Tolerance",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Allowable variance in currency units (৳)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = toleranceEnabled,
                        onCheckedChange = { toleranceEnabled = it },
                        modifier = Modifier.testTag("toggle_tolerance_enabled")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (toleranceEnabled) {
                    // Global tolerance slider & label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Global Tolerance: ±৳${globalTolerance.toInt()}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Safe default: ৳10",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Slider(
                        value = globalTolerance.toFloat(),
                        onValueChange = { globalTolerance = it.toDouble() },
                        valueRange = 0f..maxTolerance.toFloat(),
                        steps = 19,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slider_global_tolerance")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Maximum Tolerance Ceiling Setting
                    OutlinedTextField(
                        value = maxTolerance.toInt().toString(),
                        onValueChange = { str ->
                            str.toDoubleOrNull()?.let {
                                maxTolerance = it.coerceIn(10.0, 200.0)
                            }
                        },
                        label = { Text("Server Safety Ceiling Limit (৳)") },
                        supportingText = { Text("Maximum tolerance allowed server-side to prevent underpayment exploitation") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Embedded interactive preview
                    TolerancePreviewCard(
                        expectedPrice = 1000.0,
                        tolerance = globalTolerance,
                        maxTolerance = maxTolerance,
                        toleranceEnabled = toleranceEnabled
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Per-Plan Overrides
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Plan-Specific Tolerance Overrides",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { showAddPlanDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Plan Override", tint = PrimaryIndigo)
                        }
                    }

                    if (perPlanMap.isEmpty()) {
                        Text(
                            text = "No plan overrides. All packages use global tolerance (±৳${globalTolerance.toInt()}).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        perPlanMap.forEach { (plan, tol) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$plan: ±৳${tol.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                IconButton(onClick = { viewModel.removePerPlanTolerance(plan) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = StatusError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Tolerance disabled: Payments must match exact subscription price to ৳0.00 precision.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Payment Method Listeners
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Payment Method Channels",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingSwitchRow("bKash Listener", BkashPink, currentConfig.bkashEnabled) {
                    viewModel.toggleMethod("bkash", it)
                }
                SettingSwitchRow("Nagad Listener", NagadOrange, currentConfig.nagadEnabled) {
                    viewModel.toggleMethod("nagad", it)
                }
                SettingSwitchRow("Rocket Listener", RocketPurple, currentConfig.rocketEnabled) {
                    viewModel.toggleMethod("rocket", it)
                }
                SettingSwitchRow("Upay Listener", UpayBlue, currentConfig.upayEnabled) {
                    viewModel.toggleMethod("upay", it)
                }
            }
        }

        // 3. Backend Integration & Health Diagnostics
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Firebase Backend Integration",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    OutlinedButton(
                        onClick = { viewModel.runBackendHealthCheck() },
                        enabled = !isCheckingHealth,
                        modifier = Modifier.testTag("test_backend_health_button")
                    ) {
                        if (isCheckingHealth) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Icon(Icons.Default.HealthAndSafety, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Ping", fontSize = 12.sp)
                    }
                }

                if (healthResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = if (healthResult!!.status == "PASS") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusBadge(status = healthResult!!.status)
                            Text(
                                text = healthResult!!.message,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = backendUrl,
                    onValueChange = { backendUrl = it },
                    label = { Text("Cloud Function Endpoint URL") },
                    supportingText = { Text("HTTPS URL for processPaymentListenerEvent") },
                    modifier = Modifier.fillMaxWidth().testTag("input_backend_url"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Internal Service Secret / API Key") },
                    supportingText = { Text("Pre-shared key validated by the Cloud Function") },
                    modifier = Modifier.fillMaxWidth().testTag("input_api_key"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Auto-sync incoming events", fontWeight = FontWeight.Medium)
                        Text(text = "Instantly forward parsed payments to backend", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(
                        checked = autoSync,
                        onCheckedChange = { autoSync = it }
                    )
                }
            }
        }

        // 4. Device Identity & Environment
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Device Identity & Environment",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = deviceId,
                    onValueChange = { deviceId = it },
                    label = { Text("Device ID / Audit Identifier") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = environment,
                    onValueChange = { environment = it },
                    label = { Text("Environment (production / sandbox)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // 5. Package & Keyword Customization
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Listening Filters & Package Names",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Fine-tune application package names and sender shortcodes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bkashPackages,
                    onValueChange = { bkashPackages = it },
                    label = { Text("bKash App Packages") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = bkashKeywords,
                    onValueChange = { bkashKeywords = it },
                    label = { Text("bKash Sender Keywords") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nagadPackages,
                    onValueChange = { nagadPackages = it },
                    label = { Text("Nagad App Packages") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = nagadKeywords,
                    onValueChange = { nagadKeywords = it },
                    label = { Text("Nagad Sender Keywords") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Save All Changes Button
        Button(
            onClick = {
                val updated = currentConfig.copy(
                    deviceName = deviceName,
                    deviceId = deviceId,
                    environment = environment,
                    toleranceEnabled = toleranceEnabled,
                    globalTolerance = globalTolerance,
                    maxTolerance = maxTolerance,
                    bkashKeywords = bkashKeywords,
                    nagadKeywords = nagadKeywords,
                    rocketKeywords = rocketKeywords,
                    upayKeywords = upayKeywords,
                    bkashPackages = bkashPackages,
                    nagadPackages = nagadPackages,
                    rocketPackages = rocketPackages,
                    upayPackages = upayPackages,
                    backendUrl = backendUrl,
                    apiKey = apiKey,
                    autoSyncEnabled = autoSync
                )
                viewModel.saveConfig(updated)
                Toast.makeText(context, "All configuration settings saved", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("save_config_button"),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save All Settings", fontWeight = FontWeight.Bold)
        }

        // Reset to Factory Defaults
        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset to Factory Defaults")
        }

        // Maintenance / Clear Cache
        OutlinedButton(
            onClick = { showClearEventsDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
        ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Clear Local Event Database")
        }
    }

    // Add Plan Override Dialog
    if (showAddPlanDialog) {
        AlertDialog(
            onDismissRequest = { showAddPlanDialog = false },
            title = { Text("Add Plan Tolerance Override") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPlanName,
                        onValueChange = { newPlanName = it },
                        label = { Text("Plan Name (e.g. 3 Months)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPlanTolerance,
                        onValueChange = { newPlanTolerance = it },
                        label = { Text("Tolerance ৳ (e.g. 15)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tol = newPlanTolerance.toDoubleOrNull() ?: 10.0
                        if (newPlanName.isNotBlank()) {
                            viewModel.setPerPlanTolerance(newPlanName.trim(), tol)
                        }
                        showAddPlanDialog = false
                    }
                ) {
                    Text("Add Override")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlanDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Reset Defaults Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Configuration?") },
            text = { Text("This will restore default tolerance (৳10), packages, keywords, and endpoints.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetConfigToDefaults()
                        showResetDialog = false
                        Toast.makeText(context, "Reset to default configuration", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Clear Events Confirmation Dialog
    if (showClearEventsDialog) {
        AlertDialog(
            onDismissRequest = { showClearEventsDialog = false },
            title = { Text("Clear Local Transaction History?") },
            text = { Text("This will delete all local payment events recorded on this phone. Backend Firestore records are NOT affected.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllEvents()
                        showClearEventsDialog = false
                        Toast.makeText(context, "Local event history cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearEventsDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    color: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontWeight = FontWeight.SemiBold, color = color)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = color)
        )
    }
}
