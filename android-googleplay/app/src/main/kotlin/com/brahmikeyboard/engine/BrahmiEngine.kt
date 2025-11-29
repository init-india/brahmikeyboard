package com.brahmikeyboard.engine

import android.content.res.AssetManager
import com.brahmikeyboard.data.ScriptMappingLoader

data class ConversionResult(
    val previewText: String,
    val outputText: String,
    val referenceScript: String,
    val brahmiText: String
)

class BrahmiEngine(private val assets: AssetManager) {
    
    private val scriptLoader = ScriptMappingLoader(assets)
    private var currentReferenceScript = "devanagari"
    
    // Word boundary characters
    private val wordBoundaries = setOf(' ', '\n', '\t', '.', ',', '!', '?', ';', ':', '"', '\'', '(', ')', '[', ']', '{', '}')
    
    fun setReferenceScript(script: String) {
        currentReferenceScript = script
    }
    
    fun convertToBrahmi(input: String, mode: KeyboardMode): ConversionResult {
        return when (mode) {
            KeyboardMode.ENGLISH -> convertEnglish(input)
            KeyboardMode.BRAHMI -> convertBrahmi(input)
            KeyboardMode.PURE_BRAHMI -> convertPureBrahmi(input)
        }
    }
    
    private fun convertEnglish(input: String): ConversionResult {
        // English mode - direct output, no conversion
        return ConversionResult(
            previewText = "English: $input",
            outputText = input,
            referenceScript = "english",
            brahmiText = input
        )
    }
    
    private fun convertBrahmi(romanInput: String): ConversionResult {
        // TRUE PARALLEL PROCESSING - both from Roman source
        val segments = processTextWithWordBoundaries(romanInput)
        
        val brahmiResult = StringBuilder()
        val indianResult = StringBuilder()
        
        // Process each segment
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
                else -> {
                    brahmiResult.append(segment.text)
                    indianResult.append(segment.text)
                }
            }
        }
        
        val brahmiText = brahmiResult.toString()
        val indianText = indianResult.toString()
        val scriptName = getScriptDisplayName(currentReferenceScript)
        val preview = "Brahmi: $brahmiText\n$scriptName: $indianText"
        
        return ConversionResult(
            previewText = preview,
            outputText = brahmiText,
            referenceScript = currentReferenceScript,
            brahmiText = brahmiText
        )
    }
        val scriptName = getScriptDisplayName(currentReferenceScript)
    // Use proper string formatting
       val preview = String.format("Brahmi: %s\n%s: %s", brahmiText, scriptName, indianText)
    
       return ConversionResult(
            previewText = preview,
            outputText = brahmiText,
            referenceScript = currentReferenceScript,
            brahmiText = brahmiText
        )
    }
    private fun convertPureBrahmi(brahmiInput: String): ConversionResult {
        // For Pure Brahmi mode - Brahmi input, show Indian reference
        val segments = processTextWithWordBoundaries(brahmiInput)
        
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
                else -> {
                    indianResult.append(segment.text)
                }
            }
        }
        
        val indianText = indianResult.toString()
        val scriptName = getScriptDisplayName(currentReferenceScript)
        val preview = "Brahmi: $brahmiInput\n$scriptName: $indianText"
        
        return ConversionResult(
            previewText = preview,
            outputText = brahmiInput, // Direct Brahmi output
            referenceScript = currentReferenceScript,
            brahmiText = brahmiInput
        )
    }
    
    
        val scriptName = getScriptDisplayName(currentReferenceScript)
    // Use proper string formatting
       val preview = String.format("Brahmi: %s\n%s: %s", brahmiInput, scriptName, indianText)
    
       return ConversionResult(
            previewText = preview,
            outputText = brahmiInput,
            referenceScript = currentReferenceScript,
            brahmiText = brahmiInput
       )
    }





  // Word boundary processing
    private fun processTextWithWordBoundaries(text: String): List<TextSegment> {
        val segments = mutableListOf<TextSegment>()
        var currentWord = StringBuilder()
        var currentType: SegmentType = SegmentType.TEXT
        
        for (char in text) {
            if (wordBoundaries.contains(char)) {
                // Commit current word if exists
                if (currentWord.isNotEmpty()) {
                    segments.add(TextSegment(currentWord.toString(), SegmentType.WORD))
                    currentWord.clear()
                }
                // Add the boundary as separate segment
                segments.add(TextSegment(char.toString(), SegmentType.BOUNDARY))
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
    
    private fun getScriptDisplayName(script: String): String {
        return when (script) {
            "devanagari" -> "Devanagari"
            "kannada" -> "Kannada"
            "telugu" -> "Telugu"
            "bengali" -> "Bengali"
            "gujarati" -> "Gujarati"
            "tamil" -> "Tamil"
            "malayalam" -> "Malayalam"
            "odia" -> "Odia"
            "punjabi" -> "Punjabi"
            "assamese" -> "Assamese"
            else -> script.replaceFirstChar { it.uppercase() }
        }
    }
}

// Supporting data classes
data class TextSegment(val text: String, val type: SegmentType)

enum class SegmentType {
    WORD, BOUNDARY, TEXT
}

enum class KeyboardMode {
    ENGLISH, BRAHMI, PURE_BRAHMI
}
