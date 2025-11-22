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
            previewText = input,
            outputText = input,
            referenceScript = "english",
            brahmiText = input
        )
    }
    
    private fun convertBrahmi(romanInput: String): ConversionResult {
        // DEBUG: Add this temporary logging
        println("DEBUG convertBrahmi: input='$romanInput'")
        println("DEBUG convertBrahmi: currentReferenceScript='$currentReferenceScript'")
        
        // Convert Roman to Indian script first
        val indianScript = scriptLoader.romanToIndianScript(romanInput, currentReferenceScript)
        println("DEBUG convertBrahmi: indianScript='$indianScript'")
        
        // Then convert Indian script to Brahmi
        val brahmiText = scriptLoader.scriptToBrahmi(indianScript, currentReferenceScript)
        println("DEBUG convertBrahmi: brahmiText='$brahmiText'")
        
        val preview = "$indianScript = $brahmiText"
        println("DEBUG convertBrahmi: final preview='$preview'")
        
        return ConversionResult(
            previewText = preview,
            outputText = brahmiText,
            referenceScript = currentReferenceScript,
            brahmiText = brahmiText
        )
    }
    
    private fun convertPureBrahmi(brahmiInput: String): ConversionResult {
        // For pure Brahmi mode, convert Brahmi back to Indian script for preview only
        val indianScript = scriptLoader.brahmiToScript(brahmiInput, currentReferenceScript)
        
        return ConversionResult(
            previewText = "$indianScript = $brahmiInput",
            outputText = brahmiInput,
            referenceScript = currentReferenceScript,
            brahmiText = brahmiInput
        )
    }
}

enum class KeyboardMode {
    ENGLISH, BRAHMI, PURE_BRAHMI
}
