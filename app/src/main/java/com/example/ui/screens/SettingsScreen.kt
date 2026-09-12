package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.BkashPink
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.RocketPurple
import com.example.ui.theme.Slate100
import com.example.ui.theme.StatusError
import com.example.ui.theme.UpayBlue

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentConfig by viewModel.config.collectAsStateWithLifecycle()

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Listener & Source Configuration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // System Permissions Shortcuts
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Android System Access",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Grant permissions to allow automatic payment detection on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Notification Listener Settings")
                }
            }
        }

        // Method ON/OFF Toggles
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Payment Method Listeners",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
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

        // Backend Endpoint Settings
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Firebase Backend Integration",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "HTTPS endpoint of the processPaymentListenerEvent Cloud Function.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = backendUrl,
                    onValueChange = { backendUrl = it },
                    label = { Text("Cloud Function Endpoint URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Internal Service Secret / API Key") },
                    modifier = Modifier.fillMaxWidth(),
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
                        Text(text = "Immediately submit to backend on detection", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(
                        checked = autoSync,
                        onCheckedChange = { autoSync = it }
                    )
                }
            }
        }

        // Keywords and Packages Configuration
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sender Keywords & App Packages",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Comma-separated keywords used to match SMS sender and app notifications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = bkashKeywords,
                    onValueChange = { bkashKeywords = it },
                    label = { Text("bKash Sender Keywords") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nagadKeywords,
                    onValueChange = { nagadKeywords = it },
                    label = { Text("Nagad Sender Keywords") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = rocketKeywords,
                    onValueChange = { rocketKeywords = it },
                    label = { Text("Rocket Sender Keywords") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = upayKeywords,
                    onValueChange = { upayKeywords = it },
                    label = { Text("Upay Sender Keywords") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Save Configuration Button
        Button(
            onClick = {
                val updated = currentConfig.copy(
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
                Toast.makeText(context, "Configuration saved", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("save_config_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save Configuration")
        }

        // Maintenance / Clear Cache
        OutlinedButton(
            onClick = {
                viewModel.clearAllEvents()
                Toast.makeText(context, "Local event history cleared", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
        ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Clear Local Event Database")
        }
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
