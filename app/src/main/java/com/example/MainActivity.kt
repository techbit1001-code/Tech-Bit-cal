package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calculator.CalculatorNavEvent
import com.example.calculator.CalculatorScreen
import com.example.calculator.CalculatorViewModel
import com.example.data.model.VaultType
import com.example.data.security.SecurityManager
import com.example.ui.theme.MyApplicationTheme
import com.example.vault.VaultEvent
import com.example.vault.VaultScreen
import com.example.vault.VaultViewModel

class MainActivity : ComponentActivity() {

    private val calculatorViewModel: CalculatorViewModel by viewModels()
    private val vaultViewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(
                        calculatorViewModel = calculatorViewModel,
                        vaultViewModel = vaultViewModel
                    )
                }
            }
        }
    }
}

enum class ScreenState {
    CALCULATOR,
    VAULT
}

@Composable
fun AppRoot(
    calculatorViewModel: CalculatorViewModel,
    vaultViewModel: VaultViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentScreen by remember { mutableStateOf(ScreenState.CALCULATOR) }
    var showFakeCrashDialog by remember { mutableStateOf(false) }

    val calcUiState by calculatorViewModel.uiState.collectAsStateWithLifecycle()
    val vaultUiState by vaultViewModel.uiState.collectAsStateWithLifecycle()

    val securityManager = remember { SecurityManager.getInstance(context) }

    // Auto-lock when activity goes to background (onStop / onPause)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && securityManager.isAutoLockEnabled()) {
                currentScreen = ScreenState.CALCULATOR
                calculatorViewModel.onClear()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle Calculator Navigation Events
    LaunchedEffect(Unit) {
        calculatorViewModel.navEvents.collect { event ->
            when (event) {
                is CalculatorNavEvent.OpenVault -> {
                    vaultViewModel.initVault(event.vaultType)
                    currentScreen = ScreenState.VAULT
                }
                is CalculatorNavEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is CalculatorNavEvent.ShowFakeCrash -> {
                    showFakeCrashDialog = true
                }
            }
        }
    }

    // Handle Vault Navigation & Action Events
    LaunchedEffect(Unit) {
        vaultViewModel.events.collect { event ->
            when (event) {
                is VaultEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is VaultEvent.OpenExternalFile -> {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(event.uri, event.mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Open file with"))
                    } catch (_: Exception) {
                        Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
                    }
                }
                is VaultEvent.LockVault -> {
                    calculatorViewModel.onClear()
                    currentScreen = ScreenState.CALCULATOR
                }
                is VaultEvent.LaunchGalleryDelete -> {
                    // Handled within VaultScreen's launcher
                }
            }
        }
    }

    when (currentScreen) {
        ScreenState.CALCULATOR -> {
            CalculatorScreen(
                viewModel = calculatorViewModel,
                uiState = calcUiState
            )
        }
        ScreenState.VAULT -> {
            VaultScreen(
                viewModel = vaultViewModel,
                uiState = vaultUiState,
                onLockToCalculator = {
                    calculatorViewModel.onClear()
                    currentScreen = ScreenState.CALCULATOR
                }
            )
        }
    }

    // Fake Crash Alert for stealth protection
    if (showFakeCrashDialog) {
        AlertDialog(
            onDismissRequest = {
                showFakeCrashDialog = false
                vaultViewModel.initVault(VaultType.DECOY)
                currentScreen = ScreenState.VAULT
            },
            title = { Text("Calculator has stopped") },
            text = { Text("Unfortunately, Calculator has encountered an unexpected error.") },
            confirmButton = {
                Button(
                    onClick = {
                        showFakeCrashDialog = false
                        vaultViewModel.initVault(VaultType.DECOY)
                        currentScreen = ScreenState.VAULT
                    }
                ) {
                    Text("Open Decoy Space")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showFakeCrashDialog = false
                    }
                ) {
                    Text("Close App")
                }
            }
        )
    }
}
