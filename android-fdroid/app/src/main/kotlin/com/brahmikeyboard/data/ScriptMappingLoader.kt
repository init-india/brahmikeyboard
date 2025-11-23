package com.brahmikeyboard.data

import android.content.res.AssetManager
import kotlinx.serialization.json.Json
import java.io.InputStream

class ScriptMappingLoader(private val assets: AssetManager) {
    
    private val scriptMappings = mutableMapOf<String, Map<String, String>>()
    private val romanToIndianMappings = mutableMapOf<String, Map<String, String>>()
    
    // Brahmi combinations sorted by length (longest first)
    private val brahmiCombinations = listOf(
        "aa", "ee", "uu", "ei", "ou",
        "kh", "gh", "nga", "ch", "jh", "yn",
        "Th", "Dh", "N", "th", "dh", "ph", "bh", "L"
    ).sortedByDescending { it.length }

    private fun loadRomanToIndianMappings(): Map<String, Map<String, String>> {
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
    
    // Roman to Indian Script conversion
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
    
    // Direct Roman to Brahmi conversion
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
