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
import com.brahmikeyboard.ui.SettingsActivity
import com.brahmikeyboard.R 

class BrahmiKeyboardView(
    context: Context,
    private val brahmiEngine: BrahmiEngine,
    private val preferences: PreferencesManager
) : LinearLayout(context) {
    
    private var inputConnection: InputConnection? = null
    private var currentMode: KeyboardMode = KeyboardMode.BRAHMI
    private var currentBuffer: String = ""
    private lateinit var previewBar: TextView
    private var isPasswordField: Boolean = false
    
    private val allIndianLanguages = listOf(
        "assamese", "awadhi", "bengali", "bhojpuri", "chhattisgarhi",
        "devanagari", "dogri", "gujarati", "harayanvi", "kannada",
        "kashmiri", "konkani", "maithili", "malayalam", "manipuri",
        "marathi", "nepali", "odia", "punjabi", "rajasthani",
        "sanskrit", "sindhi", "tamil", "telugu"
    )
    
    // Keyboard states
    private var isNumpadActive = false
    private var isSymbolsActive = false
    private var isShiftActive = false
    
    // Brahmi letters mapping for Pure Brahmi mode
    private val brahmiKeyMapping = mapOf(
        // Vowels
        "a" to "𑀅", "aa" to "𑀆", "i" to "𑀇", "ii" to "𑀈", 
        "u" to "𑀉", "uu" to "𑀊", "e" to "𑀏", "ee" to "𑀐",
        "o" to "𑀑", "ou" to "𑀒",
        
        // Consonants
        "k" to "𑀓", "kh" to "𑀔", "g" to "𑀕", "gh" to "𑀖", 
        "c" to "𑀘", "ch" to "𑀙", "j" to "𑀚", "jh" to "𑀛",
        "T" to "𑀝", "Th" to "𑀞", "D" to "𑀟", "Dh" to "𑀠",
        "t" to "𑀢", "th" to "𑀣", "d" to "𑀤", "dh" to "𑀥",
        "p" to "𑀧", "ph" to "𑀨", "b" to "𑀩", "bh" to "𑀪",
        
        // Other consonants
        "n" to "𑀦", "m" to "𑀫", "y" to "𑀬", "r" to "𑀭", 
        "l" to "𑀮", "v" to "𑀯", "s" to "𑀰", "h" to "𑀳",
        "L" to "𑀴",
        
        // Special consonants (multi-character)
        "nga" to "𑀗", "yn" to "𑀜", "N" to "𑀡"
    )
    
    // Shift mapping for special characters
    private val shiftMapping = mapOf(
        "t" to "T", "th" to "Th", "d" to "D", "dh" to "Dh", "n" to "N", "l" to "L"
    )
    
    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.keyboard_view, this, true)
        setupKeyboard()
    }
    
    fun setInputConnection(ic: InputConnection?) {
        inputConnection = ic
    }
    
    fun setPasswordField(isPassword: Boolean) {
        isPasswordField = isPassword
        updatePreviewBar()
    }
    
    fun clearPreview() {
        currentBuffer = ""
        updatePreviewBar()
    }
    
    private fun setupKeyboard() {
        previewBar = findViewById(R.id.preview_bar)
        
        // Load saved states
        currentMode = preferences.getCurrentMode()
        isNumpadActive = preferences.isNumpadActive()
        isSymbolsActive = preferences.isSymbolsActive()
        
        brahmiEngine.setReferenceScript(preferences.getReferenceScript())
        setupKeyListeners()
        updateAllIndicators()
        updateLayout()
        updateKeyboardLabels()
    }
    
    private fun setupKeyListeners() {
        setupAlphabetKeys()
        setupFunctionKeys()
        setupNumpadListeners()
        setupSymbolsListeners()
    }
    
    private fun setupAlphabetKeys() {
        listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", 
               "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z").forEach { char ->
            val keyId = resources.getIdentifier("key_$char", "id", context.packageName)
            findViewById<Button>(keyId)?.setOnClickListener { onKeyPress(char) }
        }
    }
    
    private fun setupFunctionKeys() {
        findViewById<Button>(R.id.key_backspace)?.setOnClickListener { onKeyPress("BACKSPACE") }
        findViewById<Button>(R.id.key_space)?.setOnClickListener { onKeyPress(" ") }
        findViewById<Button>(R.id.key_enter)?.setOnClickListener { onKeyPress("ENTER") }
        findViewById<Button>(R.id.key_mode)?.setOnClickListener { onKeyPress("MODE_SWITCH") }
        findViewById<Button>(R.id.key_settings)?.setOnClickListener { openSettings() }
        findViewById<Button>(R.id.key_lang)?.setOnClickListener { switchReferenceLanguage() }
        findViewById<Button>(R.id.key_numpad)?.setOnClickListener { toggleNumpad() }
        findViewById<Button>(R.id.key_symbols)?.setOnClickListener { toggleSymbols() }
        findViewById<Button>(R.id.key_shift)?.setOnClickListener { toggleShift() }
    }
    
    private fun setupNumpadListeners() {
        // Numpad number keys
        (1..9).forEach { num ->
            val keyId = resources.getIdentifier("key_$num", "id", context.packageName)
            findViewById<Button>(keyId)?.setOnClickListener { onKeyPress(num.toString()) }
        }
        
        // Numpad special keys
        findViewById<Button>(R.id.key_0)?.setOnClickListener { onKeyPress("0") }
        findViewById<Button>(R.id.key_plus)?.setOnClickListener { onKeyPress("+") }
        findViewById<Button>(R.id.key_minus)?.setOnClickListener { onKeyPress("-") }
        findViewById<Button>(R.id.key_dot)?.setOnClickListener { onKeyPress(".") }
        findViewById<Button>(R.id.key_comma)?.setOnClickListener { onKeyPress(",") }
        findViewById<Button>(R.id.key_enter_num)?.setOnClickListener { onKeyPress("ENTER") }
        findViewById<Button>(R.id.key_backspace_num)?.setOnClickListener { onKeyPress("BACKSPACE") }
        findViewById<Button>(R.id.key_abc_num)?.setOnClickListener { toggleNumpad() }
        findViewById<Button>(R.id.key_equals)?.setOnClickListener { onKeyPress("=") }
        findViewById<Button>(R.id.key_percent_num)?.setOnClickListener { onKeyPress("%") }
    }
    
    private fun setupSymbolsListeners() {
        findViewById<Button>(R.id.key_excl)?.setOnClickListener { onKeyPress("!") }
        findViewById<Button>(R.id.key_at)?.setOnClickListener { onKeyPress("@") }
        findViewById<Button>(R.id.key_hash)?.setOnClickListener { onKeyPress("#") }
        findViewById<Button>(R.id.key_dollar)?.setOnClickListener { onKeyPress("$") }
        findViewById<Button>(R.id.key_percent)?.setOnClickListener { onKeyPress("%") }
        findViewById<Button>(R.id.key_caret)?.setOnClickListener { onKeyPress("^") }
        findViewById<Button>(R.id.key_amp)?.setOnClickListener { onKeyPress("&") }
        findViewById<Button>(R.id.key_star)?.setOnClickListener { onKeyPress("*") }
        findViewById<Button>(R.id.key_lparen)?.setOnClickListener { onKeyPress("(") }
        findViewById<Button>(R.id.key_rparen)?.setOnClickListener { onKeyPress(")") }
        findViewById<Button>(R.id.key_underscore)?.setOnClickListener { onKeyPress("_") }
        findViewById<Button>(R.id.key_plus_sym)?.setOnClickListener { onKeyPress("+") }
        findViewById<Button>(R.id.key_equals_sym)?.setOnClickListener { onKeyPress("=") }
        findViewById<Button>(R.id.key_lbrace)?.setOnClickListener { onKeyPress("{") }
        findViewById<Button>(R.id.key_rbrace)?.setOnClickListener { onKeyPress("}") }
        findViewById<Button>(R.id.key_slash)?.setOnClickListener { onKeyPress("/") }
        findViewById<Button>(R.id.key_backslash)?.setOnClickListener { onKeyPress("\\") }
        findViewById<Button>(R.id.key_pipe)?.setOnClickListener { onKeyPress("|") }
        findViewById<Button>(R.id.key_lt)?.setOnClickListener { onKeyPress("<") }
        findViewById<Button>(R.id.key_gt)?.setOnClickListener { onKeyPress(">") }
        findViewById<Button>(R.id.key_enter_sym)?.setOnClickListener { onKeyPress("ENTER") }
        findViewById<Button>(R.id.key_abc_sym)?.setOnClickListener { toggleSymbols() }
        findViewById<Button>(R.id.key_space_sym)?.setOnClickListener { onKeyPress(" ") }
        findViewById<Button>(R.id.key_dot_sym)?.setOnClickListener { onKeyPress(".") }
        findViewById<Button>(R.id.key_comma_sym)?.setOnClickListener { onKeyPress(",") }
    }
    
    private fun onKeyPress(key: String) {
        when {
            key == "BACKSPACE" -> handleBackspace()
            key == "MODE_SWITCH" -> switchMode()
            key == "ENTER" -> handleEnter()
            key == " " -> handleSpace()
            key.length == 1 -> handleCharacter(key)
        }
    }
    
    private fun handleCharacter(char: String) {
        if (isPasswordField) {
            // Direct input for password fields
            handleCharacterInPassword(char)
        } else {
            // Normal buffer system for other fields
            handleCharacterInNormal(char)
        }
    }
    
    private fun handleCharacterInPassword(char: String) {
        val actualChar = if (isShiftActive) shiftMapping[char] ?: char.uppercase() else char
        
        when (currentMode) {
            KeyboardMode.ENGLISH -> {
                inputConnection?.commitText(actualChar, 1)
            }
            KeyboardMode.BRAHMI -> {
                val conversion = brahmiEngine.convertToBrahmi(actualChar, currentMode)
                inputConnection?.commitText(conversion.outputText, 1)
            }
            KeyboardMode.PURE_BRAHMI -> {
                val brahmiChar = brahmiKeyMapping[actualChar] ?: actualChar
                inputConnection?.commitText(brahmiChar, 1)
            }
        }
        
        // Auto-disable shift after one character in password fields
        if (isShiftActive) {
            isShiftActive = false
            updateKeyboardLabels()
        }
    }
    
    private fun handleCharacterInNormal(char: String) {
        val actualChar = if (isShiftActive) shiftMapping[char] ?: char.uppercase() else char
        
        currentBuffer += actualChar
        updatePreview()
        
        // Auto-disable shift after one character
        if (isShiftActive) {
            isShiftActive = false
            updateKeyboardLabels()
        }
    }
    
    private fun handleBackspace() {
        if (isPasswordField) {
            // Direct deletion for password fields
            inputConnection?.deleteSurroundingText(1, 0)
        } else {
            // Buffer management for normal fields
            if (currentBuffer.isNotEmpty()) {
                currentBuffer = currentBuffer.dropLast(1)
                updatePreview()
            } else {
                inputConnection?.deleteSurroundingText(1, 0)
            }
        }
    }
    
    private fun handleEnter() {
        if (isPasswordField) {
            // For password fields, just commit newline
            inputConnection?.commitText("\n", 1)
        } else {
            // For normal fields, commit buffer + newline
            if (currentBuffer.isNotEmpty()) {
                commitCurrentBuffer()
            }
            inputConnection?.commitText("\n", 1)
        }
    }
    
    private fun handleSpace() {
        if (isPasswordField) {
            // Direct space for password fields
            inputConnection?.commitText(" ", 1)
        } else {
            // For normal fields, commit buffer + space
            if (currentBuffer.isNotEmpty()) {
                commitCurrentBuffer()
            }
            inputConnection?.commitText(" ", 1)
        }
    }
    
    private fun commitCurrentBuffer() {
        if (currentBuffer.isNotEmpty()) {
            val conversion = brahmiEngine.convertToBrahmi(currentBuffer, currentMode)
            inputConnection?.commitText(conversion.outputText, 1)
            clearPreview()
        }
    }
    
    private fun switchMode() {
        currentMode = when (currentMode) {
            KeyboardMode.ENGLISH -> KeyboardMode.BRAHMI
            KeyboardMode.BRAHMI -> KeyboardMode.PURE_BRAHMI
            KeyboardMode.PURE_BRAHMI -> KeyboardMode.ENGLISH
        }
        preferences.setCurrentMode(currentMode)
        clearPreview()
        updateAllIndicators()
        updateKeyboardLabels()
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
    
    private fun toggleNumpad() {
        isNumpadActive = !isNumpadActive
        isSymbolsActive = false
        isShiftActive = false
        preferences.setNumpadActive(isNumpadActive)
        preferences.setSymbolsActive(false)
        updateLayout()
        updateKeyboardLabels()
    }
    
    private fun toggleSymbols() {
        isSymbolsActive = !isSymbolsActive
        isNumpadActive = false
        isShiftActive = false
        preferences.setSymbolsActive(isSymbolsActive)
        preferences.setNumpadActive(false)
        updateLayout()
        updateKeyboardLabels()
    }
    
    private fun toggleShift() {
        isShiftActive = !isShiftActive
        isNumpadActive = false
        isSymbolsActive = false
        preferences.setNumpadActive(false)
        preferences.setSymbolsActive(false)
        updateLayout()
        updateKeyboardLabels()
        updateShiftIndicator()
    }
    
    private fun updateLayout() {
        val alphabetLayout = findViewById<View>(R.id.layout_alphabet)
        val numpadLayout = findViewById<View>(R.id.layout_numpad)
        val symbolsLayout = findViewById<View>(R.id.layout_symbols)
        
        alphabetLayout?.visibility = if (!isNumpadActive && !isSymbolsActive && !isShiftActive) View.VISIBLE else View.GONE
        numpadLayout?.visibility = if (isNumpadActive) View.VISIBLE else View.GONE
        symbolsLayout?.visibility = if (isSymbolsActive) View.VISIBLE else View.GONE
        
        updateNumpadIndicator()
        updateSymbolsIndicator()
        updateShiftIndicator()
    }
    
    private fun updateKeyboardLabels() {
        if (isPasswordField) {
            // For password fields, show generic preview
            previewBar.text = context.getString(R.string.password_indicator)
            return
        }
        
        when {
            isShiftActive -> {
                // Show shifted characters
                updateKeyLabel("t", "T")
                updateKeyLabel("th", "Th")
                updateKeyLabel("d", "D")
                updateKeyLabel("dh", "Dh")
                updateKeyLabel("n", "N")
                updateKeyLabel("l", "L")
                // Keep other keys as they are
                listOf("a", "b", "c", "e", "f", "g", "h", "i", "j", "k", "m", "o", "p", "q", "r", "s", "u", "v", "w", "x", "y", "z").forEach { char ->
                    updateKeyLabel(char, char.uppercase())
                }
            }
            currentMode == KeyboardMode.PURE_BRAHMI -> {
                // Show Brahmi characters on keys
                brahmiKeyMapping.forEach { (english, brahmi) ->
                    updateKeyLabel(english, brahmi)
                }
            }
            else -> {
                // Show English characters for other modes
                listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", 
                       "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z").forEach { char ->
                    updateKeyLabel(char, char)
                }
            }
        }
    }
    
    private fun updateKeyLabel(englishChar: String, displayChar: String) {
        val keyId = resources.getIdentifier("key_$englishChar", "id", context.packageName)
        findViewById<Button>(keyId)?.text = displayChar
    }
    
    private fun updatePreview() {
        if (isPasswordField) {
            previewBar.text = context.getString(R.string.password_indicator)
            return
        }
        
        if (currentBuffer.isNotEmpty()) {
            val conversion = brahmiEngine.convertToBrahmi(currentBuffer, currentMode)
            previewBar.text = conversion.previewText
            
            // Log warnings if any
            if (conversion.warnings.isNotEmpty()) {
                conversion.warnings.forEach { warning ->
                    android.util.Log.d("BrahmiKeyboard", "Warning: $warning")
                }
            }
        } else {
            updatePreviewBar()
        }
    }
    
    private fun updatePreviewBar() {
        if (isPasswordField) {
            previewBar.text = context.getString(R.string.password_indicator)
            return
        }
        
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
    
    private fun updateAllIndicators() {
        updateModeIndicator()
        updateLanguageIndicator()
        updateNumpadIndicator()
        updateSymbolsIndicator()
        updateShiftIndicator()
        updatePreviewBar()
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
    
    private fun updateNumpadIndicator() {
        findViewById<Button>(R.id.key_numpad)?.text = if (isNumpadActive) "ABC" else "123"
    }
    
    private fun updateSymbolsIndicator() {
        findViewById<Button>(R.id.key_symbols)?.text = if (isSymbolsActive) "ABC" else "#+="
    }
    
    private fun updateShiftIndicator() {
        val shiftButton = findViewById<Button>(R.id.key_shift)
        shiftButton?.isActivated = isShiftActive
        shiftButton?.setBackgroundColor(if (isShiftActive) 0xFFCCCCCC.toInt() else 0xFFFFFFFF.toInt())
    }
    
    private fun openSettings() {
        try {
            val intent = Intent(context, SettingsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Settings not available", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
