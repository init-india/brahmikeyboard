package com.brahmikeyboard.data

import android.content.Context
import android.view.inputmethod.InputConnection  // FIXED: Add inputmethod
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.brahmikeyboard.engine.BrahmiEngine
import com.brahmikeyboard.engine.KeyboardMode
import com.brahmikeyboard.data.PreferencesManager
import com.brahmikeyboard.ime.foss.R 

class ScriptMappingLoader(private val assets: AssetManager) {
    
    private val romanMappings = loadRomanMappings()
    private val scriptMappings = mutableMapOf<String, Map<String, String>>()
    
    private fun loadRomanMappings(): Map<String, Map<String, String>> {
        return try {
            val inputStream: InputStream = assets.open("script-mappings/roman-to-indian-scripts.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            Json.decodeFromString<RomanMappings>(jsonString).mappings
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
        
        // Add vowels
        data.brahmi_mappings.vowels?.forEach { (key, value) ->
            fullMap[key] = value
        }
        
        // Add consonants
        data.brahmi_mappings.consonants?.forEach { (key, value) ->
            fullMap[key] = value
        }
        
        // Add vowel marks
        data.brahmi_mappings.vowel_marks?.forEach { (key, value) ->
            fullMap[key] = value
        }
        
        // Add special marks
        data.brahmi_mappings.special_marks?.forEach { (key, value) ->
            fullMap[key] = value
        }
        
        // Add numerals
        data.brahmi_mappings.numerals?.forEach { (key, value) ->
            fullMap[key] = value
        }
        
        return fullMap
    }
    
    fun romanToScript(romanText: String, targetScript: String): String {
        var result = romanText
        val mappings = romanMappings
        
        // Process consonants first (longer sequences)
        mappings["consonants"]?.forEach { (roman, scriptMap) ->
            val targetChar = scriptMap[targetScript] ?: return@forEach
            result = result.replace(roman, targetChar)
        }
        
        // Process vowels
        mappings["vowels"]?.forEach { (roman, scriptMap) ->
            val targetChar = scriptMap[targetScript] ?: return@forEach
            result = result.replace(roman, targetChar)
        }
        
        return result
    }
    
    fun scriptToBrahmi(scriptText: String, sourceScript: String): String {
        val mapping = loadScriptMapping(sourceScript)
        var result = scriptText
        
        mapping.forEach { (scriptChar, brahmiChar) ->
            result = result.replace(scriptChar, brahmiChar)
        }
        
        return result
    }
    
    fun brahmiToScript(brahmiText: String, targetScript: String): String {
        val mapping = loadScriptMapping(targetScript)
        val reverseMapping = mapping.entries.associate { (k, v) -> v to k }
        var result = brahmiText
        
        reverseMapping.forEach { (brahmiChar, scriptChar) ->
            result = result.replace(brahmiChar, scriptChar)
        }
        
        return result
    }
}

@kotlinx.serialization.Serializable
data class RomanMappings(val mappings: Map<String, Map<String, Map<String, String>>>)

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
