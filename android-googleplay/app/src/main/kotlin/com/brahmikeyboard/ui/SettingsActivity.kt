package com.brahmikeyboard.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
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
            .replace(R.id.settings_container, SettingsFragment().apply {
                setPreferencesManager(preferencesManager)
            })
            .commit()
    }
    
    class SettingsFragment : PreferenceFragmentCompat() {
        
        private var preferencesManager: PreferencesManager? = null
        
        fun setPreferencesManager(manager: PreferencesManager) {
            this.preferencesManager = manager
        }
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)
            setupPreferenceListeners()
        }
        
        private fun setupPreferenceListeners() {
            // Reference Script preference
            findPreference("reference_script")?.setOnPreferenceChangeListener { preference, newValue ->
                preferencesManager?.setReferenceScript(newValue as String)
                updatePreferenceSummaries()
                true
            }
            
            // Default Mode preference
            findPreference("default_mode")?.setOnPreferenceChangeListener { preference, newValue ->
                preferencesManager?.setDefaultMode(newValue as String)
                updatePreferenceSummaries()
                true
            }
            
            // Commit Delay preference
            findPreference("commit_delay")?.setOnPreferenceChangeListener { preference, newValue ->
                try {
                    val delay = (newValue as String).toLong()
                    preferencesManager?.setCommitDelay(delay)
                    updatePreferenceSummaries()
                } catch (e: NumberFormatException) {
                    // Handle invalid input
                }
                true
            }
        }
        
        override fun onResume() {
            super.onResume()
            updatePreferenceSummaries()
        }
        
        private fun updatePreferenceSummaries() {
            preferencesManager?.let { prefs ->
                // Update reference script summary to show current selection
                findPreference("reference_script")?.summary = 
                    "Current: ${getScriptDisplayName(prefs.getReferenceScript())}"
                
                // Update commit delay summary  
                findPreference("commit_delay")?.summary = 
                    "Current: ${prefs.getCommitDelay()}ms"
                
                // Update default mode summary
                findPreference("default_mode")?.summary = 
                    "Current: ${getModeDisplayName(prefs.getDefaultMode() ?: "brahmi")}"
            }
        }
        
        private fun getScriptDisplayName(scriptValue: String): String {
            return when (scriptValue) {
                "devanagari" -> "Devanagari"
                "bengali" -> "Bengali"
                "tamil" -> "Tamil"
                "telugu" -> "Telugu"
                "kannada" -> "Kannada"
                "malayalam" -> "Malayalam"
                "gujarati" -> "Gujarati"
                "punjabi" -> "Punjabi"
                else -> scriptValue.replaceFirstChar { it.uppercase() }
            }
        }
        
        private fun getModeDisplayName(modeValue: String): String {
            return when (modeValue) {
                "brahmi" -> "Brahmi Mode"
                "direct" -> "Direct Input"
                else -> modeValue.replaceFirstChar { it.uppercase() }
            }
        }
    }
}
