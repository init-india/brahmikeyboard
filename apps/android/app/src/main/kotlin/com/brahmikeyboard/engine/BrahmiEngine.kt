package com.brahmikeyboard.engine

import android.content.res.AssetManager
import com.brahmikeyboard.data.ScriptMappingLoader
import kotlinx.serialization.json.Json

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
    
    fun convertToBrahmi(romanInput: String, mode: KeyboardMode): ConversionResult {
        return when (mode) {
            KeyboardMode.ENGLISH -> convertEnglish(romanInput)
            KeyboardMode.BRAHMI -> convertBrahmi(romanInput)
            KeyboardMode.PURE_BRAHMI -> convertPureBrahmi(romanInput)
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
        val referenceText = scriptLoader.romanToScript(romanInput, currentReferenceScript)
        val brahmiText = scriptLoader.scriptToBrahmi(referenceText, currentReferenceScript)
        
        return ConversionResult(
            previewText = "$referenceText = $brahmiText",
            outputText = brahmiText,
            referenceScript = currentReferenceScript,
            brahmiText = brahmiText
        )
    }
    
    private fun convertPureBrahmi(brahmiInput: String): ConversionResult {
        val referenceText = scriptLoader.brahmiToScript(brahmiInput, currentReferenceScript)
        
        return ConversionResult(
            previewText = "$referenceText = $brahmiInput",
            outputText = brahmiInput,
            referenceScript = currentReferenceScript,
            brahmiText = brahmiInput
        )
    }
}

enum class KeyboardMode {
    ENGLISH, BRAHMI, PURE_BRAHMI
}
