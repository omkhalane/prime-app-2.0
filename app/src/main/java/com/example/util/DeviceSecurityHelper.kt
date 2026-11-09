package com.example.util

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build

object DeviceSecurityHelper {

    /**
     * Checks if the device has a secure screen lock set (PIN, Pattern, Password, or Biometrics).
     */
    fun isDeviceSecure(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            km.isDeviceSecure
        } else {
            km.isKeyguardSecure
        }
    }

    /**
     * Creates an Intent to prompt the user to re-authenticate with their system device credentials
     * (Screen Lock PIN, Pattern, Password, or Biometrics).
     */
    fun createConfirmDeviceCredentialIntent(
        context: Context,
        title: String = "Re-authenticate to Access Never Allowed",
        description: String = "Enter your phone's screen lock (PIN, pattern, or password) or use biometrics to continue."
    ): Intent? {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            km.createConfirmDeviceCredentialIntent(title, description)
        } else {
            null
        }
    }
}
