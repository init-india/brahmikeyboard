package com.brahmikeyboard.ime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Keyboard will be available after boot
            // No specific action needed as IME is system-managed
        }
    }
}
