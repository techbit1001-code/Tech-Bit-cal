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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.VaultType
import com.example.data.security.SecurityManager

@Composable
fun SecuritySettingsDialog(
    securityManager: SecurityManager,
    currentVaultType: VaultType,
    onDismiss: () -> Unit,
    onToast: (String) -> Unit
) {
    var primaryPinInput by remember { mutableStateOf("") }
    var decoyPinInput by remember { mutableStateOf("") }
    var backupPinInput by remember { mutableStateOf("") }
    var securityQuestionInput by remember { mutableStateOf(securityManager.getSecurityQuestion()) }
    var securityAnswerInput by remember { mutableStateOf("") }
    var fakeCrashEnabled by remember { mutableStateOf(securityManager.isFakeCrashEnabled()) }
    var autoLockEnabled by remember { mutableStateOf(securityManager.isAutoLockEnabled()) }
    var deleteFromGalleryEnabled by remember { mutableStateOf(securityManager.isDeleteFromGalleryEnabled()) }

    var expandedSection by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (currentVaultType == VaultType.PRIMARY) "Vault Security Settings" else "Decoy Space Settings",
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentVaultType == VaultType.PRIMARY) {
                    // Primary Vault: full controls
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "1. Change Master PIN",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = primaryPinInput,
                                onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) primaryPinInput = it },
                                label = { Text("New Primary PIN (4-8 digits)") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_primary_pin_input")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (primaryPinInput.length in 4..8) {
                                        securityManager.setPrimaryPin(primaryPinInput)
                                        primaryPinInput = ""
                                        onToast("Primary PIN successfully updated!")
                                    } else {
                                        onToast("Primary PIN must be 4-8 digits")
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("update_primary_pin_button")
                            ) {
                                Text("Update PIN")
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "2. Decoy Vault PIN",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Enter this PIN on calculator to open a decoy vault with harmless files if pressured by someone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = decoyPinInput,
                                onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) decoyPinInput = it },
                                label = { Text("Decoy PIN (4-8 digits)") },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_decoy_pin_input")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        if (decoyPinInput.length in 4..8) {
                                            securityManager.setDecoyPin(decoyPinInput)
                                            decoyPinInput = ""
                                            onToast("Decoy PIN saved!")
                                        } else {
                                            onToast("Decoy PIN must be 4-8 digits")
                                        }
                                    },
                                    modifier = Modifier.testTag("save_decoy_pin_button")
                                ) {
                                    Text("Save Decoy PIN")
                                }
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "3. Emergency Backup PIN",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Use this code on calculator or in recovery dialog if you forget your primary PIN.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = backupPinInput,
                                onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) backupPinInput = it },
                                label = { Text("Backup Recovery PIN") },
                                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_backup_pin_input")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (backupPinInput.length in 4..8) {
                                        securityManager.setBackupPin(backupPinInput)
                                        backupPinInput = ""
                                        onToast("Backup recovery PIN saved!")
                                    } else {
                                        onToast("Backup PIN must be 4-8 digits")
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("save_backup_pin_button")
                            ) {
                                Text("Save Backup PIN")
                            }
                        }
                    }

                    HorizontalDivider()

                    // Additional stealth options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fake App Crash on Decoy", style = MaterialTheme.typography.titleSmall)
                            Text("Show fake crash message before opening decoy vault", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = fakeCrashEnabled,
                            onCheckedChange = {
                                fakeCrashEnabled = it
                                securityManager.setFakeCrashEnabled(it)
                            },
                            modifier = Modifier.testTag("fake_crash_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto Lock on Minimize", style = MaterialTheme.typography.titleSmall)
                            Text("Immediately lock back to calculator when leaving app", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = autoLockEnabled,
                            onCheckedChange = {
                                autoLockEnabled = it
                                securityManager.setAutoLockEnabled(it)
                            },
                            modifier = Modifier.testTag("auto_lock_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Delete Originals from Gallery", style = MaterialTheme.typography.titleSmall)
                            Text("Remove from main gallery after hiding so files only exist in vault", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = deleteFromGalleryEnabled,
                            onCheckedChange = {
                                deleteFromGalleryEnabled = it
                                securityManager.setDeleteFromGalleryEnabled(it)
                            },
                            modifier = Modifier.testTag("delete_from_gallery_switch")
                        )
                    }
                } else {
                    // Decoy Vault Settings
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Decoy Vault Active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You are currently inside the secondary decoy partition. Files here are isolated and dummy. Master PIN cannot be modified from decoy mode for your privacy.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_settings_button")
            ) {
                Text("Close")
            }
        }
    )
}
