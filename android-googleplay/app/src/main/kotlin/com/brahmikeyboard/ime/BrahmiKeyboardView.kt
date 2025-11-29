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
    private lateinit var previewLine1: TextView
    private lateinit var previewLine2: TextView
    private lateinit var previewLabel1: TextView
    private lateinit var previewLabel2: TextView
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
    
    // Brahmi character mapping for PURE BRAHMI mode
    private val brahmiCharacterMap = mapOf(
        // Vowels
        "a" to "𑀅", "aa" to "𑀆", "i" to "𑀇", "ee" to "𑀈", 
        "u" to "𑀉", "uu" to "𑀊", "e" to "𑀏", "ei" to "𑀐",
        "o" to "𑀑", "ou" to "𑀒",
        
        // Consonants
        "k" to "𑀓", "kh" to "𑀔", "g" to "𑀕", "gh" to "𑀖", 
        "nga" to "𑀗", "c" to "𑀘", "ch" to "𑀙", "j" to "𑀚", 
        "jh" to "𑀛", "yn" to "𑀜", "T" to "𑀝", "Th" to "𑀞",
        "D" to "𑀟", "Dh" to "𑀠", "N" to "𑀡", "t" to "𑀢",
        "th" to "𑀣", "d" to "𑀤", "dh" to "𑀥", "n" to "𑀦",
        "p" to "𑀧", "ph" to "𑀨", "b" to "𑀩", "bh" to "𑀪",
        "m" to "𑀫", "y" to "𑀬", "r" to "𑀭", "l" to "𑀮",
        "v" to "𑀯", "sh" to "𑀰", "Sh" to "𑀱", "s" to "𑀲", 
        "h" to "𑀳", "L" to "𑀴",
        
        // Special keys
        "halant" to "𑁆"
    )
    
    private val languageDisplayNames = mapOf(
        "assamese" to "assamese", "awadhi" to "awadhi", "bengali" to "bengali",
        "bhojpuri" to "bhojpuri", "chhattisgarhi" to "chhattisgarhi", "devanagari" to "devanagari",
        "dogri" to "dogri", "gujarati" to "gujarati", "harayanvi" to "harayanvi",
        "kannada" to "kannada", "kashmiri" to "kashmiri", "konkani" to "konkani",
        "maithili" to "maithili", "malayalam" to "malayalam", "manipuri" to "manipuri",
        "marathi" to "marathi", "nepali" to "nepali", "odia" to "odia",
        "punjabi" to "punjabi", "rajasthani" to "rajasthani", "sanskrit" to "sanskrit",
        "sindhi" to "sindhi", "tamil" to "tamil", "telugu" to "telugu"
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
        updatePreviewDisplay()
    }
    
    fun clearPreview() {
        currentBuffer = ""
        updatePreviewDisplay()
    }
    
    fun updateFromPreferences() {
        currentMode = preferences.getCurrentMode()
        brahmiEngine.setReferenceScript(preferences.getReferenceScript())
        updateAllIndicators()
        updateLayout()
        updatePreviewLabels()
    }
    
    fun applyTheme(theme: String) {
        // Apply theme to keyboard elements
        when (theme) {
            "dark" -> applyDarkTheme()
            "light" -> applyLightTheme()
            "high_contrast" -> applyHighContrastTheme()
            "sepia" -> applySepiaTheme()
            else -> applyLightTheme()
        }
    }
    
    private fun setupKeyboard() {
        previewLine1 = findViewById(R.id.preview_line1)
        previewLine2 = findViewById(R.id.preview_line2)
        previewLabel1 = findViewById(R.id.preview_label1)
        previewLabel2 = findViewById(R.id.preview_label2)
        
        // Load saved states
        currentMode = preferences.getCurrentMode()
        isNumpadActive = preferences.isNumpadActive()
        isSymbolsActive = preferences.isSymbolsActive()
        
        brahmiEngine.setReferenceScript(preferences.getReferenceScript())
        setupKeyListeners()
        updateAllIndicators()
        updateLayout()
        updatePreviewLabels()
    }
    
    private fun setupKeyListeners() {
        setupEnglishAlphabetKeys()
        setupBrahmiKeys()
        setupEnglishFunctionKeys()
        setupBrahmiFunctionKeys()
        setupNumpadListeners()
        setupSymbolsListeners()
    }
    
    private fun setupEnglishAlphabetKeys() {
        // Alphabet keys q w e r t y u i o p a s d f g h j k l z x c v b n m
        val englishKeys = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p",
                                "a", "s", "d", "f", "g", "h", "j", "k", "l", 
                                "z", "x", "c", "v", "b", "n", "m")
        
        englishKeys.forEach { char ->
            val keyId = resources.getIdentifier("key_$char", "id", context.packageName)
            findViewById<Button>(keyId)?.setOnClickListener { 
                if (isShiftActive) {
                    onKeyPress(char.uppercase())
                } else {
                    onKeyPress(char)
                }
            }
        }
    }
    
    private fun setupBrahmiKeys() {
        // Brahmi vowel keys
        listOf("a", "aa", "i", "ee", "u", "uu", "e", "ei", "o", "ou").forEach { vowel ->
            val keyId = resources.getIdentifier("key_brahmi_$vowel", "id", context.packageName)
            findViewById<Button>(keyId)?.setOnClickListener { onBrahmiKeyPress(vowel) }
        }
        
        // Brahmi consonant keys
        listOf("k", "kh", "g", "gh", "nga", "c", "ch", "j", "jh", "yn",
               "T", "Th", "D", "Dh", "N", "t", "th", "d", "dh", "n",
               "p", "ph", "b", "bh", "m", "y", "r", "l", "v", "sh", "Sh", "s", "h", "L").forEach { consonant ->
            val keyId = resources.getIdentifier("key_brahmi_$consonant", "id", context.packageName)
            findViewById<Button>(keyId)?.setOnClickListener { onBrahmiKeyPress(consonant) }
        }
    }
    
    private fun setupEnglishFunctionKeys() {
        // Backspace
        findViewById<Button>(R.id.key_backspace)?.setOnClickListener { onKeyPress("BACKSPACE") }
        
        // Space
        findViewById<Button>(R.id.key_space)?.setOnClickListener { onKeyPress(" ") }
        
        // Enter
        findViewById<Button>(R.id.key_enter)?.setOnClickListener { onKeyPress("ENTER") }
        
        // Mode switch
        findViewById<Button>(R.id.key_mode)?.setOnClickListener { switchMode() }
        
        // Settings
        findViewById<Button>(R.id.key_settings)?.setOnClickListener { openSettings() }
        
        // Language switch
        findViewById<Button>(R.id.key_lang)?.setOnClickListener { switchReferenceLanguage() }
        
        // Numpad toggle
        findViewById<Button>(R.id.key_numpad)?.setOnClickListener { toggleNumpad() }
        
        // Symbols toggle
        findViewById<Button>(R.id.key_symbols)?.setOnClickListener { toggleSymbols() }
        
        // Shift
        findViewById<Button>(R.id.key_shift)?.setOnClickListener { toggleShift() }
        
        // Punctuation
        findViewById<Button>(R.id.key_question)?.setOnClickListener { onKeyPress("?") }
        findViewById<Button>(R.id.key_dot)?.setOnClickListener { onKeyPress(".") }
        findViewById<Button>(R.id.key_comma)?.setOnClickListener { onKeyPress(",") }
        findViewById<Button>(R.id.key_at)?.setOnClickListener { onKeyPress("@") }
    }
    
    private fun setupBrahmiFunctionKeys() {
        // Brahmi layout number pad toggle
        findViewById<Button>(R.id.key_brahmi_numpad)?.setOnClickListener { toggleNumpad() }
        
        // Brahmi layout symbols toggle  
        findViewById<Button>(R.id.key_brahmi_symbols)?.setOnClickListener { toggleSymbols() }
        
        // Brahmi layout mode switch
        findViewById<Button>(R.id.key_brahmi_mode)?.setOnClickListener { switchMode() }
        
        // Brahmi layout space
        findViewById<Button>(R.id.key_brahmi_space)?.setOnClickListener { onKeyPress(" ") }
        
        // Brahmi layout language switch
        findViewById<Button>(R.id.key_brahmi_lang)?.setOnClickListener { switchReferenceLanguage() }
        
        // Brahmi layout settings
        findViewById<Button>(R.id.key_brahmi_settings)?.setOnClickListener { openSettings() }
        
        // Brahmi layout backspace
        findViewById<Button>(R.id.key_brahmi_backspace)?.setOnClickListener { onKeyPress("BACKSPACE") }
        
        // Brahmi layout enter
        findViewById<Button>(R.id.key_brahmi_enter)?.setOnClickListener { onKeyPress("ENTER") }
        
        // Brahmi layout special characters
        findViewById<Button>(R.id.key_brahmi_dot)?.setOnClickListener { onKeyPress(".") }
        findViewById<Button>(R.id.key_brahmi_question)?.setOnClickListener { onKeyPress("?") }
        findViewById<Button>(R.id.key_brahmi_at)?.setOnClickListener { onKeyPress("@") }
        findViewById<Button>(R.id.key_brahmi_comma)?.setOnClickListener { onKeyPress(",") }
        findViewById<Button>(R.id.key_brahmi_halant)?.setOnClickListener { onBrahmiKeyPress("halant") }
    }
    
    private fun setupNumpadListeners() {
        // Numbers 0-9
        (0..9).forEach { num ->
            val keyId = resources.getIdentifier("key_$num", "id", context.packageName)
            findViewById<Button>(keyId)?.setOnClickListener { onKeyPress(num.toString()) }
        }
        
        // Numpad special keys
        findViewById<Button>(R.id.key_plus)?.setOnClickListener { onKeyPress("+") }
        findViewById<Button>(R.id.key_minus)?.setOnClickListener { onKeyPress("-") }
        findViewById<Button>(R.id.key_dot_num)?.setOnClickListener { onKeyPress(".") }
        findViewById<Button>(R.id.key_comma_num)?.setOnClickListener { onKeyPress(",") }
        findViewById<Button>(R.id.key_enter_num)?.setOnClickListener { onKeyPress("ENTER") }
        findViewById<Button>(R.id.key_backspace_num)?.setOnClickListener { onKeyPress("BACKSPACE") }
        findViewById<Button>(R.id.key_abc_num)?.setOnClickListener { toggleNumpad() }
        findViewById<Button>(R.id.key_equals)?.setOnClickListener { onKeyPress("=") }
        findViewById<Button>(R.id.key_percent_num)?.setOnClickListener { onKeyPress("%") }
    }
    
    private fun setupSymbolsListeners() {
        // Symbol keys
        val symbolMap = mapOf(
            R.id.key_excl to "!", R.id.key_at_sym to "@", R.id.key_hash to "#", 
            R.id.key_dollar to "$", R.id.key_percent_sym to "%", R.id.key_caret to "^",
            R.id.key_amp to "&", R.id.key_star to "*", R.id.key_lparen to "(",
            R.id.key_rparen to ")", R.id.key_underscore to "_", R.id.key_plus_sym to "+",
            R.id.key_equals_sym to "=", R.id.key_lbrace to "{", R.id.key_rbrace to "}",
            R.id.key_slash to "/", R.id.key_backslash to "\\", R.id.key_pipe to "|",
            R.id.key_lt to "<", R.id.key_gt to ">", R.id.key_dot_sym to ".",
            R.id.key_comma_sym to ","
        )
        
        symbolMap.forEach { (id, symbol) ->
            findViewById<Button>(id)?.setOnClickListener { onKeyPress(symbol) }
        }
        
        // Navigation
        findViewById<Button>(R.id.key_enter_sym)?.setOnClickListener { onKeyPress("ENTER") }
        findViewById<Button>(R.id.key_abc_sym)?.setOnClickListener { toggleSymbols() }
        findViewById<Button>(R.id.key_space_sym)?.setOnClickListener { onKeyPress(" ") }
    }
    
    private fun onKeyPress(key: String) {
        when {
            key == "BACKSPACE" -> handleBackspace()
            key == "ENTER" -> handleEnter()
            key == " " -> handleSpace()
            key.length == 1 -> handleCharacter(key)
        }
    }
    
    private fun onBrahmiKeyPress(brahmiKey: String) {
        if (currentMode == KeyboardMode.PURE_BRAHMI) {
            val actualChar = brahmiCharacterMap[brahmiKey] ?: brahmiKey
            handleCharacter(actualChar)
        } else {
            // In BRAHMI mode, treat as Roman input
            handleCharacter(brahmiKey)
        }
    }
    
    private fun handleCharacter(char: String) {
        if (isPasswordField) {
            handleCharacterInPassword(char)
        } else {
            handleCharacterInNormal(char)
        }
    }
    
    private fun handleCharacterInPassword(char: String) {
        // Direct output for passwords - no preview
        inputConnection?.commitText(char, 1)
        
        if (isShiftActive) {
            isShiftActive = false
            updateLayout()
        }
        
        // Show asterisks in preview for passwords
        updatePreviewDisplay()
    }
    
    private fun handleCharacterInNormal(char: String) {
        when (currentMode) {
            KeyboardMode.ENGLISH -> {
                // Direct output for English mode
                inputConnection?.commitText(char, 1)
                currentBuffer = "" // No buffering in English mode
            }
            KeyboardMode.BRAHMI, KeyboardMode.PURE_BRAHMI -> {
                // Buffer for preview in Brahmi modes
                currentBuffer += char
                updatePreview()
            }
        }
        
        if (isShiftActive) {
            isShiftActive = false
            updateLayout()
        }
    }
    
    private fun handleBackspace() {
        if (isPasswordField || currentMode == KeyboardMode.ENGLISH) {
            // Direct backspace for passwords and English mode
            inputConnection?.deleteSurroundingText(1, 0)
        } else {
            // Backspace in preview buffer for Brahmi modes
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
            inputConnection?.commitText("\n", 1)
        } else {
            if (currentBuffer.isNotEmpty() && currentMode != KeyboardMode.ENGLISH) {
                commitCurrentBuffer()
            }
            inputConnection?.commitText("\n", 1)
        }
    }
    
    private fun handleSpace() {
        if (isPasswordField) {
            inputConnection?.commitText(" ", 1)
        } else {
            if (currentBuffer.isNotEmpty() && currentMode != KeyboardMode.ENGLISH) {
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
        updateLayout()
    }
    
    private fun switchReferenceLanguage() {
        val currentLang = preferences.getReferenceScript()
        val currentIndex = allIndianLanguages.indexOf(currentLang)
        val nextIndex = if (currentIndex == -1 || currentIndex == allIndianLanguages.size - 1) {
            0
        } else {
            currentIndex + 1
        }
        val nextLang = allIndianLanguages[nextIndex]
        
        preferences.setReferenceScript(nextLang)
        brahmiEngine.setReferenceScript(nextLang)
        updatePreviewLabels()
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
    }
    
    private fun toggleSymbols() {
        isSymbolsActive = !isSymbolsActive
        isNumpadActive = false
        isShiftActive = false
        preferences.setSymbolsActive(isSymbolsActive)
        preferences.setNumpadActive(false)
        updateLayout()
    }
    
    private fun toggleShift() {
        isShiftActive = !isShiftActive
        isNumpadActive = false
        isSymbolsActive = false
        preferences.setNumpadActive(false)
        preferences.setSymbolsActive(false)
        updateLayout()
    }
    
    private fun updateLayout() {
        val englishLayout = findViewById<View>(R.id.layout_english)
        val brahmiLayout = findViewById<View>(R.id.layout_brahmi)
        val numpadLayout = findViewById<View>(R.id.layout_numpad)
        val symbolsLayout = findViewById<View>(R.id.layout_symbols)
        
        // Show appropriate main layout based on mode
        when (currentMode) {
            KeyboardMode.ENGLISH, KeyboardMode.BRAHMI -> {
                englishLayout?.visibility = if (!isNumpadActive && !isSymbolsActive) View.VISIBLE else View.GONE
                brahmiLayout?.visibility = View.GONE
            }
            KeyboardMode.PURE_BRAHMI -> {
                englishLayout?.visibility = View.GONE
                brahmiLayout?.visibility = if (!isNumpadActive && !isSymbolsActive) View.VISIBLE else View.GONE
            }
        }
        
        numpadLayout?.visibility = if (isNumpadActive) View.VISIBLE else View.GONE
        symbolsLayout?.visibility = if (isSymbolsActive) View.VISIBLE else View.GONE
        
        updateNumpadIndicator()
        updateSymbolsIndicator()
        updateShiftIndicator()
    }
    
    private fun updatePreview() {
        if (isPasswordField) {
            showPasswordPreview()
            return
        }
        
        if (currentBuffer.isNotEmpty()) {
            val conversion = brahmiEngine.convertToBrahmi(currentBuffer, currentMode)
            showConversionPreview(conversion)
        } else {
            showEmptyPreview()
        }
    }
    
    private fun updatePreviewDisplay() {
        if (isPasswordField) {
            showPasswordPreview()
        } else if (currentBuffer.isNotEmpty()) {
            val conversion = brahmiEngine.convertToBrahmi(currentBuffer, currentMode)
            showConversionPreview(conversion)
        } else {
            showEmptyPreview()
        }
    }
    
    private fun showConversionPreview(conversion: com.brahmikeyboard.engine.ConversionResult) {
        previewLine1.text = conversion.brahmiText
        previewLine2.text = conversion.indianScriptText
    }
    
    private fun showPasswordPreview() {
        previewLine1.text = "****"
        previewLine2.text = "****"
    }
    
    private fun showEmptyPreview() {
        previewLine1.text = ""
        previewLine2.text = ""
    }
    
    private fun updatePreviewLabels() {
        val currentScript = brahmiEngine.getCurrentReferenceScript()
        val displayName = languageDisplayNames[currentScript] ?: currentScript
        previewLabel2.text = "$displayName:"
        previewLabel1.text = "brahmi:"
    }
    
    private fun updateAllIndicators() {
        updateModeIndicator()
        updateLanguageIndicator()
        updateNumpadIndicator()
        updateSymbolsIndicator()
        updateShiftIndicator()
        updatePreviewLabels()
    }
    
    private fun updateModeIndicator() {
        val modeText = when (currentMode) {
            KeyboardMode.ENGLISH -> "ENG"
            KeyboardMode.BRAHMI -> "BRM"
            KeyboardMode.PURE_BRAHMI -> "PBR"
        }
        findViewById<Button>(R.id.key_mode)?.text = modeText
        findViewById<Button>(R.id.key_brahmi_mode)?.text = modeText
    }
    
    private fun updateLanguageIndicator() {
        val currentLang = preferences.getReferenceScript()
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
        findViewById<Button>(R.id.key_brahmi_lang)?.text = langCode
    }
    
    private fun updateNumpadIndicator() {
        findViewById<Button>(R.id.key_numpad)?.text = if (isNumpadActive) "ABC" else "123"
        findViewById<Button>(R.id.key_brahmi_numpad)?.text = if (isNumpadActive) "ABC" else "123"
    }
    
    private fun updateSymbolsIndicator() {
        findViewById<Button>(R.id.key_symbols)?.text = if (isSymbolsActive) "ABC" else "#+="
        findViewById<Button>(R.id.key_brahmi_symbols)?.text = if (isSymbolsActive) "ABC" else "#+="
    }
    
    private fun updateShiftIndicator() {
        val shiftButton = findViewById<Button>(R.id.key_shift)
        shiftButton?.isActivated = isShiftActive
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
    
    // Theme application methods
    private fun applyDarkTheme() {
        setBackgroundColor(0xFF333333.toInt())
        // Implement dark theme for all keyboard elements
    }
    
    private fun applyLightTheme() {
        setBackgroundColor(0xFFF0F0F0.toInt())
        // Implement light theme for all keyboard elements
    }
    
    private fun applyHighContrastTheme() {
        setBackgroundColor(0xFF000000.toInt())
        // Implement high contrast theme
    }
    
    private fun applySepiaTheme() {
        setBackgroundColor(0xFFF4ECD8.toInt())
        // Implement sepia theme
    }
}
