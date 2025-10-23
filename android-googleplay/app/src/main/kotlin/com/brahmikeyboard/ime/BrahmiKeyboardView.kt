package com.brahmikeyboard.ime

import android.content.Context
import android.content.Intent
import android.view.inputmethod.InputConnection
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import com.brahmikeyboard.engine.BrahmiEngine
import com.brahmikeyboard.engine.KeyboardMode
import com.brahmikeyboard.data.PreferencesManager
import java.util.Timer
import java.util.TimerTask
import com.brahmikeyboard.ui.SettingsActivity
import com.brahmikeyboard.ime.premium.R

class BrahmiKeyboardView(
    context: Context,
    private val brahmiEngine: BrahmiEngine,
    private val preferences: PreferencesManager
) : LinearLayout(context) {
    
    private var inputConnection: InputConnection? = null
    private var currentMode: KeyboardMode = KeyboardMode.ENGLISH
    private var currentBuffer: String = ""
    private var commitTimer: Timer? = null
    private lateinit var previewBar: TextView
    
    // Complete list of all Indian languages supported
    private val allIndianLanguages = listOf(
        "assamese", "awadhi", "bengali", "bhojpuri", "chhattisgarhi",
        "devanagari", "dogri", "gujarati", "harayanvi", "kannada",
        "kashmiri", "konkani", "maithili", "malayalam", "manipuri",
        "marathi", "nepali", "odia", "punjabi", "rajasthani",
        "sanskrit", "sindhi", "tamil", "telugu"
    )
    
    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.keyboard_view, this, true)
        setupKeyboard()
    }
    
    fun setInputConnection(ic: InputConnection?) {
        inputConnection = ic
    }
    
    fun clearPreview() {
        currentBuffer = ""
        updatePreviewBar()
    }
    
    private fun setupKeyboard() {
        previewBar = findViewById(R.id.preview_bar)
        setupKeyListeners()
        updateModeIndicator()
    }
    
    private fun setupKeyListeners() {
        // Alphabet keys
        listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", 
               "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z").forEach { char ->
            val keyId = resources.getIdentifier("key_$char", "id", context.packageName)
            findViewById<Button>(keyId)?.setOnClickListener { onKeyPress(char) }
        }
        
        // Special keys
        findViewById<Button>(R.id.key_backspace)?.setOnClickListener { onKeyPress("BACKSPACE") }
        findViewById<Button>(R.id.key_space)?.setOnClickListener { onKeyPress(" ") }
        findViewById<Button>(R.id.key_enter)?.setOnClickListener { onKeyPress("\n") }
        findViewById<Button>(R.id.key_mode)?.setOnClickListener { onKeyPress("MODE_SWITCH") }
        findViewById<Button>(R.id.key_settings)?.setOnClickListener { openSettings() }
        findViewById<Button>(R.id.key_lang)?.setOnClickListener { switchReferenceLanguage() }
        
        // Number keys (0-9)
        (0..9).forEach { num ->
            val keyId = resources.getIdentifier("key_$num", "id", context.packageName)
            findViewById<Button>(keyId)?.setOnClickListener { onKeyPress(num.toString()) }
        }
        
        // Special character keys
        findViewById<Button>(R.id.key_123)?.setOnClickListener { onKeyPress("1") } // Basic numpad
    }
    
    private fun onKeyPress(key: String) {
        when {
            key == "BACKSPACE" -> handleBackspace()
            key == "MODE_SWITCH" -> switchMode()
            key == " " -> commitCurrentBufferAndAdd(" ")
            key == "\n" -> commitCurrentBufferAndAdd("\n")
            key.length == 1 -> handleCharacter(key)
        }
    }
    
    private fun handleCharacter(char: String) {
        currentBuffer += char
        updatePreview()
        startCommitTimer()
    }
    
    private fun handleBackspace() {
        if (currentBuffer.isNotEmpty()) {
            currentBuffer = currentBuffer.dropLast(1)
            updatePreview()
        } else {
            inputConnection?.deleteSurroundingText(1, 0)
        }
    }
    
    private fun switchMode() {
        currentMode = when (currentMode) {
            KeyboardMode.ENGLISH -> KeyboardMode.BRAHMI
            KeyboardMode.BRAHMI -> KeyboardMode.PURE_BRAHMI
            KeyboardMode.PURE_BRAHMI -> KeyboardMode.ENGLISH
        }
        updateModeIndicator()
        clearPreview()
    }
    
    private fun switchReferenceLanguage() {
        val currentLang = preferences.getReferenceLanguage()
        val currentIndex = allIndianLanguages.indexOf(currentLang)
        val nextIndex = if (currentIndex == -1 || currentIndex == allIndianLanguages.size - 1) {
            0
        } else {
            currentIndex + 1
        }
        val nextLang = allIndianLanguages[nextIndex]
        
        preferences.setReferenceLanguage(nextLang)
        brahmiEngine.setReferenceScript(nextLang)
        updatePreview()
        updateLanguageIndicator()
    }
    
    private fun updatePreview() {
        val conversion = brahmiEngine.convertToBrahmi(currentBuffer, currentMode)
        previewBar.text = conversion.previewText
    }
    
    private fun updatePreviewBar() {
        val modeText = when (currentMode) {
            KeyboardMode.ENGLISH -> "EN"
            KeyboardMode.BRAHMI -> "BR" 
            KeyboardMode.PURE_BRAHMI -> "PB"
        }
        val lang = preferences.getReferenceLanguage()
        val langCode = when {
            lang.length >= 3 -> lang.take(3).uppercase()
            else -> lang.uppercase()
        }
        previewBar.text = "[$modeText|$langCode] ${context.getString(R.string.preview_hint)}"
    }
    
    private fun updateModeIndicator() {
        val modeText = when (currentMode) {
            KeyboardMode.ENGLISH -> "ENG"
            KeyboardMode.BRAHMI -> "BRM"
            KeyboardMode.PURE_BRAHMI -> "PBR"
        }
        findViewById<Button>(R.id.key_mode)?.text = modeText
    }
    
    private fun updateLanguageIndicator() {
        val currentLang = preferences.getReferenceLanguage()
        val langCode = when (currentLang) {
            "assamese" -> "ASM"
            "awadhi" -> "AWA"
            "bengali" -> "BEN"
            "bhojpuri" -> "BHO"
            "chhattisgarhi" -> "CHH"
            "devanagari" -> "DEV"
            "dogri" -> "DOG"
            "gujarati" -> "GUJ"
            "harayanvi" -> "HAR"
            "kannada" -> "KAN"
            "kashmiri" -> "KAS"
            "konkani" -> "KON"
            "maithili" -> "MAI"
            "malayalam" -> "MAL"
            "manipuri" -> "MAN"
            "marathi" -> "MAR"
            "nepali" -> "NEP"
            "odia" -> "ODI"
            "punjabi" -> "PUN"
            "rajasthani" -> "RAJ"
            "sanskrit" -> "SAN"
            "sindhi" -> "SIN"
            "tamil" -> "TAM"
            "telugu" -> "TEL"
            else -> "DEV"
        }
        findViewById<Button>(R.id.key_lang)?.text = langCode
    }
    
    private fun startCommitTimer() {
        commitTimer?.cancel()
        commitTimer = Timer()
        commitTimer?.schedule(object : TimerTask() {
            override fun run() {
                commitCurrentBuffer()
            }
        }, 1000) // 1 second delay
    }
    
    private fun commitCurrentBuffer() {
        if (currentBuffer.isNotEmpty()) {
            val conversion = brahmiEngine.convertToBrahmi(currentBuffer, currentMode)
            inputConnection?.commitText(conversion.outputText, 1)
            clearPreview()
        }
    }
    
    private fun commitCurrentBufferAndAdd(text: String) {
        commitCurrentBuffer()
        inputConnection?.commitText(text, 1)
    }
    
    private fun openSettings() {
        val intent = Intent(context, SettingsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
