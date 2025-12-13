package com.brahmikeyboard.data

import android.content.res.AssetManager
import kotlinx.serialization.json.Json
import java.io.InputStream

class ScriptMappingLoader(private val assets: AssetManager) {
    
    // ==============================================
    // CORE MAPPINGS - AS PER YOUR SPECIFICATION
    // ==============================================
    
    // Universal Roman to Indian script mapping (applies to ALL Indian scripts)
    private val universalRomanToIndianMap = mapOf(
        // VOWELS
        "a" to "अ", "aa" to "आ", "i" to "इ", "ee" to "ई",
        "u" to "उ", "uu" to "ऊ", "e" to "ए", "ei" to "ऐ",
        "o" to "ओ", "ou" to "औ",
        
        // CONSONANTS
        "k" to "क", "kh" to "ख", "g" to "ग", "gh" to "घ",
        "nga" to "ङ", "c" to "च", "ch" to "छ", "j" to "ज",
        "jh" to "झ", "yn" to "ञ", "T" to "ट", "Th" to "ठ",
        "D" to "ड", "Dh" to "ढ", "N" to "ण", "t" to "त",
        "th" to "थ", "d" to "द", "dh" to "ध", "n" to "न",
        "p" to "प", "ph" to "फ", "b" to "ब", "bh" to "भ",
        "m" to "म", "y" to "य", "r" to "र", "l" to "ल",
        "v" to "व", "sh" to "श", "Sh" to "ष", "s" to "स",
        "h" to "ह", "L" to "ऴ"
    )
    
    // Roman to Brahmi base character mapping
    private val romanToBrahmiMap = mapOf(
        // VOWELS
        "a" to "𑀅", "aa" to "𑀆", "i" to "𑀇", "ee" to "𑀈",
        "u" to "𑀉", "uu" to "𑀊", "e" to "𑀏", "ei" to "𑀐",
        "o" to "𑀑", "ou" to "𑀒",
        
        // CONSONANTS
        "k" to "𑀓", "kh" to "𑀔", "g" to "𑀕", "gh" to "𑀖",
        "nga" to "𑀗", "c" to "𑀘", "ch" to "𑀙", "j" to "𑀚",
        "jh" to "𑀛", "yn" to "𑀜", "T" to "𑀝", "Th" to "𑀞",
        "D" to "𑀟", "Dh" to "𑀠", "N" to "𑀡", "t" to "𑀢",
        "th" to "𑀣", "d" to "𑀤", "dh" to "𑀥", "n" to "𑀦",
        "p" to "𑀧", "ph" to "𑀨", "b" to "𑀩", "bh" to "𑀪",
        "m" to "𑀫", "y" to "𑀬", "r" to "𑀭", "l" to "𑀮",
        "v" to "𑀯", "sh" to "𑀰", "Sh" to "𑀱", "s" to "𑀲",
        "h" to "𑀳", "L" to "𑀴"
    )
    
    // Brahmi vowel diacritics (to attach to consonants)
    private val brahmiVowelDiacritics = mapOf(
        "" to "", "a" to "",           // inherent 'a' - no diacritic
        "aa" to "𑀸", "i" to "𑀺", "ee" to "𑀻",
        "u" to "𑀼", "uu" to "𑀽", "e" to "𑁂",
        "ei" to "𑁃", "o" to "𑁄", "ou" to "𑁅"
    )
    
    // ==============================================
    // SIMPLE SYLLABLE MAPPING METHODS
    // ==============================================
    
    fun romanToBrahmiSyllable(romanSyllable: String, targetScript: String): String {
        val input = romanSyllable.lowercase()
        
        // SIMPLE LOGIC: If input exists in map, return it
        // Otherwise, try to build it from parts
        
        // 1. Check if it's a complete vowel (a, i, u, etc.)
        romanToBrahmiMap[input]?.let { return it }
        
        // 2. Try to process as consonant + vowel
        if (input.length >= 2) {
            // Simple approach: first char/2-chars is consonant, rest is vowel
            val consonant = getConsonantPart(input)
            val vowel = input.substring(consonant.length)
            
            val baseBrahmi = romanToBrahmiMap[consonant]
            val diacritic = brahmiVowelDiacritics[vowel] ?: ""
            
            if (baseBrahmi != null) {
                return baseBrahmi + diacritic
            }
        }
        
        // 3. Fallback: return input as-is
        return input
    }
    
    fun romanToIndianSyllable(romanSyllable: String, targetScript: String): String {
        val input = romanSyllable.lowercase()
        
        // SIMPLE: Just use universal mapping
        // If exact match exists, use it
        universalRomanToIndianMap[input]?.let { return it }
        
        // Otherwise, try consonant + vowel
        if (input.length >= 2) {
            val consonant = getConsonantPart(input)
            val vowel = input.substring(consonant.length)
            
            val baseIndian = universalRomanToIndianMap[consonant]
            if (baseIndian != null && vowel.isNotEmpty()) {
                // For Indian scripts, vowel handling is complex
                // This is a simplified version
                val vowelChar = universalRomanToIndianMap[vowel] ?: vowel
                return baseIndian + vowelChar
            }
        }
        
        // Fallback
        return input
    }
    
    private fun getConsonantPart(input: String): String {
        // Check for 2-character consonants first
        if (input.length >= 2) {
            val firstTwo = input.substring(0, 2)
            if (firstTwo in listOf("kh", "gh", "ch", "jh", "th", "dh", "ph", "bh", "sh", "Th", "Dh", "Sh")) {
                return firstTwo
            }
        }
        
        // Single character consonant
        return if (input.isNotEmpty()) input[0].toString() else ""
    }
    
    // ==============================================
    // BRAHMI TO ROMAN (for Pure Brahmi mode preview)
    // ==============================================
    
    fun brahmiToRoman(brahmiText: String): String {
        // Simple reverse lookup
        val result = StringBuilder()
        
        // First, try to find exact matches in reverse map
        val reverseMap = romanToBrahmiMap.entries.associate { (k, v) -> v to k }
        
        var i = 0
        while (i < brahmiText.length) {
            var found = false
            
            // Check for Brahmi vowel diacritics
            for ((vowel, diacritic) in brahmiVowelDiacritics) {
                if (diacritic.isNotEmpty() && i + diacritic.length <= brahmiText.length) {
                    if (brahmiText.substring(i, i + diacritic.length) == diacritic) {
                        result.append(vowel)
                        i += diacritic.length
                        found = true
                        break
                    }
                }
            }
            
            if (!found) {
                val char = brahmiText[i].toString()
                result.append(reverseMap[char] ?: char)
                i++
            }
        }
        
        return result.toString()
    }
    
    // ==============================================
    // CHARACTER-LEVEL MAPPING METHODS (for fallback)
    // ==============================================
    
    fun romanToIndianScript(romanText: String, targetScript: String): String {
        // Simple character-by-character mapping
        val result = StringBuilder()
        var i = 0
        
        while (i < romanText.length) {
            // Try 2-character combinations first
            if (i + 1 < romanText.length) {
                val twoChars = romanText.substring(i, i + 2).lowercase()
                universalRomanToIndianMap[twoChars]?.let {
                    result.append(it)
                    i += 2
                    continue
                }
            }
            
            // Single character
            val char = romanText[i].toString().lowercase()
            result.append(universalRomanToIndianMap[char] ?: char)
            i++
        }
        
        return result.toString()
    }
    
    fun romanToBrahmiScript(romanText: String, targetScript: String): String {
        // Convert via Indian script first (simple approach)
        val indianScript = romanToIndianScript(romanText, targetScript)
        
        // For now, return the Roman text as Brahmi
        // In reality, you'd need Indian-to-Brahmi mapping
        return romanText // Simplified - needs proper mapping
    }
    
    // ==============================================
    // FILE LOADING METHODS (keep existing structure)
    // ==============================================
    
    private val scriptMappings = mutableMapOf<String, Map<String, String>>()
    private val wordLevelMappings = mutableMapOf<String, Map<String, String>>()
    
    private fun loadScriptMapping(script: String): Map<String, String> {
        return scriptMappings.getOrPut(script) {
            try {
                val inputStream: InputStream = assets.open("script-mappings/${script}.json")
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val mappingData = Json.decodeFromString<ScriptMappingData>(jsonString)
                createFullMapping(mappingData)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
    
    private fun loadWordLevelMappings(): Map<String, Map<String, String>> {
        return try {
            val inputStream: InputStream = assets.open("script-mappings/word-level-mappings.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            Json.decodeFromString<Map<String, Map<String, String>>>(jsonString)
        } catch (e: Exception) {
            mutableMapOf<String, Map<String, String>>().apply {
                put("roman_to_brahmi_words", createCommonWordMappings())
                put("brahmi_to_roman_words", createReverseWordMappings())
            }
        }
    }
    
    private fun createFullMapping(data: ScriptMappingData): Map<String, String> {
        val fullMap = mutableMapOf<String, String>()
        
        data.brahmi_mappings.vowels?.forEach { (key, value) -> fullMap[key] = value }
        data.brahmi_mappings.consonants?.forEach { (key, value) -> fullMap[key] = value }
        data.brahmi_mappings.vowel_marks?.forEach { (key, value) -> fullMap[key] = value }
        data.brahmi_mappings.special_marks?.forEach { (key, value) -> fullMap[key] = value }
        data.brahmi_mappings.numerals?.forEach { (key, value) -> fullMap[key] = value }
        
        return fullMap
    }
    
    fun scriptToBrahmi(scriptText: String, sourceScript: String): String {
        val mapping = loadScriptMapping(sourceScript)
        var result = StringBuilder()
        
        for (char in scriptText) {
            val brahmiChar = mapping[char.toString()] ?: char.toString()
            result.append(brahmiChar)
        }
        
        return result.toString()
    }
    
    fun brahmiToScript(brahmiText: String, targetScript: String): String {
        val mapping = loadScriptMapping(targetScript)
        val reverseMapping = mapping.entries.associate { (k, v) -> v to k }
        var result = StringBuilder()
        
        for (char in brahmiText) {
            val scriptChar = reverseMapping[char.toString()] ?: char.toString()
            result.append(scriptChar)
        }
        
        return result.toString()
    }
    
    fun romanToBrahmiWordLevel(romanWord: String, targetScript: String): String {
        val wordMappings = loadWordLevelMappings()
        val brahmiWordMapping = wordMappings["roman_to_brahmi_words"]
        
        return brahmiWordMapping?.get(romanWord.lowercase()) 
            ?: romanToBrahmiScript(romanWord, targetScript)
    }
    
    fun romanToIndianWordLevel(romanWord: String, targetScript: String): String {
        val wordMappings = loadWordLevelMappings()
        val indianWordMapping = wordMappings["roman_to_${targetScript}_words"]
        
        return indianWordMapping?.get(romanWord.lowercase())
            ?: romanToIndianScript(romanWord, targetScript)
    }
    
    fun brahmiToRomanWordLevel(brahmiWord: String, sourceScript: String): String {
        val wordMappings = loadWordLevelMappings()
        val romanWordMapping = wordMappings["brahmi_to_roman_words"]
        
        return romanWordMapping?.get(brahmiWord) ?: brahmiToRoman(brahmiWord)
    }
    
    private fun createCommonWordMappings(): Map<String, String> {
        return mapOf(
            "namaste" to "𑀦𑀫𑀲𑁆𑀢𑁂",
            "hello" to "𑀳𑁂𑀮𑁄",
            "thank" to "𑀣𑀦𑁆𑀓",
            "you" to "𑀬𑁄𑀉",
            "yes" to "𑀬𑁂𑀲",
            "no" to "𑀦𑁄"
        )
    }
    
    private fun createReverseWordMappings(): Map<String, String> {
        val commonMappings = createCommonWordMappings()
        return commonMappings.entries.associate { (k, v) -> v to k }
    }
}

@kotlinx.serialization.Serializable
data class ScriptMappingData(
    val script: String,
    val brahmi_mappings: BrahmiMappings
)

@kotlinx.serialization.Serializable
data class BrahmiMappings(
    val vowels: Map<String, String>? = null,
    val consonants: Map<String, String>? = null,
    val vowel_marks: Map<String, String>? = null,
    val special_marks: Map<String, String>? = null,
    val numerals: Map<String, String>? = null
)
