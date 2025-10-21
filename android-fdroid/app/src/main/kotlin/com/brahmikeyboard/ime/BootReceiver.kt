package com.brahmikeyboard.ime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // The system will automatically rediscover enabled IMEs after boot
            // This receiver just ensures our IME is in the enabled state
        }
    }
}
