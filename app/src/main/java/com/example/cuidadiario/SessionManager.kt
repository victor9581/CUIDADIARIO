package com.example.cuidadiario

import android.content.Context
import android.content.SharedPreferences

object SessionManager {

    private const val PREF_NAME = "cuidadiario_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_FAMILY_CODE = "family_code"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_BIOMETRIC_CONFIGURED = "biometric_configured" // Registra si ya se le preguntó

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveUserSession(context: Context, id: Int, name: String, email: String, familyCode: String) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putInt(KEY_USER_ID, id)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_FAMILY_CODE, familyCode)
            apply()
        }
    }

    // Guarda si el usuario activó la huella y marca que ya fue consultado
    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            putBoolean(KEY_BIOMETRIC_CONFIGURED, true)
            apply()
        }
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    // Verifica si ya se le preguntó al usuario sobre la biometría
    fun isBiometricConfigured(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_CONFIGURED, false)
    }

    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserId(context: Context): Int {
        return getPrefs(context).getInt(KEY_USER_ID, 1)
    }

    fun getUserName(context: Context): String {
        return getPrefs(context).getString(KEY_USER_NAME, "Tutor Familiar") ?: "Tutor Familiar"
    }

    fun getUserEmail(context: Context): String {
        return getPrefs(context).getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun getFamilyCode(context: Context): String {
        return getPrefs(context).getString(KEY_FAMILY_CODE, "CD-1001") ?: "CD-1001"
    }

    fun logout(context: Context) {
        getPrefs(context).edit().apply {
            clear()
            putBoolean(KEY_IS_LOGGED_IN, false)
            putBoolean(KEY_BIOMETRIC_ENABLED, false)
            putBoolean(KEY_BIOMETRIC_CONFIGURED, false)
            commit() // Forzar guardado síncrono
        }
    }
}