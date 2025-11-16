package com.brahmikeyboard.data

import android.content.Context
import android.content.SharedPreferences
import com.brahmikeyboard.engine.KeyboardMode

class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("brahmi_prefs", Context.MODE_PRIVATE)
    
    // Mode management
    fun getCurrentMode(): KeyboardMode {
        val modeString = prefs.getString("current_mode", "BRAHMI") ?: "BRAHMI"
        return when (modeString) {
            "ENGLISH" -> KeyboardMode.ENGLISH
            "PURE_BRAHMI" -> KeyboardMode.PURE_BRAHMI
            else -> KeyboardMode.BRAHMI
        }
    }
    
    fun setCurrentMode(mode: KeyboardMode) {
        val modeString = when (mode) {
            KeyboardMode.ENGLISH -> "ENGLISH"
            KeyboardMode.BRAHMI -> "BRAHMI"
            KeyboardMode.PURE_BRAHMI -> "PURE_BRAHMI"
        }
        prefs.edit().putString("current_mode", modeString).apply()
    }
    
    // Reference language
    fun getReferenceScript(): String {
        return prefs.getString("reference_script", "devanagari") ?: "devanagari"
    }
    
    fun setReferenceScript(script: String) {
        prefs.edit().putString("reference_script", script).apply()
    }
    
    // Keyboard layout state
    fun isNumpadActive(): Boolean {
        return prefs.getBoolean("numpad_active", false)
    }
    
    fun setNumpadActive(active: Boolean) {
        prefs.edit().putBoolean("numpad_active", active).apply()
    }
    
    fun isSymbolsActive(): Boolean {
        return prefs.getBoolean("symbols_active", false)
    }
    
    fun setSymbolsActive(active: Boolean) {
        prefs.edit().putBoolean("symbols_active", active).apply()
    }
    
    // Utility methods
    fun getReferenceLanguage(): String {
        return getReferenceScript()
    }
    
    fun setReferenceLanguage(language: String) {
        setReferenceScript(language)
    }
    
    fun getDefaultMode(): String? {
        return prefs.getString("default_mode", "brahmi")
    }
    
    fun setDefaultMode(mode: String) {
        prefs.edit().putString("default_mode", mode).apply()
    }
    
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
