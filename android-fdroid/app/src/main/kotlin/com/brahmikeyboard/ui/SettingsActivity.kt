package com.brahmikeyboard.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.brahmikeyboard.R
import com.brahmikeyboard.data.PreferencesManager

class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }
    
    class SettingsFragment : PreferenceFragmentCompat() {
        
        private lateinit var preferences: PreferencesManager
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)
            preferences = PreferencesManager(requireContext())
            setupPreferences()
        }
        
        private fun setupPreferences() {
            // Reference Script Preference
            val scriptPreference = findPreference<ListPreference>("reference_script")
            scriptPreference?.setOnPreferenceChangeListener { _, newValue ->
                preferences.setReferenceScript(newValue.toString())
                true
            }
            
            // Commit Delay Preference
            val delayPreference = findPreference<Preference>("commit_delay")
            delayPreference?.setOnPreferenceChangeListener { _, newValue ->
                preferences.setCommitDelay((newValue as String).toLong())
                true
            }
            
            // Keyboard Mode Preference
            val modePreference = findPreference<ListPreference>("default_mode")
            modePreference?.setOnPreferenceChangeListener { _, newValue ->
                // Handle default mode change
                true
            }
        }
    }
}
