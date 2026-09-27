package com.example.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.VaultType
import com.example.data.security.PinMatchResult
import com.example.data.security.SecurityManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CalculationHistoryItem(
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface CalculatorNavEvent {
    data class OpenVault(val vaultType: VaultType) : CalculatorNavEvent
    data class ShowToast(val message: String) : CalculatorNavEvent
    data object ShowFakeCrash : CalculatorNavEvent
}

data class CalculatorUiState(
    val expression: String = "",
    val resultPreview: String = "",
    val isEvaluated: Boolean = false,
    val history: List<CalculationHistoryItem> = emptyList(),
    val isHistoryVisible: Boolean = false,
    val isSecurityConfigured: Boolean = false,
    val showSetupPinDialog: Boolean = false,
    val showRecoveryDialog: Boolean = false,
    val setupInitialPinSuggestion: String = "",
    val infoBannerVisible: Boolean = false
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val securityManager = SecurityManager.getInstance(application)

    private val _uiState = MutableStateFlow(
        CalculatorUiState(
            isSecurityConfigured = securityManager.isConfigured()
        )
    )
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private val _navEvents = MutableSharedFlow<CalculatorNavEvent>()
    val navEvents: SharedFlow<CalculatorNavEvent> = _navEvents.asSharedFlow()

    private var rawEnteredDigits = StringBuilder()

    init {
        checkSecurityStatus()
    }

    fun checkSecurityStatus() {
        val configured = securityManager.isConfigured()
        _uiState.value = _uiState.value.copy(
            isSecurityConfigured = configured,
            infoBannerVisible = !configured
        )
    }

    fun onDigit(digit: String) {
        val currentExpr = if (_uiState.value.isEvaluated) "" else _uiState.value.expression
        val newExpr = currentExpr + digit
        rawEnteredDigits.append(digit)

        val preview = if (hasOperator(newExpr)) {
            CalculatorEngine.evaluate(newExpr).resultString
        } else ""

        _uiState.value = _uiState.value.copy(
            expression = newExpr,
            resultPreview = preview,
            isEvaluated = false
        )
    }

    fun onOperator(op: String) {
        rawEnteredDigits.clear()
        val currentExpr = _uiState.value.expression
        if (currentExpr.isEmpty()) {
            if (op == "−") {
                _uiState.value = _uiState.value.copy(expression = "−", isEvaluated = false)
            }
            return
        }

        val lastChar = currentExpr.last().toString()
        val expr = if (lastChar in listOf("+", "−", "×", "÷")) {
            currentExpr.dropLast(1) + op
        } else {
            "$currentExpr $op "
        }

        _uiState.value = _uiState.value.copy(
            expression = expr,
            isEvaluated = false
        )
    }

    fun onDecimal() {
        val currentExpr = _uiState.value.expression
        val tokens = currentExpr.split(" ")
        val lastToken = tokens.lastOrNull() ?: ""
        if (!lastToken.contains(".")) {
            val toAdd = if (lastToken.isEmpty() || lastToken in listOf("+", "−", "×", "÷")) "0." else "."
            _uiState.value = _uiState.value.copy(
                expression = currentExpr + toAdd,
                isEvaluated = false
            )
        }
    }

    fun onPercentage() {
        val currentExpr = _uiState.value.expression
        if (currentExpr.isNotEmpty()) {
            val eval = CalculatorEngine.evaluate("$currentExpr %")
            _uiState.value = _uiState.value.copy(
                expression = eval.resultString,
                resultPreview = "",
                isEvaluated = true
            )
        }
    }

    fun onNegate() {
        val currentExpr = _uiState.value.expression
        if (currentExpr.isEmpty()) return

        val tokens = currentExpr.split(" ").toMutableList()
        val last = tokens.lastOrNull() ?: return
        if (last.toDoubleOrNull() != null) {
            val negated = if (last.startsWith("-")) last.substring(1) else "-$last"
            tokens[tokens.lastIndex] = negated
            val newExpr = tokens.joinToString(" ")
            _uiState.value = _uiState.value.copy(expression = newExpr)
        }
    }

    fun onBackspace() {
        val currentExpr = _uiState.value.expression
        if (currentExpr.isNotEmpty()) {
            val trimmed = currentExpr.trimEnd()
            val newExpr = if (trimmed.endsWith(" ")) {
                trimmed.dropLast(1)
            } else {
                trimmed.dropLast(1).trimEnd()
            }
            if (rawEnteredDigits.isNotEmpty()) {
                rawEnteredDigits.deleteCharAt(rawEnteredDigits.length - 1)
            }

            val preview = if (hasOperator(newExpr)) {
                CalculatorEngine.evaluate(newExpr).resultString
            } else ""

            _uiState.value = _uiState.value.copy(
                expression = newExpr,
                resultPreview = preview,
                isEvaluated = false
            )
        }
    }

    fun onClear() {
        rawEnteredDigits.clear()
        _uiState.value = _uiState.value.copy(
            expression = "",
            resultPreview = "",
            isEvaluated = false
        )
    }

    fun onEquals() {
        val expr = _uiState.value.expression.trim()
        if (expr.isEmpty()) return

        // 1. Check if input is a pure numeric PIN
        val numericOnly = expr.replace(" ", "")
        if (numericOnly.all { it.isDigit() } && numericOnly.length in 4..16) {
            if (!securityManager.isConfigured()) {
                // First-time setup: prompt user to confirm this PIN as their Master Code
                _uiState.value = _uiState.value.copy(
                    setupInitialPinSuggestion = numericOnly,
                    showSetupPinDialog = true
                )
                return
            }

            // Check against stored PINs
            when (securityManager.checkPin(numericOnly)) {
                PinMatchResult.PRIMARY -> {
                    onClear()
                    viewModelScope.launch {
                        _navEvents.emit(CalculatorNavEvent.OpenVault(VaultType.PRIMARY))
                    }
                    return
                }
                PinMatchResult.DECOY -> {
                    onClear()
                    viewModelScope.launch {
                        if (securityManager.isFakeCrashEnabled()) {
                            _navEvents.emit(CalculatorNavEvent.ShowFakeCrash)
                        } else {
                            _navEvents.emit(CalculatorNavEvent.OpenVault(VaultType.DECOY))
                        }
                    }
                    return
                }
                PinMatchResult.BACKUP -> {
                    onClear()
                    viewModelScope.launch {
                        _navEvents.emit(CalculatorNavEvent.ShowToast("Backup PIN accepted. Unlocking Primary Vault."))
                        _navEvents.emit(CalculatorNavEvent.OpenVault(VaultType.PRIMARY))
                    }
                    return
                }
                PinMatchResult.NONE -> {
                    // Check for hardcoded emergency trigger: 987654321= or 112233= if forgotten, or continue normal evaluation
                    if (numericOnly == "000000" || numericOnly == "999999") {
                        openRecoveryDialog()
                        return
                    }
                }
            }
        }

        // 2. Perform normal calculation
        val result = CalculatorEngine.evaluate(expr)
        val historyItem = CalculationHistoryItem(
            expression = expr,
            result = result.resultString
        )

        _uiState.value = _uiState.value.copy(
            expression = result.resultString,
            resultPreview = "",
            isEvaluated = true,
            history = listOf(historyItem) + _uiState.value.history.take(49)
        )
        rawEnteredDigits.clear()
    }

    fun openSetupPinDialog(initialSuggestion: String = "") {
        _uiState.value = _uiState.value.copy(
            setupInitialPinSuggestion = initialSuggestion,
            showSetupPinDialog = true
        )
    }

    fun dismissSetupPinDialog() {
        _uiState.value = _uiState.value.copy(showSetupPinDialog = false)
        checkSecurityStatus()
    }

    fun openRecoveryDialog() {
        _uiState.value = _uiState.value.copy(showRecoveryDialog = true)
    }

    fun dismissRecoveryDialog() {
        _uiState.value = _uiState.value.copy(showRecoveryDialog = false)
        checkSecurityStatus()
    }

    fun toggleHistory() {
        _uiState.value = _uiState.value.copy(
            isHistoryVisible = !_uiState.value.isHistoryVisible
        )
    }

    fun clearHistory() {
        _uiState.value = _uiState.value.copy(history = emptyList())
    }

    fun dismissBanner() {
        _uiState.value = _uiState.value.copy(infoBannerVisible = false)
    }

    private fun hasOperator(expr: String): Boolean {
        return expr.contains("+") || expr.contains("−") || expr.contains("×") || expr.contains("÷")
    }
}
