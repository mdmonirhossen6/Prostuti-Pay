package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SetupWizardScreen
import com.example.ui.screens.TestParserScreen
import com.example.ui.screens.TransactionDetailDialog
import com.example.ui.screens.TransactionLogScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainScreen(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
  val context = LocalContext.current
  val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
  val selectedEvent by viewModel.selectedEvent.collectAsStateWithLifecycle()

  // Request SMS permissions on initial launch
  val smsPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { /* Results processed silently; UI adapts */ }

  LaunchedEffect(Unit) {
    val permissionsToRequest = mutableListOf<String>()
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
      permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
    }
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
      permissionsToRequest.add(Manifest.permission.READ_SMS)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
    if (permissionsToRequest.isNotEmpty()) {
      smsPermissionLauncher.launch(permissionsToRequest.toTypedArray())
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    bottomBar = {
      NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { viewModel.selectTab(0) },
          icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
          label = { Text("Dashboard") },
          modifier = Modifier.testTag("nav_item_dashboard")
        )
        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { viewModel.selectTab(1) },
          icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transactions") },
          label = { Text("Transactions") },
          modifier = Modifier.testTag("nav_item_transactions")
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { viewModel.selectTab(2) },
          icon = { Icon(Icons.Default.Science, contentDescription = "Test Mode") },
          label = { Text("Test Mode") },
          modifier = Modifier.testTag("nav_item_test_mode")
        )
        NavigationBarItem(
          selected = selectedTab == 3,
          onClick = { viewModel.selectTab(3) },
          icon = { Icon(Icons.Default.Settings, contentDescription = "Config") },
          label = { Text("Config") },
          modifier = Modifier.testTag("nav_item_settings")
        )
        NavigationBarItem(
          selected = selectedTab == 4,
          onClick = { viewModel.selectTab(4) },
          icon = { Icon(Icons.Default.HelpOutline, contentDescription = "Setup & Help") },
          label = { Text("Setup & Help") },
          modifier = Modifier.testTag("nav_item_setup_wizard")
        )
      }
    }
  ) { innerPadding ->
    when (selectedTab) {
      0 -> DashboardScreen(
        viewModel = viewModel,
        modifier = Modifier.padding(innerPadding),
        onNavigateToTransactions = { viewModel.selectTab(1) },
        onNavigateToTest = { viewModel.selectTab(2) },
        onNavigateToWizard = { viewModel.selectTab(4) }
      )
      1 -> TransactionLogScreen(
        viewModel = viewModel,
        modifier = Modifier.padding(innerPadding)
      )
      2 -> TestParserScreen(
        viewModel = viewModel,
        modifier = Modifier.padding(innerPadding)
      )
      3 -> SettingsScreen(
        viewModel = viewModel,
        modifier = Modifier.padding(innerPadding)
      )
      4 -> SetupWizardScreen(
        viewModel = viewModel,
        modifier = Modifier.padding(innerPadding),
        onNavigateToConfig = { viewModel.selectTab(3) },
        onNavigateToSandbox = { viewModel.selectTab(2) }
      )
    }

    // Modal dialog when a transaction is selected
    if (selectedEvent != null) {
      TransactionDetailDialog(
        event = selectedEvent!!,
        viewModel = viewModel,
        onDismiss = { viewModel.selectEvent(null) }
      )
    }
  }
}

