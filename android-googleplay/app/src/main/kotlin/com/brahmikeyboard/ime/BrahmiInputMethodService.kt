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
    
    override fun onCreate() {
        super.onCreate()
        brahmiEngine = BrahmiEngine(assets)
        preferences = PreferencesManager(this)
    }
    
    override fun onEvaluateInputViewShown(): Boolean {
        return true
    }
    
    override fun onCreateInputView(): View {
        keyboardView = BrahmiKeyboardView(this, brahmiEngine, preferences)
        return keyboardView
    }
    
    override fun onStartInput(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInput(editorInfo, restarting)
        isPasswordField = isPasswordField(editorInfo)
        if (::keyboardView.isInitialized) {
            keyboardView.setInputConnection(currentInputConnection)
            keyboardView.setPasswordField(isPasswordField)
        }
    }
    
    override fun onFinishInput() {
        super.onFinishInput()
        if (::keyboardView.isInitialized) {
            keyboardView.clearPreview()
        }
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
}
