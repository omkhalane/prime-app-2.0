package com.example.util

import android.app.KeyguardManager
import android.content.Context

object DeviceSecurityHelper {
    /** Returns true only when Android reports a secure device credential is configured. */
    fun isDeviceSecure(context: Context): Boolean {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return false
        return keyguard.isDeviceSecure
    }
}
