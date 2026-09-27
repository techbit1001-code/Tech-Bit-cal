package com.example.vault.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RecoveryDialog(
    securityQuestion: String,
    onDismiss: () -> Unit,
    onVerifyBackupPin: (backupPin: String, newPin: String) -> Boolean,
    onVerifySecurityAnswer: (answer: String, newPin: String) -> Boolean,
    onSuccess: () -> Unit
) {
    var selectedMethod by remember { mutableIntStateOf(0) } // 0: Backup PIN, 1: Security Question
    var backupPinInput by remember { mutableStateOf("") }
    var securityAnswerInput by remember { mutableStateOf("") }
    var newPrimaryPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Passcode Recovery",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (successMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = successMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    TabRow(selectedTabIndex = selectedMethod) {
                        Tab(
                            selected = selectedMethod == 0,
                            onClick = { selectedMethod = 0; errorMessage = null },
                            text = { Text("Backup PIN") }
                        )
                        Tab(
                            selected = selectedMethod == 1,
                            onClick = { selectedMethod = 1; errorMessage = null },
                            text = { Text("Security Question") }
                        )
                    }

                    if (selectedMethod == 0) {
                        OutlinedTextField(
                            value = backupPinInput,
                            onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) backupPinInput = it },
                            label = { Text("Enter Backup PIN") },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recovery_backup_pin_input")
                        )
                    } else {
                        Text(
                            text = securityQuestion,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = securityAnswerInput,
                            onValueChange = { securityAnswerInput = it },
                            label = { Text("Your Answer") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recovery_security_answer_input")
                        )
                    }

                    Text(
                        text = "Set New Primary PIN",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = newPrimaryPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) newPrimaryPin = it },
                        label = { Text("New PIN (4-8 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_primary_pin_input")
                    )

                    OutlinedTextField(
                        value = confirmNewPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) confirmNewPin = it },
                        label = { Text("Confirm New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_new_primary_pin_input")
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (successMessage != null) {
                Button(
                    onClick = {
                        onSuccess()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("recovery_done_button")
                ) {
                    Text("Done")
                }
            } else {
                Button(
                    onClick = {
                        if (newPrimaryPin.length < 4) {
                            errorMessage = "New PIN must be at least 4 digits."
                            return@Button
                        }
                        if (newPrimaryPin != confirmNewPin) {
                            errorMessage = "New PINs do not match."
                            return@Button
                        }

                        val success = if (selectedMethod == 0) {
                            onVerifyBackupPin(backupPinInput, newPrimaryPin)
                        } else {
                            onVerifySecurityAnswer(securityAnswerInput, newPrimaryPin)
                        }

                        if (success) {
                            errorMessage = null
                            successMessage = "Primary PIN successfully reset! You can now unlock the vault using your new code and '='."
                        } else {
                            errorMessage = if (selectedMethod == 0) {
                                "Incorrect Backup PIN. Please try again."
                            } else {
                                "Incorrect answer to security question."
                            }
                        }
                    },
                    modifier = Modifier.testTag("verify_recovery_button")
                ) {
                    Text("Reset PIN")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_recovery_button")
            ) {
                Text(if (successMessage != null) "Close" else "Cancel")
            }
        }
    )
}
