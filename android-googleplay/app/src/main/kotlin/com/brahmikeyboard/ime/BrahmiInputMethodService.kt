package com.brahmikeyboard.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import com.brahmikeyboard.engine.BrahmiEngine
import com.brahmikeyboard.data.PreferencesManager

class BrahmiInputMethodService : InputMethodService() {
    
    private lateinit var keyboardView: BrahmiKeyboardView
    private lateinit var brahmiEngine: BrahmiEngine
    private lateinit var preferences: PreferencesManager
    
    override fun onCreate() {
        super.onCreate()
        brahmiEngine = BrahmiEngine(assets)
        preferences = PreferencesManager(this)
        
        // Use Android's built-in Notification (no extra dependency needed)
        val notification = android.app.Notification.Builder(this)
            .setContentTitle("Brahmi Keyboard")
            .setContentText("Input method service is active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(android.app.Notification.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        startForeground(1, notification)
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
        keyboardView.setInputConnection(currentInputConnection)
    }
    
    override fun onFinishInput() {
        super.onFinishInput()
        keyboardView.clearPreview()
    }
}
