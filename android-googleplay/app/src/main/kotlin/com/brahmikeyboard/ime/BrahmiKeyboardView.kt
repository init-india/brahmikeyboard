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
import com.brahmikeyboard.data.ScriptMappingLoader
import com.brahmikeyboard.ui.SettingsActivity
import com.brahmikeyboard.R

class BrahmiKeyboardView(
    context: Context,
    private val brahmiEngine: BrahmiEngine,
    private val scriptLoader: ScriptMappingLoader,
    private val preferences: PreferencesManager
) : LinearLayout(context) {
    
    private var inputConnection: InputConnection? = null
    private var currentMode: KeyboardMode = KeyboardMode.BRAHMI
    
    // Unified input state
    private var inputBuffer = StringBuilder()
    private var isSyllableComplete = false
    private var pendingConsonant: String? = null
    private var isConsonantPending = false
    
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
    
    // Character mappings (extended from script loader)
    private val romanConsonants = setOf(
        "k", "kh", "g", "gh", "nga", "c", "ch", "j", "jh", "yn",
        "T", "Th", "D", "Dh", "N", "t", "th", "d", "dh", "n",
        "p", "ph", "b", "bh", "m", "y", "r", "l", "v", "sh", "Sh", "s", "h", "L"
    )
    
    private val romanVowels = setOf(
        "a", "aa", "i", "ee", "u", "uu", "e", "ei", "o", "ou"
    )
    
    private val romanStandaloneVowels = setOf(
        "a", "aa", "i", "ee", "u", "uu", "e", "ei", "o", "ou"
    )
    
    private val languageDisplayNames = mapOf(
        "assamese" to "Assamese", "awadhi" to "Awadhi", "bengali" to "Bengali",
        "bhojpuri" to "Bhojpuri", "chhattisgarhi" to "Chhattisgarhi", "devanagari" to "Devanagari",
        "dogri" to "Dogri", "gujarati" to "Gujarati", "harayanvi" to "Harayanvi",
        "kannada" to "Kannada", "kashmiri" to "Kashmiri", "konkani" to "Konkani",
        "maithili" to "Maithili", "malayalam" to "Malayalam", "manipuri" to "Manipuri",
        "marathi" to "Marathi", "nepali" to "Nepali", "odia" to "Odia",
        "punjabi" to "Punjabi", "rajasthani" to "Rajasthani", "sanskrit" to "Sanskrit",
        "sindhi" to "Sindhi", "tamil" to "Tamil", "telugu" to "Telugu"
    )
    
    // Data classes for syllable processing
    private data class SyllableParseResult(
        val isValidPartial: Boolean = false,
        val isValidComplete: Boolean = false,
        val syllable: String = "",
        val partial: String = ""
    ) {
        companion object {
            fun invalid(): SyllableParseResult = SyllableParseResult()
        }
    }
    
    private data class InputState(
        val buffer: String = "",
        val isComplete: Boolean = false,
        val pendingConsonant: String? = null,
        val displayText: String = ""
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
        inputBuffer.clear()
        isSyllableComplete = false
        pendingConsonant = null
        isConsonantPending = false
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
        listOf("a", "aa", "i", "ee", "u", "uu", "e", "ei", "o", "ou").forEach { vowel ->
            val keyId = resources.getIdentifier("key_brahmi_$vowel", "id", context.packageName)
            findViewById<Button>(keyId)?.setOnClickListener { onBrahmiKeyPress(vowel) }
        }
        
        romanConsonants.forEach { consonant ->
            val keyId = resources.getIdentifier("key_brahmi_$consonant", "id", context.packageName)
            findViewById<Button>(keyId)?.setOnClickListener { onBrahmiKeyPress(consonant) }
        }
        
        // Special Brahmi keys
        findViewById<Button>(R.id.key_brahmi_halant)?.setOnClickListener { onBrahmiKeyPress("halant") }
        findViewById<Button>(R.id.key_brahmi_anusvara)?.setOnClickListener { onBrahmiKeyPress("anusvara") }
        findViewById<Button>(R.id.key_brahmi_visarga)?.setOnClickListener { onBrahmiKeyPress("visarga") }
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
        findViewById<Button>(R.id.key_question)?.setOnClickListener { onKeyPress("?") }
        findViewById<Button>(R.id.key_dot)?.setOnClickListener { onKeyPress(".") }
        findViewById<Button>(R.id.key_comma)?.setOnClickListener { onKeyPress(",") }
        findViewById<Button>(R.id.key_at)?.setOnClickListener { onKeyPress("@") }
    }
    
    private fun setupBrahmiFunctionKeys() {
        findViewById<Button>(R.id.key_brahmi_numpad)?.setOnClickListener { toggleNumpad() }
        findViewById<Button>(R.id.key_brahmi_symbols)?.setOnClickListener { toggleSymbols() }
        findViewById<Button>(R.id.key_brahmi_mode)?.setOnClickListener { switchMode() }
        findViewById<Button>(R.id.key_brahmi_space)?.setOnClickListener { onKeyPress(" ") }
        findViewById<Button>(R.id.key_brahmi_lang)?.setOnClickListener { switchReferenceLanguage() }
        findViewById<Button>(R.id.key_brahmi_settings)?.setOnClickListener { openSettings() }
        findViewById<Button>(R.id.key_brahmi_backspace)?.setOnClickListener { onKeyPress("BACKSPACE") }
        findViewById<Button>(R.id.key_brahmi_enter)?.setOnClickListener { onKeyPress("ENTER") }
        findViewById<Button>(R.id.key_brahmi_dot)?.setOnClickListener { onKeyPress(".") }
        findViewById<Button>(R.id.key_brahmi_question)?.setOnClickListener { onKeyPress("?") }
        findViewById<Button>(R.id.key_brahmi_at)?.setOnClickListener { onKeyPress("@") }
        findViewById<Button>(R.id.key_brahmi_comma)?.setOnClickListener { onKeyPress(",") }
    }
    
    private fun setupNumpadListeners() {
        (0..9).forEach { num ->
            val keyId = resources.getIdentifier("key_$num", "id", context.packageName)
            findViewById<Button>(keyId)?.setOnClickListener { onKeyPress(num.toString()) }
        }
        
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
            handlePureBrahmiKey(brahmiKey)
        } else {
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
        inputConnection?.commitText(char, 1)
        if (isShiftActive) {
            isShiftActive = false
            updateLayout()
        }
        showPasswordPreview()
    }
    
    private fun handleCharacterInNormal(char: String) {
        when (currentMode) {
            KeyboardMode.ENGLISH -> {
                inputConnection?.commitText(char, 1)
                clearInputState()
            }
            
            KeyboardMode.BRAHMI -> {
                processBrahmiCharacter(char)
            }
            
            KeyboardMode.PURE_BRAHMI -> {
                processPureBrahmiCharacter(char)
            }
        }
        
        if (isShiftActive) {
            isShiftActive = false
            updateLayout()
        }
    }
    
    private fun processBrahmiCharacter(char: String) {
        inputBuffer.append(char)
        
        val parseResult = analyzeInputBuffer()
        
        when {
            parseResult.isValidComplete -> {
                // Complete syllable ready
                val conversion = brahmiEngine.convertToBrahmi(parseResult.syllable, currentMode)
                showCompletePreview(conversion)
                isSyllableComplete = true
            }
            
            parseResult.isValidPartial -> {
                // Valid partial input
                showPartialPreview(parseResult.partial)
                isSyllableComplete = false
            }
            
            else -> {
                // Invalid input - handle as new syllable
                if (inputBuffer.length > 1) {
                    // Try to salvage by extracting valid syllable
                    val salvaged = salvageSyllable(inputBuffer.toString())
                    if (salvaged.isNotEmpty()) {
                        inputBuffer = StringBuilder(salvaged)
                        showPartialPreview(salvaged)
                    } else {
                        // Commit previous and start fresh
                        commitCurrentBuffer()
                        inputBuffer = StringBuilder(char)
                        showPartialPreview(char)
                    }
                } else {
                    showPartialPreview(char)
                }
            }
        }
    }
    
    private fun analyzeInputBuffer(): SyllableParseResult {
        val buffer = inputBuffer.toString()
        if (buffer.isEmpty()) return SyllableParseResult.invalid()
        
        // Check if it's a direct match in mappings
        if (isExactMapping(buffer)) {
            return SyllableParseResult(
                isValidPartial = true,
                isValidComplete = isCompleteSyllable(buffer),
                syllable = buffer,
                partial = buffer
            )
        }
        
        // Try to extract the longest valid syllable
        val extracted = extractValidSyllable(buffer)
        
        return SyllableParseResult(
            isValidPartial = extracted.isNotEmpty(),
            isValidComplete = isCompleteSyllable(extracted),
            syllable = extracted,
            partial = extracted
        )
    }
    
    private fun extractValidSyllable(buffer: String): String {
        // Try longest possible matches first (up to 3 chars for syllables like "nga")
        for (length in 3 downTo 1) {
            if (buffer.length >= length) {
                val candidate = buffer.substring(0, length)
                if (isValidSyllable(candidate)) {
                    return candidate
                }
            }
        }
        
        // If no valid syllable found, check if it could be a consonant followed by vowel
        if (buffer.length >= 2) {
            val consonant = getConsonantPart(buffer)
            if (consonant.isNotEmpty() && consonant in romanConsonants) {
                val remaining = buffer.substring(consonant.length)
                if (remaining.isEmpty() || remaining in romanVowels) {
                    return consonant + remaining
                }
            }
        }
        
        return ""
    }
    
    private fun isExactMapping(text: String): Boolean {
        // Check if text exists in the universal mapping
        return when {
            text in romanConsonants -> true
            text in romanVowels -> true
            text in setOf("halant", "anusvara", "visarga") -> true
            else -> false
        }
    }
    
    private fun isValidSyllable(text: String): Boolean {
        if (text.isEmpty()) return false
        
        return when {
            // Standalone vowel
            text in romanStandaloneVowels -> true
            
            // Consonant (with or without inherent 'a')
            text in romanConsonants -> true
            
            // Consonant + vowel combination
            text.length >= 2 -> {
                val consonant = getConsonantPart(text)
                val vowel = text.substring(consonant.length)
                consonant in romanConsonants && vowel in romanVowels
            }
            
            else -> false
        }
    }
    
    private fun isCompleteSyllable(text: String): Boolean {
        if (text.isEmpty()) return false
        
        return when {
            // Vowel is always complete
            text in romanStandaloneVowels -> true
            
            // Consonant alone (inherent 'a') is complete
            text in romanConsonants -> true
            
            // Consonant + vowel is complete
            text.length >= 2 -> {
                val consonant = getConsonantPart(text)
                val vowel = text.substring(consonant.length)
                consonant in romanConsonants && vowel in romanVowels
            }
            
            else -> false
        }
    }
    
    private fun getConsonantPart(input: String): String {
        // Check for 3-character consonants first
        if (input.length >= 3) {
            val firstThree = input.substring(0, 3)
            if (firstThree in listOf("nga", "yna")) { // Add more 3-char consonants as needed
                return firstThree
            }
        }
        
        // Check for 2-character consonants
        if (input.length >= 2) {
            val firstTwo = input.substring(0, 2)
            if (firstTwo in listOf("kh", "gh", "ch", "jh", "th", "dh", "ph", 
                                  "bh", "sh", "Th", "Dh", "Sh")) {
                return firstTwo
            }
        }
        
        // Single character consonant
        return if (input.isNotEmpty() && input[0].toString() in romanConsonants) {
            input[0].toString()
        } else {
            ""
        }
    }
    
    private fun salvageSyllable(buffer: String): String {
        // Try to extract a valid syllable from invalid buffer
        for (i in buffer.length downTo 1) {
            val candidate = buffer.substring(0, i)
            if (isValidSyllable(candidate)) {
                return candidate
            }
        }
        return ""
    }
    
    private fun processPureBrahmiCharacter(char: String) {
        // For Pure Brahmi mode, we need different handling
        // This could be Roman input that gets converted to Brahmi characters
        // Or direct Brahmi character input
        
        // For now, treat it like Brahmi mode but with different conversion
        inputBuffer.append(char)
        val conversion = brahmiEngine.convertToBrahmi(inputBuffer.toString(), currentMode)
        
        if (conversion.brahmiText.isNotEmpty()) {
            showPureBrahmiPreview(conversion)
            isSyllableComplete = true
        } else {
            showPartialPreview(inputBuffer.toString())
            isSyllableComplete = false
        }
    }
    
    private fun handlePureBrahmiKey(key: String) {
        when (key) {
            "halant" -> handleHalant()
            "anusvara" -> handleAnusvara()
            "visarga" -> handleVisarga()
            else -> {
                // Treat as Brahmi character key
                val brahmiChar = getBrahmiCharacter(key)
                if (brahmiChar.isNotEmpty()) {
                    inputConnection?.commitText(brahmiChar, 1)
                    updatePureBrahmiPreview()
                }
            }
        }
    }
    
    private fun getBrahmiCharacter(key: String): String {
        // Map Roman key to Brahmi character
        // This should come from script loader or a mapping table
        return when (key) {
            in romanConsonants -> scriptLoader.romanToBrahmiSyllable(key, "brahmi")
            in romanVowels -> scriptLoader.romanToBrahmiSyllable(key, "brahmi")
            else -> key
        }
    }
    
    private fun handleHalant() {
        // Add halant to current buffer or commit
        if (inputBuffer.isNotEmpty()) {
            inputBuffer.append("_") // Represent halant
            showPartialPreview(inputBuffer.toString())
        }
    }
    
    private fun handleAnusvara() {
        // Add anusvara
        inputConnection?.commitText("ṃ", 1)
    }
    
    private fun handleVisarga() {
        // Add visarga
        inputConnection?.commitText("ḥ", 1)
    }
    
    private fun handleBackspace() {
        when {
            isPasswordField || currentMode == KeyboardMode.ENGLISH -> {
                inputConnection?.deleteSurroundingText(1, 0)
            }
            
            currentMode == KeyboardMode.BRAHMI -> {
                if (inputBuffer.isNotEmpty()) {
                    inputBuffer.deleteCharAt(inputBuffer.length - 1)
                    updatePreview()
                } else {
                    inputConnection?.deleteSurroundingText(1, 0)
                }
            }
            
            currentMode == KeyboardMode.PURE_BRAHMI -> {
                inputConnection?.deleteSurroundingText(1, 0)
                updatePreview()
            }
        }
    }
    
    private fun handleEnter() {
        if (isPasswordField) {
            inputConnection?.commitText("\n", 1)
        } else {
            commitCurrentBuffer()
            inputConnection?.commitText("\n", 1)
        }
    }
    
    private fun handleSpace() {
        if (isPasswordField) {
            inputConnection?.commitText(" ", 1)
        } else {
            commitCurrentBuffer()
            inputConnection?.commitText(" ", 1)
        }
    }
    
    private fun commitCurrentBuffer() {
        if (inputBuffer.isEmpty()) return
        
        when (currentMode) {
            KeyboardMode.BRAHMI -> {
                val conversion = brahmiEngine.convertToBrahmi(inputBuffer.toString(), currentMode)
                inputConnection?.commitText(conversion.outputText, 1)
                clearInputState()
                clearPreview()
            }
            
            KeyboardMode.PURE_BRAHMI -> {
                // For Pure Brahmi, buffer should already contain Brahmi characters
                inputConnection?.commitText(inputBuffer.toString(), 1)
                clearInputState()
                clearPreview()
            }
            
            else -> {} // English mode handled elsewhere
        }
    }
    
    private fun clearInputState() {
        inputBuffer.clear()
        isSyllableComplete = false
        pendingConsonant = null
        isConsonantPending = false
    }
    
    private fun switchMode() {
        // Commit any pending input before switching
        commitCurrentBuffer()
        
        currentMode = when (currentMode) {
            KeyboardMode.ENGLISH -> KeyboardMode.BRAHMI
            KeyboardMode.BRAHMI -> KeyboardMode.PURE_BRAHMI
            KeyboardMode.PURE_BRAHMI -> KeyboardMode.ENGLISH
        }
        preferences.setCurrentMode(currentMode)
        clearInputState()
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
        
        when (currentMode) {
            KeyboardMode.BRAHMI -> {
                if (inputBuffer.isNotEmpty()) {
                    val parseResult = analyzeInputBuffer()
                    if (parseResult.isValidComplete) {
                        val conversion = brahmiEngine.convertToBrahmi(parseResult.syllable, currentMode)
                        showCompletePreview(conversion)
                    } else if (parseResult.isValidPartial) {
                        showPartialPreview(parseResult.partial)
                    } else {
                        showPartialPreview(inputBuffer.toString())
                    }
                } else {
                    showEmptyPreview()
                }
            }
            
            KeyboardMode.PURE_BRAHMI -> {
                if (inputBuffer.isNotEmpty()) {
                    val conversion = brahmiEngine.convertToBrahmi(inputBuffer.toString(), currentMode)
                    showPureBrahmiPreview(conversion)
                } else {
                    showEmptyPreview()
                }
            }
            
            else -> showEmptyPreview()
        }
    }
    
    private fun showCompletePreview(conversion: com.brahmikeyboard.engine.ConversionResult) {
        previewLine1.text = conversion.brahmiText
        previewLine2.text = conversion.indianScriptText
    }
    
    private fun showPureBrahmiPreview(conversion: com.brahmikeyboard.engine.ConversionResult) {
        // For Pure Brahmi, line 1 shows Brahmi, line 2 shows Roman/Indian
        previewLine1.text = conversion.brahmiText
        previewLine2.text = conversion.indianScriptText
    }
    
    private fun showPartialPreview(partial: String) {
        previewLine1.text = "$partial..."
        previewLine2.text = "$partial..."
    }
    
    private fun updatePreviewDisplay() {
        if (isPasswordField) {
            showPasswordPreview()
        } else {
            updatePreview()
        }
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
        previewLabel1.text = "Brahmi:"
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
    
    private fun applyDarkTheme() {
        setBackgroundColor(0xFF333333.toInt())
    }
    
    private fun applyLightTheme() {
        setBackgroundColor(0xFFF0F0F0.toInt())
    }
    
    private fun applyHighContrastTheme() {
        setBackgroundColor(0xFF000000.toInt())
    }
    
    private fun applySepiaTheme() {
        setBackgroundColor(0xFFF4ECD8.toInt())
    }
}
