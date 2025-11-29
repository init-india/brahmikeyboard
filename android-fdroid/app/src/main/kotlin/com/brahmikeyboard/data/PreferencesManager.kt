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
    
    // Reference language/script
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
    
    // Theme and customization
    fun getThemeMode(): String {
        return prefs.getString("theme_mode", "light") ?: "light"
    }
    
    fun setThemeMode(theme: String) {
        prefs.edit().putString("theme_mode", theme).apply()
    }
    
    fun getKeyShape(): String {
        return prefs.getString("key_shape", "square") ?: "square"
    }
    
    fun setKeyShape(shape: String) {
        prefs.edit().putString("key_shape", shape).apply()
    }
    
    fun getFontFamily(): String {
        return prefs.getString("font_family", "default") ?: "default"
    }
    
    fun setFontFamily(font: String) {
        prefs.edit().putString("font_family", font).apply()
    }
    
    fun getFontSize(): Int {
        return prefs.getInt("font_size", 16)
    }
    
    fun setFontSize(size: Int) {
        prefs.edit().putInt("font_size", size).apply()
    }
    
    fun getKeyHeight(): Int {
        return prefs.getInt("key_height", 48)
    }
    
    fun setKeyHeight(height: Int) {
        prefs.edit().putInt("key_height", height).apply()
    }
    
    fun getKeyWidth(): Int {
        return prefs.getInt("key_width", 40)
    }
    
    fun setKeyWidth(width: Int) {
        prefs.edit().putInt("key_width", width).apply()
    }
    
    // Keyboard dimensions
    fun getKeyboardHeightPortrait(): Int {
        return prefs.getInt("keyboard_height_portrait", 300)
    }
    
    fun setKeyboardHeightPortrait(height: Int) {
        prefs.edit().putInt("keyboard_height_portrait", height).apply()
    }
    
    fun getKeyboardHeightLandscape(): Int {
        return prefs.getInt("keyboard_height_landscape", 200)
    }
    
    fun setKeyboardHeightLandscape(height: Int) {
        prefs.edit().putInt("keyboard_height_landscape", height).apply()
    }
    
    // Color customization
    fun getKeyboardBackgroundColor(): Int {
        return prefs.getInt("keyboard_bg_color", 0xFFF0F0F0.toInt())
    }
    
    fun setKeyboardBackgroundColor(color: Int) {
        prefs.edit().putInt("keyboard_bg_color", color).apply()
    }
    
    fun getKeyBackgroundColor(): Int {
        return prefs.getInt("key_bg_color", 0xFFFFFFFF.toInt())
    }
    
    fun setKeyBackgroundColor(color: Int) {
        prefs.edit().putInt("key_bg_color", color).apply()
    }
    
    fun getKeyTextColor(): Int {
        return prefs.getInt("key_text_color", 0xFF000000.toInt())
    }
    
    fun setKeyTextColor(color: Int) {
        prefs.edit().putInt("key_text_color", color).apply()
    }
    
    fun getPreviewBackgroundColor(): Int {
        return prefs.getInt("preview_bg_color", 0xFFFFFFFF.toInt())
    }
    
    fun setPreviewBackgroundColor(color: Int) {
        prefs.edit().putInt("preview_bg_color", color).apply()
    }
    
    fun getPreviewTextColor(): Int {
        return prefs.getInt("preview_text_color", 0xFF333333.toInt())
    }
    
    fun setPreviewTextColor(color: Int) {
        prefs.edit().putInt("preview_text_color", color).apply()
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
    
    // Vibration and feedback
    fun getVibrationEnabled(): Boolean {
        return prefs.getBoolean("vibration_feedback", true)
    }
    
    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("vibration_feedback", enabled).apply()
    }
    
    fun getVibrationDuration(): Int {
        return prefs.getInt("vibration_duration", 20)
    }
    
    fun setVibrationDuration(duration: Int) {
        prefs.edit().putInt("vibration_duration", duration).apply()
    }
    
    fun getSoundEnabled(): Boolean {
        return prefs.getBoolean("sound_feedback", true)
    }
    
    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_feedback", enabled).apply()
    }
    
    // Auto-correction and suggestions
    fun getAutoCorrectEnabled(): Boolean {
        return prefs.getBoolean("auto_correct", true)
    }
    
    fun setAutoCorrectEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_correct", enabled).apply()
    }
    
    fun getSuggestionsEnabled(): Boolean {
        return prefs.getBoolean("suggestions", true)
    }
    
    fun setSuggestionsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("suggestions", enabled).apply()
    }
    
    // Reset to defaults
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
    
    // First run detection
    fun isFirstRun(): Boolean {
        return prefs.getBoolean("first_run", true)
    }
    
    fun setFirstRunCompleted() {
        prefs.edit().putBoolean("first_run", false).apply()
    }
}
