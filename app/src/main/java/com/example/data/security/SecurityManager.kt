package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

enum class PinMatchResult {
    PRIMARY,
    DECOY,
    BACKUP,
    NONE
}

class SecurityManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("calc_vault_sec", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PRIMARY_HASH = "primary_hash"
        private const val KEY_DECOY_HASH = "decoy_hash"
        private const val KEY_BACKUP_HASH = "backup_hash"
        private const val KEY_SECURITY_QUESTION = "sec_question"
        private const val KEY_SECURITY_ANSWER_HASH = "sec_answer_hash"
        private const val KEY_FAKE_CRASH = "fake_crash"
        private const val KEY_CONFIGURED = "is_configured"
        private const val KEY_AUTO_LOCK = "auto_lock"
        private const val SALT = "C@lcV@ult_S3cur1ty_S@lt_2026#"

        @Volatile
        private var INSTANCE: SecurityManager? = null

        fun getInstance(context: Context): SecurityManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SecurityManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest((SALT + input.trim()).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isConfigured(): Boolean {
        return prefs.getBoolean(KEY_CONFIGURED, false) && prefs.getString(KEY_PRIMARY_HASH, null) != null
    }

    fun setupInitialPins(
        primaryPin: String,
        decoyPin: String? = null,
        backupPin: String? = null,
        securityQuestion: String? = null,
        securityAnswer: String? = null
    ) {
        val editor = prefs.edit()
        editor.putString(KEY_PRIMARY_HASH, hash(primaryPin))
        if (!decoyPin.isNullOrBlank()) {
            editor.putString(KEY_DECOY_HASH, hash(decoyPin))
        }
        if (!backupPin.isNullOrBlank()) {
            editor.putString(KEY_BACKUP_HASH, hash(backupPin))
        }
        if (!securityQuestion.isNullOrBlank() && !securityAnswer.isNullOrBlank()) {
            editor.putString(KEY_SECURITY_QUESTION, securityQuestion.trim())
            editor.putString(KEY_SECURITY_ANSWER_HASH, hash(securityAnswer.trim().lowercase()))
        }
        editor.putBoolean(KEY_CONFIGURED, true)
        editor.apply()
    }

    fun checkPin(enteredPin: String): PinMatchResult {
        if (!isConfigured()) return PinMatchResult.NONE
        val enteredHash = hash(enteredPin)
        val primaryHash = prefs.getString(KEY_PRIMARY_HASH, null)
        val decoyHash = prefs.getString(KEY_DECOY_HASH, null)
        val backupHash = prefs.getString(KEY_BACKUP_HASH, null)

        return when {
            primaryHash != null && enteredHash == primaryHash -> PinMatchResult.PRIMARY
            decoyHash != null && enteredHash == decoyHash -> PinMatchResult.DECOY
            backupHash != null && enteredHash == backupHash -> PinMatchResult.BACKUP
            else -> PinMatchResult.NONE
        }
    }

    fun setPrimaryPin(newPin: String) {
        prefs.edit().putString(KEY_PRIMARY_HASH, hash(newPin)).apply()
    }

    fun setDecoyPin(newPin: String?) {
        if (newPin.isNullOrBlank()) {
            prefs.edit().remove(KEY_DECOY_HASH).apply()
        } else {
            prefs.edit().putString(KEY_DECOY_HASH, hash(newPin)).apply()
        }
    }

    fun setBackupPin(newPin: String?) {
        if (newPin.isNullOrBlank()) {
            prefs.edit().remove(KEY_BACKUP_HASH).apply()
        } else {
            prefs.edit().putString(KEY_BACKUP_HASH, hash(newPin)).apply()
        }
    }

    fun setSecurityQuestionAndAnswer(question: String?, answer: String?) {
        val editor = prefs.edit()
        if (question.isNullOrBlank() || answer.isNullOrBlank()) {
            editor.remove(KEY_SECURITY_QUESTION)
            editor.remove(KEY_SECURITY_ANSWER_HASH)
        } else {
            editor.putString(KEY_SECURITY_QUESTION, question.trim())
            editor.putString(KEY_SECURITY_ANSWER_HASH, hash(answer.trim().lowercase()))
        }
        editor.apply()
    }

    fun verifyBackupPin(pin: String): Boolean {
        val backupHash = prefs.getString(KEY_BACKUP_HASH, null) ?: return false
        return hash(pin) == backupHash
    }

    fun verifySecurityAnswer(answer: String): Boolean {
        val storedHash = prefs.getString(KEY_SECURITY_ANSWER_HASH, null) ?: return false
        return hash(answer.trim().lowercase()) == storedHash
    }

    fun getSecurityQuestion(): String {
        return prefs.getString(KEY_SECURITY_QUESTION, "What is your favorite secret word?") ?: "What is your favorite secret word?"
    }

    fun hasDecoyPin(): Boolean = prefs.getString(KEY_DECOY_HASH, null) != null

    fun hasBackupPin(): Boolean = prefs.getString(KEY_BACKUP_HASH, null) != null

    fun hasSecurityQuestion(): Boolean = prefs.getString(KEY_SECURITY_ANSWER_HASH, null) != null

    fun isFakeCrashEnabled(): Boolean = prefs.getBoolean(KEY_FAKE_CRASH, false)

    fun setFakeCrashEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FAKE_CRASH, enabled).apply()
    }

    fun isAutoLockEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_LOCK, true)

    fun setAutoLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LOCK, enabled).apply()
    }

    fun isDeleteFromGalleryEnabled(): Boolean = prefs.getBoolean("delete_from_gallery", true)

    fun setDeleteFromGalleryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("delete_from_gallery", enabled).apply()
    }
}
