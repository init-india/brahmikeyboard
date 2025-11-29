package com.brahmikeyboard.data

import android.content.res.AssetManager
import kotlinx.serialization.json.Json
import java.io.InputStream

class ScriptMappingLoader(private val assets: AssetManager) {
    
    private val scriptMappings = mutableMapOf<String, Map<String, String>>()
    private val romanToIndianMappings = mutableMapOf<String, Map<String, String>>()
    private val wordLevelMappings = mutableMapOf<String, Map<String, String>>()
    
    // Brahmi combinations sorted by length (longest first)
    private val brahmiCombinations = listOf(
        "aa", "ee", "uu", "ei", "ou",
        "kh", "gh", "nga", "ch", "jh", "yn",
        "Th", "Dh", "N", "th", "dh", "ph", "bh", "L"
    ).sortedByDescending { it.length }

    // Complete Brahmi to Roman mapping
    private val brahmiToRomanMap = mapOf(
        // Vowels
        "𑀅" to "a", "𑀆" to "aa", "𑀇" to "i", "𑀈" to "ee",
        "𑀉" to "u", "𑀊" to "uu", "𑀏" to "e", "𑀐" to "ei",
        "𑀑" to "o", "𑀒" to "ou", "𑀃" to "", "𑀄" to "",
        
        // Consonants
        "𑀓" to "k", "𑀔" to "kh", "𑀕" to "g", "𑀖" to "gh",
        "𑀗" to "nga", "𑀘" to "c", "𑀙" to "ch", "𑀚" to "j",
        "𑀛" to "jh", "𑀜" to "yn", "𑀝" to "T", "𑀞" to "Th",
        "𑀟" to "D", "𑀠" to "Dh", "𑀡" to "N", "𑀢" to "t",
        "𑀣" to "th", "𑀤" to "d", "𑀥" to "dh", "𑀦" to "n",
        "𑀧" to "p", "𑀨" to "ph", "𑀩" to "b", "𑀪" to "bh",
        "𑀫" to "m", "𑀬" to "y", "𑀭" to "r", "𑀮" to "l",
        "𑀯" to "v", "𑀰" to "sh", "𑀱" to "Sh", "𑀲" to "s",
        "𑀳" to "h", "𑀴" to "L",
        
        // Vowel signs
        "𑀺" to "i", "𑀻" to "ee", "𑀼" to "u", "𑀽" to "uu",
        "𑀾" to "", "𑀿" to "", "𑁀" to "e", "𑁁" to "ei",
        "𑁂" to "e", "𑁃" to "ei", "𑁄" to "o", "𑁅" to "ou",
        "𑁆" to "", "𑁇" to "", "𑁈" to "", "𑁉" to "",
        "𑁊" to "", "𑁋" to "", "𑁌" to "", "𑁍" to "",
        
        // Special marks
        "𑁆" to "", // halant/virama
        "𑀀" to "", // anusvara
        "𑀁" to "", // anusvara
        "𑀂" to "", // visarga
        "𑀃" to "", // visarga
        
        // Numerals
        "𑁧" to "1", "𑁨" to "2", "𑁩" to "3", "𑁪" to "4",
        "𑁫" to "5", "𑁬" to "6", "𑁭" to "7", "𑁮" to "8",
        "𑁯" to "9", "𑁦" to "0",
        
        // Punctuation
        "𑁰" to ".", "𑁱" to ",", "𑁲" to "|", "𑁳" to "|",
        "𑁴" to "(", "𑁵" to ")"
    )

    private fun loadRomanToIndianMappings(): Map<String, Map<String, String>> {
        return try {
            val inputStream: InputStream = assets.open("script-mappings/roman-to-indian-scripts.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            Json.decodeFromString<Map<String, Map<String, String>>>(jsonString)
        } catch (e: Exception) {
            // Return empty map if file doesn't exist
            emptyMap()
        }
    }
    
    private fun loadScriptMapping(script: String): Map<String, String> {
        return scriptMappings.getOrPut(script) {
            try {
                val inputStream: InputStream = assets.open("script-mappings/${script}.json")
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val mappingData = Json.decodeFromString<ScriptMappingData>(jsonString)
                createFullMapping(mappingData)
            } catch (e: Exception) {
                // Return empty map if file doesn't exist
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
            // Return empty map if file doesn't exist
            emptyMap()
        }
    }
    
    private fun createFullMapping(data: ScriptMappingData): Map<String, String> {
        val fullMap = mutableMapOf<String, String>()
        
        // Add all mappings from the JSON data
        data.brahmi_mappings.vowels?.forEach { (key, value) -> fullMap[key] = value }
        data.brahmi_mappings.consonants?.forEach { (key, value) -> fullMap[key] = value }
        data.brahmi_mappings.vowel_marks?.forEach { (key, value) -> fullMap[key] = value }
        data.brahmi_mappings.special_marks?.forEach { (key, value) -> fullMap[key] = value }
        data.brahmi_mappings.numerals?.forEach { (key, value) -> fullMap[key] = value }
        
        return fullMap
    }
    
    // CHARACTER-LEVEL METHODS (for backward compatibility and fallback)
    
    fun romanToIndianScript(romanText: String, targetScript: String): String {
        val mappings = loadRomanToIndianMappings()
        var result = StringBuilder()
        var i = 0
        
        while (i < romanText.length) {
            var matched = false
            
            // Check for combinations first (longest match)
            for (combination in brahmiCombinations) {
                if (i + combination.length <= romanText.length) {
                    val test = romanText.substring(i, i + combination.length).lowercase()
                    if (test == combination) {
                        val scriptMapping = mappings[targetScript]
                        val mapping = scriptMapping?.get(test) ?: test
                        result.append(mapping)
                        i += combination.length
                        matched = true
                        break
                    }
                }
            }
            
            // Single character mapping
            if (!matched) {
                val char = romanText[i].toString()
                val scriptMapping = mappings[targetScript]
                val mapping = scriptMapping?.get(char.lowercase()) ?: char
                result.append(mapping)
                i += 1
            }
        }
        
        return result.toString()
    }
    
    fun romanToBrahmiScript(romanText: String, targetScript: String): String {
        // First convert to Indian script, then to Brahmi
        val indianScript = romanToIndianScript(romanText, targetScript)
        return scriptToBrahmi(indianScript, targetScript)
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
    
    // WORD-LEVEL METHODS (NEW - for parallel processing)
    
    fun romanToBrahmiWordLevel(romanWord: String, targetScript: String): String {
        // Use word-level mappings if available, fallback to character-level
        val wordMappings = loadWordLevelMappings()
        val brahmiWordMapping = wordMappings["roman_to_brahmi_words"]
        
        // Try word-level mapping first, then fallback to character-level
        return brahmiWordMapping?.get(romanWord.lowercase()) 
            ?: romanToBrahmiScript(romanWord, targetScript)
    }
    
    fun romanToIndianWordLevel(romanWord: String, targetScript: String): String {
        // Use word-level mappings if available, fallback to character-level
        val wordMappings = loadWordLevelMappings()
        val indianWordMapping = wordMappings["roman_to_${targetScript}_words"]
        
        // Try word-level mapping first, then fallback to character-level
        return indianWordMapping?.get(romanWord.lowercase())
            ?: romanToIndianScript(romanWord, targetScript)
    }
    
    fun brahmiToRomanWordLevel(brahmiWord: String, sourceScript: String): String {
        // Use reverse mapping for Brahmi to Roman
        val wordMappings = loadWordLevelMappings()
        val romanWordMapping = wordMappings["brahmi_to_roman_words"]
        
        // Try word-level mapping first, then fallback to character mapping
        return romanWordMapping?.get(brahmiWord) ?: brahmiToRoman(brahmiWord)
    }
    
    // Complete Brahmi to Roman character mapping
    private fun brahmiToRoman(brahmiText: String): String {
        var result = StringBuilder()
        for (char in brahmiText) {
            val romanChar = brahmiToRomanMap[char.toString()] ?: char.toString()
            result.append(romanChar)
        }
        return result.toString()
    }
}

// Data classes for JSON serialization
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
