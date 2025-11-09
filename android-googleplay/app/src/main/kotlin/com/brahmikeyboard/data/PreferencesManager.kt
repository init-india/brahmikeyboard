package com.brahmikeyboard.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("brahmi_prefs", Context.MODE_PRIVATE)
    
    // Core functionality matching your XML preferences
    fun getReferenceScript(): String {
        return prefs.getString("reference_script", "devanagari") ?: "devanagari"
    }
    
    fun setReferenceScript(script: String) {
        prefs.edit().putString("reference_script", script).apply()
    }
    
    fun getCommitDelay(): Long {
        return prefs.getLong("commit_delay", 1000L)
    }
    
    fun setCommitDelay(delay: Long) {
        prefs.edit().putLong("commit_delay", delay).apply()
    }
    
    fun getDefaultMode(): String? {
        return prefs.getString("default_mode", "brahmi")
    }
    
    fun setDefaultMode(mode: String) {
        prefs.edit().putString("default_mode", mode).apply()
    }
    
    // Additional utility methods for future features
    fun getReferenceLanguage(): String {
        return getReferenceScript()
    }
    
    fun setReferenceLanguage(language: String) {
        setReferenceScript(language)
    }
    
    // Google Play specific enhancements (can be added later)
    fun setCloudSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cloud_sync_enabled", enabled).apply()
    }
    
    fun isCloudSyncEnabled(): Boolean {
        return prefs.getBoolean("cloud_sync_enabled", false)
    }
    
    fun setAnalyticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("analytics_enabled", true).apply() // Default enabled for Google Play
    }
    
    fun isAnalyticsEnabled(): Boolean {
        return prefs.getBoolean("analytics_enabled", true)
    }
    
    // Utility methods
    fun getLastUsedScript(): String {
        return prefs.getString("last_used_script", getReferenceScript()) ?: getReferenceScript()
    }
    
    fun setLastUsedScript(script: String) {
        prefs.edit().putString("last_used_script", script).apply()
    }
    
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
