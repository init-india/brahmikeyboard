package com.brahmikeyboard.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.brahmikeyboard.R
import com.brahmikeyboard.data.PreferencesManager

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        
        preferencesManager = PreferencesManager(this)
        
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
            
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat(), 
                            SharedPreferences.OnSharedPreferenceChangeListener {
        
        private lateinit var preferencesManager: PreferencesManager
        
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            preferencesManager = PreferencesManager(requireContext())
        }
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)
            setupPreferenceListeners()
        }
        
        override fun onResume() {
            super.onResume()
            preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }
        
        override fun onPause() {
            super.onPause()
            preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        }
        
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                "reference_script" -> {
                    val newScript = sharedPreferences?.getString(key, "devanagari") ?: "devanagari"
                    preferencesManager.setReferenceScript(newScript)
                }
                "default_mode" -> {
                    val newMode = sharedPreferences?.getString(key, "brahmi") ?: "brahmi"
                    preferencesManager.setDefaultMode(newMode)
                }
                "theme_mode" -> {
                    val newTheme = sharedPreferences?.getString(key, "light") ?: "light"
                    preferencesManager.setThemeMode(newTheme)
                    // Apply theme changes immediately
                    requireActivity().recreate()
                }
                "key_shape" -> {
                    val newShape = sharedPreferences?.getString(key, "square") ?: "square"
                    preferencesManager.setKeyShape(newShape)
                }
                "font_family" -> {
                    val newFont = sharedPreferences?.getString(key, "default") ?: "default"
                    preferencesManager.setFontFamily(newFont)
                }
                "font_size" -> {
                    val newSize = sharedPreferences?.getInt(key, 16)
                    if (newSize != null) preferencesManager.setFontSize(newSize)
                }
                "key_height" -> {
                    val newHeight = sharedPreferences?.getInt(key, 48)
                    if (newHeight != null) preferencesManager.setKeyHeight(newHeight)
                }
                "vibration_feedback" -> {
                    val enabled = sharedPreferences?.getBoolean(key, true) ?: true
                    preferencesManager.setVibrationEnabled(enabled)
                }
                "vibration_duration" -> {
                    val duration = sharedPreferences?.getInt(key, 20)
                    if (duration != null) preferencesManager.setVibrationDuration(duration)
                }
                "sound_feedback" -> {
                    val enabled = sharedPreferences?.getBoolean(key, true) ?: true
                    preferencesManager.setSoundEnabled(enabled)
                }
            }
        }
        
        private fun setupPreferenceListeners() {
            // Reset settings preference
            findPreference<Preference>("reset_settings")?.setOnPreferenceClickListener {
                preferencesManager.resetToDefaults()
                requireActivity().recreate()
                true
            }
            
            // About preference
            findPreference<Preference>("about")?.setOnPreferenceClickListener {
                // Show about dialog
                AboutDialogFragment().show(parentFragmentManager, "about_dialog")
                true
            }
            
            // Color preferences (would open color picker dialogs)
            findPreference<Preference>("key_background_color")?.setOnPreferenceClickListener {
                // Open color picker for key background
                true
            }
            
            findPreference<Preference>("key_text_color")?.setOnPreferenceClickListener {
                // Open color picker for key text
                true
            }
            
            findPreference<Preference>("keyboard_background_color")?.setOnPreferenceClickListener {
                // Open color picker for keyboard background
                true
            }
            
            findPreference<Preference>("preview_bar_color")?.setOnPreferenceClickListener {
                // Open color picker for preview bar
                true
            }
        }
    }
}

// Simple About Dialog
class AboutDialogFragment : androidx.fragment.app.DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        return androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("About Brahmi Keyboard")
            .setMessage("Brahmi Keyboard v1.0\n\nA keyboard for typing in ancient Brahmi script with real-time preview in modern Indian scripts.")
            .setPositiveButton("OK", null)
            .create()
    }
}
