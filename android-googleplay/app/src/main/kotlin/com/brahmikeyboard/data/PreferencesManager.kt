package com.brahmikeyboard.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("brahmi_prefs", Context.MODE_PRIVATE)
    
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
    
    fun getReferenceLanguage(): String {
        return getReferenceScript()
    }
    
    fun setReferenceLanguage(language: String) {
        setReferenceScript(language)
    }
}
