package com.example.vault.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
fun SetupPinDialog(
    initialPrimaryPin: String = "",
    onDismiss: () -> Unit,
    onSave: (
        primaryPin: String,
        decoyPin: String?,
        backupPin: String?,
        question: String?,
        answer: String?
    ) -> Unit
) {
    var primaryPin by remember { mutableStateOf(initialPrimaryPin) }
    var confirmPrimaryPin by remember { mutableStateOf(initialPrimaryPin) }
    var decoyPin by remember { mutableStateOf("") }
    var backupPin by remember { mutableStateOf("") }
    var securityQuestion by remember { mutableStateOf("What was your first pet's name?") }
    var securityAnswer by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Vault Passcode Setup",
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
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Primary PIN") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Decoy & Backup") }
                    )
                }

                if (selectedTab == 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = "How to open: Type your secret PIN into the calculator and tap the '=' button to unlock your hidden files.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = primaryPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) primaryPin = it },
                        label = { Text("Primary PIN (4-8 digits)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("primary_pin_input")
                    )

                    OutlinedTextField(
                        value = confirmPrimaryPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) confirmPrimaryPin = it },
                        label = { Text("Confirm Primary PIN") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_primary_pin_input")
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Decoy Vault Protection",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Entering your Decoy PIN opens a fake secondary vault with harmless dummy data to protect your real vault under duress.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    OutlinedTextField(
                        value = decoyPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) decoyPin = it },
                        label = { Text("Decoy PIN (Optional, 4-8 digits)") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("decoy_pin_input")
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "Backup PIN & Recovery",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = backupPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) backupPin = it },
                        label = { Text("Backup Recovery PIN (4-8 digits)") },
                        leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_pin_input")
                    )

                    OutlinedTextField(
                        value = securityQuestion,
                        onValueChange = { securityQuestion = it },
                        label = { Text("Security Question") },
                        leadingIcon = { Icon(Icons.Default.QuestionMark, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_question_input")
                    )

                    OutlinedTextField(
                        value = securityAnswer,
                        onValueChange = { securityAnswer = it },
                        label = { Text("Answer to Security Question") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_answer_input")
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (primaryPin.length < 4) {
                        errorMessage = "Primary PIN must be at least 4 digits."
                        selectedTab = 0
                        return@Button
                    }
                    if (primaryPin != confirmPrimaryPin) {
                        errorMessage = "Primary PINs do not match."
                        selectedTab = 0
                        return@Button
                    }
                    if (decoyPin.isNotEmpty() && decoyPin == primaryPin) {
                        errorMessage = "Decoy PIN must be different from Primary PIN."
                        selectedTab = 1
                        return@Button
                    }
                    if (backupPin.isNotEmpty() && backupPin == primaryPin) {
                        errorMessage = "Backup PIN must be different from Primary PIN."
                        selectedTab = 1
                        return@Button
                    }

                    errorMessage = null
                    onSave(
                        primaryPin,
                        decoyPin.ifBlank { null },
                        backupPin.ifBlank { null },
                        securityQuestion.ifBlank { null },
                        securityAnswer.ifBlank { null }
                    )
                },
                modifier = Modifier.testTag("save_pin_setup_button")
            ) {
                Text("Activate Vault")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_pin_setup_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
