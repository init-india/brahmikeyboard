package com.brahmikeyboard.data

import android.content.res.AssetManager
import kotlinx.serialization.json.Json
import java.io.InputStream

data class ConversionResult(
    val previewText: String,
    val outputText: String,
    val referenceScript: String,
    val brahmiText: String,
    val warnings: List<String> = emptyList()
)

data class UnsupportedMapping(
    val approximation: String,
    val warning: String
)

class ScriptMappingLoader(private val assets: AssetManager) {
    
    private val scriptMappings = mutableMapOf<String, Map<String, String>>()
    private val romanToScriptMappings = mutableMapOf<String, Map<String, String>>()
    
    // Brahmi-compliant vowel definitions (exactly as specified)
    private val vowels = setOf(
        "a", "aa",    // अ, आ
        "i", "ee",    // इ, ई
        "u", "uu",    // उ, ऊ  
        "e", "ei",    // ए, ऐ
        "o", "ou"     // ओ, औ
    )
    
    private val vowelPriority = listOf(
        "aa", "uu", "ee", "ei", "ou",
        "a", "i", "u", "e", "o"
    )
    
    // Brahmi-compliant consonant definitions (exactly as specified)
    private val consonants = setOf(
        "k", "kh", "g", "gh", "nga",  // क, ख, ग, घ, ङ
        "c", "ch", "j", "jh", "yn",   // च, छ, ज, झ, ञ
        "T", "Th", "D", "Dh", "N",    // ट, ठ, ड, ढ, ण
        "t", "th", "d", "dh", "n",    // त, थ, द, ध, न
        "p", "ph", "b", "bh", "m",    // प, फ, ब, भ, म
        "y", "r", "l", "v", "s", "h", // य, र, ल, व, स, ह
        "L"                            // ळ (distinct from ल)
    )
    
    // Unsupported sequences with their approximations and warnings
    private val unsupportedSequences = mapOf(
        "sha" to UnsupportedMapping("sa", "ष (sha) not supported in Brahmi. Using स (sa)"),
        "Sha" to UnsupportedMapping("sa", "ष (Sha) not supported in Brahmi. Using स (sa)"),
        "sh" to UnsupportedMapping("s", "श (sh) not supported in Brahmi. Using स (s)"),
        "shi" to UnsupportedMapping("si", "शि (shi) not supported in Brahmi. Using सि (si)"),
        "dnya" to UnsupportedMapping("jyn", "ज्ञ (dnya) not in Brahmi. Using ज्ञ (jyn)"),
        "gya" to UnsupportedMapping("jyn", "ज्ञ (gya) not in Brahmi. Using ज्ञ (jyn)"),
        "ksha" to UnsupportedMapping("ksa", "क्ष (ksha) not in Brahmi. Using क्स (ksa)"),
        "aum" to UnsupportedMapping("om", "ॐ (aum) not supported in Brahmi. Using ओम् (om)"),
        "om" to UnsupportedMapping("om", "ॐ (om) not supported in Brahmi. Using ओम् (om)"),
        "ru" to UnsupportedMapping("ri", "ऋ (ru) not supported in Brahmi. Using रि (ri)"),
        "rru" to UnsupportedMapping("rri", "ॠ (rru) not supported in Brahmi. Using र्रि (rri)"),
        "lri" to UnsupportedMapping("li", "ऌ (lri) not supported in Brahmi. Using लि (li)"),
        "au" to UnsupportedMapping("ou", "औ (au) represented as ओउ (ou) in Brahmi"),
        "ai" to UnsupportedMapping("ei", "ऐ (ai) represented as एइ (ei) in Brahmi")
    )
    
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
    
    // Enhanced function for joint words with warning system
    fun romanToScriptWithJointWords(romanText: String, targetScript: String): ConversionResult {
        val mappings = loadRomanToScriptMappings()
        var result = StringBuilder()
        val warnings = mutableListOf<String>()
        var i = 0
        
        while (i < romanText.length) {
            var matched = false
            
            // Handle anusvara (^ symbol)
            if (i < romanText.length && romanText[i] == '^') {
                result.append(convertAnusvara(targetScript))
                i += 1
                matched = true
                continue
            }
            
            // Check for unsupported sequences first
            for ((unsupported, mappingInfo) in unsupportedSequences.entries.sortedByDescending { it.key.length }) {
                if (i + unsupported.length <= romanText.length) {
                    val test = romanText.substring(i, i + unsupported.length).lowercase()
                    if (test == unsupported.lowercase()) {
                        // Add warning
                        warnings.add(mappingInfo.warning)
                        // Use approximation
                        val approximationResult = processTextSegment(mappingInfo.approximation, targetScript, mappings)
                        result.append(approximationResult)
                        i += unsupported.length
                        matched = true
                        break
                    }
                }
            }
            
            if (matched) continue
            
            // Try vowel combinations
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
                    // Handle special characters
                    if (currentChar in setOf("@", "#", "$", "%", "&", "*", "!", "?", ".", ",", ":", ";", "-", "_", "=", "+")) {
                        result.append(currentChar)
                        i += 1
                    } else {
                        val mapping = mappings[currentChar]?.get(targetScript) ?: currentChar
                        result.append(mapping)
                        i += 1
                    }
                }
            }
        }
        
        return ConversionResult(
            previewText = result.toString(),
            outputText = result.toString(),
            referenceScript = targetScript,
            brahmiText = result.toString(),
            warnings = warnings
        )
    }
    
    private fun processTextSegment(text: String, targetScript: String, mappings: Map<String, Map<String, String>>): String {
        var segmentResult = StringBuilder()
        var j = 0
        
        while (j < text.length) {
            var matched = false
            for (vowel in vowelPriority) {
                if (j + vowel.length <= text.length) {
                    val test = text.substring(j, j + vowel.length)
                    if (test == vowel) {
                        val mapping = mappings[test]?.get(targetScript) ?: test
                        segmentResult.append(mapping)
                        j += vowel.length
                        matched = true
                        break
                    }
                }
            }
            
            if (!matched) {
                val char = text[j].toString()
                if (isConsonant(char)) {
                    if (j + 1 < text.length && isVowel(text[j + 1].toString())) {
                        val vowel = convertVowel(text[j + 1].toString(), targetScript, mappings)
                        val consonant = convertConsonant(char, targetScript, mappings, false)
                        segmentResult.append(consonant + vowel)
                        j += 2
                    } else {
                        val consonant = convertConsonant(char, targetScript, mappings, true)
                        segmentResult.append(consonant)
                        j += 1
                    }
                } else {
                    val mapping = mappings[char]?.get(targetScript) ?: char
                    segmentResult.append(mapping)
                    j += 1
                }
            }
        }
        
        return segmentResult.toString()
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
        var currentIndex = startIndex
        
        while (currentIndex < text.length) {
            // Check for multi-character consonants first
            if (currentIndex + 2 < text.length) {
                val triple = text.substring(currentIndex, currentIndex + 3).lowercase()
                if (isConsonant(triple)) {
                    group.append(triple)
                    currentIndex += 3
                    continue
                }
            }
            
            if (currentIndex + 1 < text.length) {
                val double = text.substring(currentIndex, currentIndex + 2).lowercase()
                if (isConsonant(double)) {
                    group.append(double)
                    currentIndex += 2
                    continue
                }
            }
            
            // Single character consonant
            val single = text[currentIndex].toString()
            if (isConsonant(single)) {
                group.append(single)
                currentIndex += 1
            } else {
                break
            }
        }
        
        return group.toString()
    }
    
    private fun processConsonantCluster(cluster: String, targetScript: String, mappings: Map<String, Map<String, String>>): String {
        val result = StringBuilder()
        var j = 0
        
        while (j < cluster.length) {
            val consonant = if (j + 2 < cluster.length && isConsonant(cluster.substring(j, j + 3))) {
                val tripleConsonant = cluster.substring(j, j + 3)
                j += 3
                tripleConsonant
            } else if (j + 1 < cluster.length && isConsonant(cluster.substring(j, j + 2))) {
                val doubleConsonant = cluster.substring(j, j + 2)
                j += 2
                doubleConsonant
            } else {
                val singleConsonant = cluster[j].toString()
                j += 1
                singleConsonant
            }
            
            val isLast = (j >= cluster.length)
            val consonantText = convertConsonant(consonant, targetScript, mappings, !isLast)
            result.append(consonantText)
        }
        
        return result.toString()
    }
    
    private fun convertConsonant(consonant: String, targetScript: String, mappings: Map<String, Map<String, String>>, isHalf: Boolean): String {
        var baseConsonant = mappings[consonant]?.get(targetScript) ?: consonant
        
        // For half consonants, add virama
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
    
    private fun convertAnusvara(targetScript: String): String {
        return when (targetScript) {
            "devanagari" -> "ं"
            "bengali" -> "ং"
            "gujarati" -> "ં"
            "kannada" -> "ಂ"
            "malayalam" -> "ം"
            "odia" -> "ଂ"
            "tamil" -> "ஂ"
            "telugu" -> "ం"
            else -> "ं"
        }
    }
    
    // Original functions kept for compatibility
    fun romanToScript(romanText: String, targetScript: String): ConversionResult {
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
