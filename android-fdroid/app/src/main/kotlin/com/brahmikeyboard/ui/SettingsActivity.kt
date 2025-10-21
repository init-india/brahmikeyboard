package com.brahmikeyboard.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.brahmikeyboard.data.PreferencesManager
import com.brahmikeyboard.ime.foss.R

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        
        preferencesManager = PreferencesManager(this)
        
        setupPreferences()
    }
    
    private fun setupPreferences() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }
    
    class SettingsFragment : androidx.preference.PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)
        }
    }
}
