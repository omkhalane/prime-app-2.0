package com.example.vpn.service

import android.content.Context
import android.content.Intent
import android.net.VpnService

object VpnPermissionManager {

    /**
     * Checks whether the user has already granted VPN permission to the application.
     * Returns true if granted (prepare() returns null).
     */
    fun isVpnPrepared(context: Context): Boolean {
        return VpnService.prepare(context) == null
    }

    /**
     * Returns the Intent needed to launch the system VPN permission dialog,
     * or null if permission is already granted.
     */
    fun getPrepareVpnIntent(context: Context): Intent? {
        return VpnService.prepare(context)
    }
}
