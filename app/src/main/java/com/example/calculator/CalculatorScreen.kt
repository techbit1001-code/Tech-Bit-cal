package com.example.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vault.components.RecoveryDialog
import com.example.vault.components.SetupPinDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    uiState: CalculatorUiState,
    modifier: Modifier = Modifier
) {
    var titleClickCount by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Calculator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                titleClickCount++
                                if (titleClickCount >= 4) {
                                    titleClickCount = 0
                                    viewModel.openRecoveryDialog()
                                }
                            }
                            .testTag("calculator_title")
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleHistory() },
                        modifier = Modifier.testTag("calculator_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Calculation History"
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("calculator_more_menu")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (!uiState.isSecurityConfigured) {
                                DropdownMenuItem(
                                    text = { Text("Set Up Vault Passcode") },
                                    leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.openSetupPinDialog()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Forgot Passcode? (Recovery)") },
                                leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.openRecoveryDialog()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // First launch subtle banner
            AnimatedVisibility(
                visible = uiState.infoBannerVisible && !uiState.isSecurityConfigured,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Disguised Vault Active",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enter a 4-8 digit code and press '=' to configure or open your hidden vault.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(
                            onClick = { viewModel.dismissBanner() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Calculation Display Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState, reverseScrolling = true),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (uiState.expression.isEmpty()) "0" else uiState.expression,
                            fontSize = if (uiState.expression.length > 10) 36.sp else 48.sp,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier.testTag("calculator_expression_display")
                        )
                    }

                    if (uiState.resultPreview.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "= ${uiState.resultPreview}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.End,
                            modifier = Modifier.testTag("calculator_preview_display")
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Calculator Keypad
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: AC, Backspace, %, ÷
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcKey(
                        text = "AC",
                        type = KeyType.FUNCTION,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onClear() },
                        testTag = "calc_key_ac"
                    )
                    CalcKey(
                        text = "⌫",
                        type = KeyType.FUNCTION,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onBackspace() },
                        testTag = "calc_key_backspace"
                    )
                    CalcKey(
                        text = "%",
                        type = KeyType.FUNCTION,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onPercentage() },
                        testTag = "calc_key_percent"
                    )
                    CalcKey(
                        text = "÷",
                        type = KeyType.OPERATOR,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onOperator("÷") },
                        testTag = "calc_key_divide"
                    )
                }

                // Row 2: 7, 8, 9, ×
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcKey(text = "7", type = KeyType.NUMBER, modifier = Modifier.weight(1f), onClick = { viewModel.onDigit("7") }, testTag = "calc_key_7")
                    CalcKey(text = "8", type = KeyType.NUMBER, modifier = Modifier.weight(1f), onClick = { viewModel.onDigit("8") }, testTag = "calc_key_8")
                    CalcKey(text = "9", type = KeyType.NUMBER, modifier = Modifier.weight(1f), onClick = { viewModel.onDigit("9") }, testTag = "calc_key_9")
                    CalcKey(text = "×", type = KeyType.OPERATOR, modifier = Modifier.weight(1f), onClick = { viewModel.onOperator("×") }, testTag = "calc_key_multiply")
                }

                // Row 3: 4, 5, 6, −
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcKey(text = "4", type = KeyType.NUMBER, modifier = Modifier.weight(1f), onClick = { viewModel.onDigit("4") }, testTag = "calc_key_4")
                    CalcKey(text = "5", type = KeyType.NUMBER, modifier = Modifier.weight(1f), onClick = { viewModel.onDigit("5") }, testTag = "calc_key_5")
                    CalcKey(text = "6", type = KeyType.NUMBER, modifier = Modifier.weight(1f), onClick = { viewModel.onDigit("6") }, testTag = "calc_key_6")
                    CalcKey(text = "−", type = KeyType.OPERATOR, modifier = Modifier.weight(1f), onClick = { viewModel.onOperator("−") }, testTag = "calc_key_subtract")
                }

                // Row 4: 1, 2, 3, +
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcKey(text = "1", type = KeyType.NUMBER, modifier = Modifier.weight(1f), onClick = { viewModel.onDigit("1") }, testTag = "calc_key_1")
                    CalcKey(text = "2", type = KeyType.NUMBER, modifier = Modifier.weight(1f), onClick = { viewModel.onDigit("2") }, testTag = "calc_key_2")
                    CalcKey(text = "3", type = KeyType.NUMBER, modifier = Modifier.weight(1f), onClick = { viewModel.onDigit("3") }, testTag = "calc_key_3")
                    CalcKey(text = "+", type = KeyType.OPERATOR, modifier = Modifier.weight(1f), onClick = { viewModel.onOperator("+") }, testTag = "calc_key_add")
                }

                // Row 5: +/-, 0, ., =
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcKey(text = "+/-", type = KeyType.NUMBER, modifier = Modifier.weight(1f), onClick = { viewModel.onNegate() }, testTag = "calc_key_negate")
                    CalcKey(text = "0", type = KeyType.NUMBER, modifier = Modifier.weight(1f), onClick = { viewModel.onDigit("0") }, testTag = "calc_key_0")
                    CalcKey(text = ".", type = KeyType.NUMBER, modifier = Modifier.weight(1f), onClick = { viewModel.onDecimal() }, testTag = "calc_key_dot")
                    CalcKey(
                        text = "=",
                        type = KeyType.EQUALS,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onEquals() },
                        onLongClick = { viewModel.openRecoveryDialog() },
                        testTag = "calc_key_equals"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Calculation History BottomSheet
    if (uiState.isHistoryVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleHistory() },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.history.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text("Clear")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recent calculations", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.history) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.onClear()
                                        for (char in item.result) {
                                            if (char.isDigit()) viewModel.onDigit(char.toString())
                                        }
                                        viewModel.toggleHistory()
                                    }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = item.expression,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "= ${item.result}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }

    // Setup PIN Dialog
    if (uiState.showSetupPinDialog) {
        val secMgr = remember { com.example.data.security.SecurityManager.getInstance(viewModel.getApplication()) }
        SetupPinDialog(
            initialPrimaryPin = uiState.setupInitialPinSuggestion,
            onDismiss = { viewModel.dismissSetupPinDialog() },
            onSave = { primaryPin, decoyPin, backupPin, q, a ->
                secMgr.setupInitialPins(
                    primaryPin = primaryPin,
                    decoyPin = decoyPin,
                    backupPin = backupPin,
                    securityQuestion = q,
                    securityAnswer = a
                )
                viewModel.dismissSetupPinDialog()
            }
        )
    }

    // Passcode Recovery Dialog
    if (uiState.showRecoveryDialog) {
        val secMgr = remember { com.example.data.security.SecurityManager.getInstance(viewModel.getApplication()) }
        RecoveryDialog(
            securityQuestion = secMgr.getSecurityQuestion(),
            onDismiss = { viewModel.dismissRecoveryDialog() },
            onVerifyBackupPin = { backupPin, newPrimaryPin ->
                if (secMgr.verifyBackupPin(backupPin)) {
                    secMgr.setPrimaryPin(newPrimaryPin)
                    true
                } else false
            },
            onVerifySecurityAnswer = { answer, newPrimaryPin ->
                if (secMgr.verifySecurityAnswer(answer)) {
                    secMgr.setPrimaryPin(newPrimaryPin)
                    true
                } else false
            },
            onSuccess = {
                viewModel.dismissRecoveryDialog()
            }
        )
    }
}

enum class KeyType {
    NUMBER,
    OPERATOR,
    FUNCTION,
    EQUALS
}

@Composable
fun CalcKey(
    text: String,
    type: KeyType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    testTag: String
) {
    val containerColor = when (type) {
        KeyType.NUMBER -> MaterialTheme.colorScheme.surfaceContainerHigh
        KeyType.OPERATOR -> MaterialTheme.colorScheme.primaryContainer
        KeyType.FUNCTION -> MaterialTheme.colorScheme.secondaryContainer
        KeyType.EQUALS -> MaterialTheme.colorScheme.primary
    }

    val contentColor = when (type) {
        KeyType.NUMBER -> MaterialTheme.colorScheme.onSurface
        KeyType.OPERATOR -> MaterialTheme.colorScheme.onPrimaryContainer
        KeyType.FUNCTION -> MaterialTheme.colorScheme.onSecondaryContainer
        KeyType.EQUALS -> MaterialTheme.colorScheme.onPrimary
    }

    val fontSize = when {
        text.length > 2 -> 20.sp
        type == KeyType.OPERATOR || type == KeyType.EQUALS -> 28.sp
        else -> 26.sp
    }

    Surface(
        modifier = modifier
            .aspectRatio(1.1f)
            .clip(CircleShape)
            .clickable(
                onClick = onClick
            )
            .testTag(testTag),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = if (type == KeyType.NUMBER) FontWeight.Normal else FontWeight.Medium
            )
        }
    }
}
