package com.brahmikeyboard.engine

import android.content.res.AssetManager
import com.brahmikeyboard.data.ScriptMappingLoader

data class ConversionResult(
    val previewText: String,
    val outputText: String,
    val referenceScript: String,
    val brahmiText: String,
    val warnings: List<String> = emptyList()
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
        // This now returns ConversionResult with warnings
        val conversion = scriptLoader.romanToScriptWithJointWords(romanInput, currentReferenceScript)
        val brahmiText = scriptLoader.scriptToBrahmi(conversion.outputText, currentReferenceScript)
        
        // Build preview with warning indicator
        val preview = buildPreviewWithWarnings(conversion.outputText, brahmiText, conversion.warnings)
        
        return ConversionResult(
            previewText = preview,
            outputText = brahmiText,
            referenceScript = currentReferenceScript,
            brahmiText = brahmiText,
            warnings = conversion.warnings
        )
    }
    
    private fun buildPreviewWithWarnings(referenceText: String, brahmiText: String, warnings: List<String>): String {
        val basePreview = "$referenceText = $brahmiText"
        return if (warnings.isNotEmpty()) {
            "$basePreview ⚠️"
        } else {
            basePreview
        }
    }
    
    private fun convertPureBrahmi(brahmiInput: String): ConversionResult {
        // For pure Brahmi mode, we need to convert Brahmi back to reference script for preview
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
