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
        return ConversionResult(
            previewText = "Brahmi: $input\nEnglish: $input",
            outputText = input,
            referenceScript = "english",
            brahmiText = input
        )
    }
    
    private fun convertBrahmi(romanInput: String): ConversionResult {
        // PARALLEL PROCESSING
        // Path 1: Roman → Indian Script (for Line 2)
        val indianScript = scriptLoader.romanToIndianScript(romanInput, currentReferenceScript)
        
        // Path 2: Roman → Brahmi (for Line 1 and output)
        val brahmiText = scriptLoader.romanToBrahmiScript(romanInput, currentReferenceScript)
        
        val scriptName = getScriptDisplayName(currentReferenceScript)
        val preview = "Brahmi: $brahmiText\n$scriptName: $indianScript"
        
        return ConversionResult(
            previewText = preview,
            outputText = brahmiText,
            referenceScript = currentReferenceScript,
            brahmiText = brahmiText
        )
    }
    
    private fun convertPureBrahmi(brahmiInput: String): ConversionResult {
        // Convert Brahmi to Indian script for Line 2 preview only
        val indianScript = scriptLoader.brahmiToScript(brahmiInput, currentReferenceScript)
        val scriptName = getScriptDisplayName(currentReferenceScript)
        val preview = "Brahmi: $brahmiInput\n$scriptName: $indianScript"
        
        return ConversionResult(
            previewText = preview,
            outputText = brahmiInput,
            referenceScript = currentReferenceScript,
            brahmiText = brahmiInput
        )
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

enum class KeyboardMode {
    ENGLISH, BRAHMI, PURE_BRAHMI
}
