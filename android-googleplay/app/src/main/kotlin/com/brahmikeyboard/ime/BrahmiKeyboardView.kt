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
        listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", 
               "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z").forEach { char ->
            val keyId = resources.getIdentifier("key_$char", "id", context.packageName)
            findViewById<Button>(keyId)?.setOnClickListener { onKeyPress(char) }
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
        findViewById<Button>(R.id.key_backspace)?.setOnClickListener { onKeyPress("BACKSPACE") }
        findViewById<Button>(R.id.key_space)?.setOnClickListener { onKeyPress(" ") }
        findViewById<Button>(R.id.key_enter)?.setOnClickListener { onKeyPress("ENTER") }
        findViewById<Button>(R.id.key_mode)?.setOnClickListener { switchMode() }
        findViewById<Button>(R.id.key_settings)?.setOnClickListener { openSettings() }
        findViewById<Button>(R.id.key_lang)?.setOnClickListener { switchReferenceLanguage() }
        findViewById<Button>(R.id.key_numpad)?.setOnClickListener { toggleNumpad() }
        findViewById<Button>(R.id.key_symbols)?.setOnClickListener { toggleSymbols() }
        findViewById<Button>(R.id.key_shift)?.setOnClickListener { toggleShift() }
        findViewById<Button>(R.id.key_question_eng)?.setOnClickListener { onKeyPress("?") }
        findViewById<Button>(R.id.key_dot_eng)?.setOnClickListener { onKeyPress(".") }
        findViewById<Button>(R.id.key_comma_eng)?.setOnClickListener { onKeyPress(",") }
        findViewById<Button>(R.id.key_at)?.setOnClickListener { onKeyPress("@") }
    }
    
    private fun setupBrahmiFunctionKeys() {
        // Brahmi layout number pad toggle
        findViewById<Button>(R.id.key_brahmi_123)?.setOnClickListener { toggleNumpad() }
        
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
        findViewById<Button>(R.id.key_brahmi_halant)?.setOnClickListener { onBrahmiKeyPress("halant") }
    }
    
    private fun setupNumpadListeners() {
        (1..9).forEach { num ->
            val keyId = resources.getIdentifier("key_$num", "id", context.packageName)
            findViewById<Button>(keyId)?.setOnClickListener { onKeyPress(num.toString()) }
        }
        
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
        findViewById<Button>(R.id.key_dot_num)?.setOnClickListener { onKeyPress(".") }
        findViewById<Button>(R.id.key_comma_num)?.setOnClickListener { onKeyPress(",") }
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
        findViewById<Button>(R.id.key_at_sym)?.setOnClickListener { onKeyPress("@") }
        findViewById<Button>(R.id.key_percent_sym)?.setOnClickListener { onKeyPress("%") }
        findViewById<Button>(R.id.key_dot_sym)?.setOnClickListener { onKeyPress(".") }
        findViewById<Button>(R.id.key_comma_sym)?.setOnClickListener { onKeyPress(",") }
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
        val actualChar = if (isShiftActive) char.uppercase() else char
        
        when (currentMode) {
            KeyboardMode.ENGLISH -> {
                inputConnection?.commitText(actualChar, 1)
            }
            KeyboardMode.BRAHMI -> {
                val conversion = brahmiEngine.convertToBrahmi(actualChar, currentMode)
                inputConnection?.commitText(conversion.outputText, 1)
            }
            KeyboardMode.PURE_BRAHMI -> {
                inputConnection?.commitText(actualChar, 1)
            }
        }
        
        if (isShiftActive) {
            isShiftActive = false
            updateLayout()
        }
    }
    
    private fun handleCharacterInNormal(char: String) {
        val actualChar = if (isShiftActive) char.uppercase() else char
        
        currentBuffer += actualChar
        updatePreview()
        
        if (isShiftActive) {
            isShiftActive = false
            updateLayout()
        }
    }
    
    private fun handleBackspace() {
        if (isPasswordField) {
            inputConnection?.deleteSurroundingText(1, 0)
        } else {
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
            if (currentBuffer.isNotEmpty()) {
                commitCurrentBuffer()
            }
            inputConnection?.commitText("\n", 1)
        }
    }
    
    private fun handleSpace() {
        if (isPasswordField) {
            inputConnection?.commitText(" ", 1)
        } else {
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
        updateLayout()
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
                englishLayout?.visibility = if (!isNumpadActive && !isSymbolsActive && !isShiftActive) View.VISIBLE else View.GONE
                brahmiLayout?.visibility = View.GONE
            }
            KeyboardMode.PURE_BRAHMI -> {
                englishLayout?.visibility = View.GONE
                brahmiLayout?.visibility = if (!isNumpadActive && !isSymbolsActive && !isShiftActive) View.VISIBLE else View.GONE
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
            previewBar.text = "Brahmi: ****\nPassword: ****"
            return
        }
        
        if (currentBuffer.isNotEmpty()) {
            val conversion = brahmiEngine.convertToBrahmi(currentBuffer, currentMode)
            previewBar.text = conversion.previewText
        } else {
            updatePreviewBar()
        }
    }
    
    private fun updatePreviewBar() {
        if (isPasswordField) {
            previewBar.text = "Brahmi: ****\nPassword: ****"
            return
        }
        
        val modeText = when (currentMode) {
            KeyboardMode.ENGLISH -> "ENG"
            KeyboardMode.BRAHMI -> "BRM" 
            KeyboardMode.PURE_BRAHMI -> "PBR"
        }
        val lang = preferences.getReferenceLanguage()
        val langCode = when {
            lang.length >= 3 -> lang.take(3).uppercase()
            else -> lang.uppercase()
        }
        previewBar.text = "[$modeText|$langCode] Type to see preview..."
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
        findViewById<Button>(R.id.key_brahmi_mode)?.text = modeText
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
        findViewById<Button>(R.id.key_brahmi_lang)?.text = langCode
    }
    
    private fun updateNumpadIndicator() {
        findViewById<Button>(R.id.key_numpad)?.text = if (isNumpadActive) "ABC" else "123"
        findViewById<Button>(R.id.key_brahmi_123)?.text = if (isNumpadActive) "ABC" else "123"
    }
    
    private fun updateSymbolsIndicator() {
        findViewById<Button>(R.id.key_symbols)?.text = if (isSymbolsActive) "ABC" else "#+="
        findViewById<Button>(R.id.key_brahmi_symbols)?.text = if (isSymbolsActive) "ABC" else "#+="
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
