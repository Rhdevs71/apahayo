package com.rhdevs.rhpatch.xposed.features.privacy

import android.content.SharedPreferences
import com.rhdevs.rhpatch.xposed.core.Feature

class MessageBlocker(loader: ClassLoader, preferences: SharedPreferences) : Feature(loader, preferences) {
    override fun doHook() {
        // DIHAPUS: Hook SQL menyebabkan WA crash/ANR (Muat ulang cache). 
        // Logika anti-spam sudah dipindahkan dengan aman ke WaMessageBlockerHook.kt menggunakan NotificationManager.
    }

    override fun getPluginName(): String {
        return "Message Blocker (Deprecated)"
    }
}
