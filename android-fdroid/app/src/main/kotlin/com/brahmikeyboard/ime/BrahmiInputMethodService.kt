package com.brahmikeyboard.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.text.InputType
import com.brahmikeyboard.engine.BrahmiEngine
import com.brahmikeyboard.data.PreferencesManager

class BrahmiInputMethodService : InputMethodService() {
    
    private lateinit var keyboardView: BrahmiKeyboardView
    private lateinit var brahmiEngine: BrahmiEngine
    private lateinit var preferences: PreferencesManager
    private var isPasswordField: Boolean = false
    private var isOtpField: Boolean = false
    
    override fun onCreate() {
        super.onCreate()
        brahmiEngine = BrahmiEngine(assets)
        preferences = PreferencesManager(this)
        
        // Set initial reference script
        brahmiEngine.setReferenceScript(preferences.getReferenceScript())
    }
    
    override fun onCreateInputView(): View {
        keyboardView = BrahmiKeyboardView(this, brahmiEngine, preferences)
        return keyboardView
    }
    
    override fun onStartInput(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInput(editorInfo, restarting)
        isPasswordField = isPasswordField(editorInfo)
        isOtpField = isOtpField(editorInfo)
        
        if (::keyboardView.isInitialized) {
            keyboardView.setInputConnection(currentInputConnection)
            keyboardView.setPasswordField(isPasswordField || isOtpField)
            keyboardView.updateFromPreferences()
        }
    }
    
    override fun onFinishInput() {
        super.onFinishInput()
        if (::keyboardView.isInitialized) {
            keyboardView.clearPreview()
        }
    }
    
    override fun onEvaluateInputViewShown(): Boolean {
        return true
    }
    
    override fun onWindowShown() {
        super.onWindowShown()
        applyThemeSettings()
    }
    
    private fun isPasswordField(editorInfo: EditorInfo?): Boolean {
        if (editorInfo == null) return false
        
        val inputType = editorInfo.inputType
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
               variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
               variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
               (inputType and InputType.TYPE_NUMBER_VARIATION_PASSWORD) == InputType.TYPE_NUMBER_VARIATION_PASSWORD
    }
    
    private fun isOtpField(editorInfo: EditorInfo?): Boolean {
        if (editorInfo == null) return false
        
        // Check for OTP field hints
        val hintText = editorInfo.hintText?.toString()?.lowercase() ?: ""
        val packageName = editorInfo.packageName?.lowercase() ?: ""
        
        return hintText.contains("otp") || 
               hintText.contains("one time password") ||
               hintText.contains("verification code") ||
               packageName.contains("bank") ||
               packageName.contains("payment") ||
               packageName.contains("wallet")
    }
    
    private fun applyThemeSettings() {
        if (::keyboardView.isInitialized) {
            keyboardView.applyTheme(preferences.getThemeMode())
        }
    }
    
    fun getBrahmiEngine(): BrahmiEngine = brahmiEngine
    
    fun getPreferences(): PreferencesManager = preferences
}
