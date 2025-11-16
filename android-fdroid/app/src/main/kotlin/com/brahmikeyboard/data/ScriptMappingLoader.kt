package com.brahmikeyboard.data

import android.content.res.AssetManager
import kotlinx.serialization.json.Json
import java.io.InputStream

class ScriptMappingLoader(private val assets: AssetManager) {
    
    private val scriptMappings = mutableMapOf<String, Map<String, String>>()
    private val romanToScriptMappings = mutableMapOf<String, Map<String, String>>()
    
    // Consonant and vowel definitions
    private val consonants = setOf(
        "k", "kh", "g", "gh", "ng", "c", "ch", "j", "jh", "yn",
        "T", "Th", "D", "Dh", "N", "t", "th", "d", "dh", "n",
        "p", "ph", "b", "bh", "m", "y", "r", "l", "v", "s", "h", "L"
    )
    
    private val vowels = setOf("a", "aa", "i", "ii", "u", "uu", "e", "ee", "o", "oo", "ai", "au")
    private val vowelPriority = listOf("aa", "ii", "uu", "ee", "oo", "ai", "au", "a", "i", "u", "e", "o")
    
    private fun loadRomanToScriptMappings(): Map<String, Map<String, String>> {
        return try {
            val inputStream: InputStream = assets.open("script-mappings/roman-to-indian-scripts.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            Json.decodeFromString<Map<String, Map<String, String>>>(jsonString)
        } catch (e: Exception) {
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
                emptyMap()
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
    
    // NEW: Enhanced function for joint words
    fun romanToScriptWithJointWords(romanText: String, targetScript: String): String {
        val mappings = loadRomanToScriptMappings()
        var result = StringBuilder()
        var i = 0
        
        while (i < romanText.length) {
            var matched = false
            
            // Try vowel combinations first
            for (vowel in vowelPriority) {
                if (i + vowel.length <= romanText.length) {
                    val test = romanText.substring(i, i + vowel.length).lowercase()
                    if (test == vowel) {
                        val mapping = mappings[test]?.get(targetScript) ?: test
                        result.append(mapping)
                        i += vowel.length
                        matched = true
                        break
                    }
                }
            }
            
            if (!matched) {
                val currentChar = romanText[i].toString()
                
                if (isConsonant(currentChar)) {
                    val consonantGroup = extractConsonantGroup(romanText, i)
                    
                    if (consonantGroup.length > 1) {
                        // Handle consonant cluster
                        result.append(processConsonantCluster(consonantGroup, targetScript, mappings))
                        i += consonantGroup.length
                    } else {
                        // Single consonant
                        if (i + 1 < romanText.length && isVowel(romanText[i + 1].toString())) {
                            // Consonant + vowel
                            val vowel = convertVowel(romanText[i + 1].toString(), targetScript, mappings)
                            val consonant = convertConsonant(currentChar, targetScript, mappings, false)
                            result.append(consonant + vowel)
                            i += 2
                        } else {
                            // Standalone consonant (half form)
                            val consonant = convertConsonant(currentChar, targetScript, mappings, true)
                            result.append(consonant)
                            i += 1
                        }
                    }
                } else {
                    // Not a consonant or vowel we recognize
                    val mapping = mappings[currentChar]?.get(targetScript) ?: currentChar
                    result.append(mapping)
                    i += 1
                }
            }
        }
        
        return result.toString()
    }
    
    // Helper functions for joint word processing
    private fun isConsonant(char: String): Boolean {
        return consonants.contains(char.lowercase())
    }
    
    private fun isVowel(char: String): Boolean {
        return vowels.contains(char.lowercase())
    }
    
    private fun extractConsonantGroup(text: String, startIndex: Int): String {
        val group = StringBuilder()
        var i = startIndex
        
        while (i < text.length) {
            // Check for multi-character consonants first
            if (i + 1 < text.length) {
                val double = text.substring(i, i + 2).lowercase()
                if (isConsonant(double)) {
                    group.append(double)
                    i += 2
                    continue
                }
            }
            
            // Single character consonant
            val single = text[i].toString()
            if (isConsonant(single)) {
                group.append(single)
                i += 1
            } else {
                break
            }
        }
        
        return group.toString()
    }
    
    private fun processConsonantCluster(cluster: String, targetScript: String, mappings: Map<String, Map<String, String>>): String {
        val result = StringBuilder()
        
        for (j in cluster.indices) {
            val consonant = if (j + 1 < cluster.length && isConsonant(cluster.substring(j, j + 2))) {
                cluster.substring(j, j + 2).also { j++ }
            } else {
                cluster[j].toString()
            }
            
            val isLast = (j == cluster.length - 1)
            val consonantText = convertConsonant(consonant, targetScript, mappings, !isLast)
            result.append(consonantText)
        }
        
        return result.toString()
    }
    
    private fun convertConsonant(consonant: String, targetScript: String, mappings: Map<String, Map<String, String>>, isHalf: Boolean): String {
        var baseConsonant = mappings[consonant]?.get(targetScript) ?: consonant
        
        // For half consonants, we need to add virama (depends on the script)
        if (isHalf) {
            baseConsonant += when (targetScript) {
                "devanagari" -> "्"
                "bengali" -> "্"
                "gujarati" -> "્"
                "kannada" -> "್"
                "malayalam" -> "്"
                "odia" -> "୍"
                "tamil" -> "்"
                "telugu" -> "్"
                else -> ""
            }
        }
        
        return baseConsonant
    }
    
    private fun convertVowel(vowel: String, targetScript: String, mappings: Map<String, Map<String, String>>): String {
        return mappings[vowel]?.get(targetScript) ?: vowel
    }
    
    // Original functions kept for compatibility
    fun romanToScript(romanText: String, targetScript: String): String {
        return romanToScriptWithJointWords(romanText, targetScript)
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
