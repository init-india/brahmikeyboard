// File: engine/BrahmiEngine.kt
package com.brahmikeyboard.engine

import android.content.res.AssetManager
import com.brahmikeyboard.data.ScriptMappingLoader

data class ConversionResult(
    val brahmiText: String,
    val indianScriptText: String,
    val outputText: String,
    val referenceScript: String
)

class BrahmiEngine(private val assets: AssetManager) {
    
    private val scriptLoader = ScriptMappingLoader(assets)
    private var currentReferenceScript = "devanagari"
    
    fun setReferenceScript(script: String) {
        currentReferenceScript = script
    }
    
    fun getCurrentReferenceScript(): String = currentReferenceScript
    
    fun convertToBrahmi(input: String, mode: KeyboardMode): ConversionResult {
        return when (mode) {
            KeyboardMode.ENGLISH -> convertEnglish(input)
            KeyboardMode.BRAHMI -> convertBrahmi(input)
            KeyboardMode.PURE_BRAHMI -> convertPureBrahmi(input)
        }
    }
    
    private fun convertEnglish(input: String): ConversionResult {
        return ConversionResult(
            brahmiText = input,
            indianScriptText = input,
            outputText = input,
            referenceScript = "english"
        )
    }
    
    private fun convertBrahmi(romanInput: String): ConversionResult {
        if (romanInput.isEmpty()) {
            return ConversionResult("", "", "", currentReferenceScript)
        }
        
        // Process input by splitting into words and syllables
        val words = splitIntoWords(romanInput)
        
        val brahmiResult = StringBuilder()
        val indianResult = StringBuilder()
        
        for (word in words) {
            // Process each word syllable by syllable
            val syllables = splitIntoSyllables(word)
            
            // PARALLEL PROCESSING
            for (syllable in syllables) {
                // Path 1: Roman → Brahmi
                val brahmiSyllable = scriptLoader.romanToBrahmiSyllable(syllable, currentReferenceScript)
                brahmiResult.append(brahmiSyllable)
                
                // Path 2: Roman → Indian Script
                val indianSyllable = scriptLoader.romanToIndianSyllable(syllable, currentReferenceScript)
                indianResult.append(indianSyllable)
            }
            
            // Add space between words
            if (word != words.last()) {
                brahmiResult.append(" ")
                indianResult.append(" ")
            }
        }
        
        return ConversionResult(
            brahmiText = brahmiResult.toString(),
            indianScriptText = indianResult.toString(),
            outputText = brahmiResult.toString(), // Brahmi output
            referenceScript = currentReferenceScript
        )
    }
    
    private fun splitIntoWords(text: String): List<String> {
        // Simple word splitting - preserve spaces for reconstruction
        return text.split(" ").filter { it.isNotEmpty() }
    }
    
    private fun splitIntoSyllables(word: String): List<String> {
        val syllables = mutableListOf<String>()
        var i = 0
        
        while (i < word.length) {
            // Find the next syllable
            val syllable = extractNextSyllable(word, i)
            syllables.add(syllable)
            i += syllable.length
        }
        
        return syllables
    }
    
    private fun extractNextSyllable(word: String, startIndex: Int): String {
        if (startIndex >= word.length) return ""
        
        var i = startIndex
        val syllable = StringBuilder()
        
        // Look for consonant(s) + vowel pattern
        while (i < word.length) {
            val char = word[i]
            
            // If we hit a delimiter in middle of word, stop
            if (char in ",.!?;:") {
                if (syllable.isEmpty()) {
                    syllable.append(char)
                    i++
                }
                break
            }
            
            syllable.append(char)
            i++
            
            // Check if we have a complete syllable
            if (isCompleteSyllable(syllable.toString(), i < word.length)) {
                break
            }
        }
        
        return syllable.toString()
    }
    
    private fun isCompleteSyllable(syllable: String, hasMoreChars: Boolean): Boolean {
        if (syllable.isEmpty()) return false
        
        // A syllable is complete when:
        // 1. It ends with a vowel
        // 2. Or it's followed by another consonant (start of new syllable)
        // 3. Or it's the end of the word
        
        val lastChar = syllable.last()
        val isVowel = lastChar in "aeiou"
        
        return isVowel || !hasMoreChars
    }
    
    private fun convertPureBrahmi(brahmiInput: String): ConversionResult {
        if (brahmiInput.isEmpty()) {
            return ConversionResult("", "", "", currentReferenceScript)
        }
        
        // For Pure Brahmi mode, input is already in Brahmi
        // We just need to convert to Indian script for line 2 preview
        
        val indianResult = StringBuilder()
        var i = 0
        
        while (i < brahmiInput.length) {
            // Try to extract a Brahmi unit (character or combined)
            val unit = extractBrahmiUnit(brahmiInput, i)
            
            // Convert Brahmi → Roman → Indian
            val roman = scriptLoader.brahmiToRoman(unit)
            val indian = scriptLoader.romanToIndianSyllable(roman, currentReferenceScript)
            indianResult.append(indian)
            
            i += unit.length
        }
        
        return ConversionResult(
            brahmiText = brahmiInput, // Direct Brahmi for line 1
            indianScriptText = indianResult.toString(),
            outputText = brahmiInput, // Direct Brahmi output
            referenceScript = currentReferenceScript
        )
    }
    
    private fun extractBrahmiUnit(text: String, startIndex: Int): String {
        if (startIndex >= text.length) return ""
        
        // Simple: take one character at a time
        // In reality, Brahmi might have combined characters
        return text[startIndex].toString()
    }
}

// Supporting enums
enum class KeyboardMode { ENGLISH, BRAHMI, PURE_BRAHMI }
