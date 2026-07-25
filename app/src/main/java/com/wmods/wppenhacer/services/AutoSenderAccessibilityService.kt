package com.wmods.wppenhacer.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Dummy service to prevent ClassNotFoundException on devices where the old
 * Accessibility Service component name is still cached by the system.
 */
class AutoSenderAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf()
        return super.onStartCommand(intent, flags, startId)
    }
}
