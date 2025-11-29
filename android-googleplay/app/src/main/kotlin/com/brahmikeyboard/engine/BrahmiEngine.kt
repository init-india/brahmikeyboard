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
    
    // Word boundary characters
    private val wordBoundaries = setOf(' ', '\n', '\t', '.', ',', '!', '?', ';', ':', '"', '\'', '(', ')', '[', ']', '{', '}', '@', '#', '$', '%', '&', '*', '/', '\\', '|', '<', '>', '=', '+', '-', '_')
    
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
        // English mode - direct output, single line preview
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
        
        // TRUE PARALLEL PROCESSING - both from Roman source
        val segments = splitTextWithBoundaries(romanInput)
        
        val brahmiResult = StringBuilder()
        val indianResult = StringBuilder()
        
        // Process each segment in parallel
        for (segment in segments) {
            when (segment.type) {
                SegmentType.WORD -> {
                    // PARALLEL PATHS: Both process the same Roman word
                    val brahmiWord = scriptLoader.romanToBrahmiWordLevel(segment.text, currentReferenceScript)
                    val indianWord = scriptLoader.romanToIndianWordLevel(segment.text, currentReferenceScript)
                    
                    brahmiResult.append(brahmiWord)
                    indianResult.append(indianWord)
                }
                SegmentType.BOUNDARY -> {
                    brahmiResult.append(segment.text)
                    indianResult.append(segment.text)
                }
                SegmentType.SYMBOL -> {
                    brahmiResult.append(segment.text)
                    indianResult.append(segment.text)
                }
            }
        }
        
        return ConversionResult(
            brahmiText = brahmiResult.toString(),
            indianScriptText = indianResult.toString(),
            outputText = brahmiResult.toString(), // Brahmi output for BRM mode
            referenceScript = currentReferenceScript
        )
    }
    
    private fun convertPureBrahmi(brahmiInput: String): ConversionResult {
        if (brahmiInput.isEmpty()) {
            return ConversionResult("", "", "", currentReferenceScript)
        }
        
        // For Pure Brahmi mode - Brahmi input, show Indian reference
        val segments = splitTextWithBoundaries(brahmiInput)
        
        val indianResult = StringBuilder()
        
        for (segment in segments) {
            when (segment.type) {
                SegmentType.WORD -> {
                    // Brahmi→Roman→Indian pipeline for word-level accuracy
                    val romanWord = scriptLoader.brahmiToRomanWordLevel(segment.text, currentReferenceScript)
                    val indianWord = scriptLoader.romanToIndianWordLevel(romanWord, currentReferenceScript)
                    indianResult.append(indianWord)
                }
                SegmentType.BOUNDARY -> {
                    indianResult.append(segment.text)
                }
                SegmentType.SYMBOL -> {
                    indianResult.append(segment.text)
                }
            }
        }
        
        return ConversionResult(
            brahmiText = brahmiInput, // Direct Brahmi display
            indianScriptText = indianResult.toString(),
            outputText = brahmiInput, // Direct Brahmi output
            referenceScript = currentReferenceScript
        )
    }
    
    // Enhanced text segmentation
    private fun splitTextWithBoundaries(text: String): List<TextSegment> {
        val segments = mutableListOf<TextSegment>()
        var currentWord = StringBuilder()
        
        for (char in text) {
            if (wordBoundaries.contains(char)) {
                // Commit current word if exists
                if (currentWord.isNotEmpty()) {
                    segments.add(TextSegment(currentWord.toString(), SegmentType.WORD))
                    currentWord.clear()
                }
                // Classify boundary type
                val type = if (char.isLetterOrDigit()) SegmentType.BOUNDARY else SegmentType.SYMBOL
                segments.add(TextSegment(char.toString(), type))
            } else {
                currentWord.append(char)
            }
        }
        
        // Add any remaining word
        if (currentWord.isNotEmpty()) {
            segments.add(TextSegment(currentWord.toString(), SegmentType.WORD))
        }
        
        return segments
    }
}

// Supporting data classes
data class TextSegment(val text: String, val type: SegmentType)

enum class SegmentType {
    WORD, BOUNDARY, SYMBOL
}

enum class KeyboardMode {
    ENGLISH, BRAHMI, PURE_BRAHMI
}
