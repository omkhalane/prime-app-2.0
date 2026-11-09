package com.example.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.vpn.dns.DnsFilterEngine
import com.example.vpn.dns.DnsFilterResult
import com.example.vpn.dns.IpPacketHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class PrimeVpnService : VpnService() {

    companion object {
        private const val TAG = "PrimeVpnService"
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "prime_vpn_channel"

        const val ACTION_START = "com.example.vpn.START"
        const val ACTION_STOP = "com.example.vpn.STOP"
        const val ACTION_RELOAD_RULES = "com.example.vpn.RELOAD_RULES"

        @Volatile
        var isServiceRunning: Boolean = false
            private set

        fun startService(context: Context) {
            val intent = Intent(context, PrimeVpnService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, PrimeVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var workerJob: Job? = null

    private lateinit var dnsFilterEngine: DnsFilterEngine
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        dnsFilterEngine = DnsFilterEngine(
            socketProtector = { socket -> protect(socket) }
        )
        dnsFilterEngine.onBlockedAttempt = { domain, category, reason ->
            BlockingManager.getInstance(this).onDomainBlocked(domain, category, reason)
        }
        createNotificationChannel()
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopVpn()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RELOAD_RULES -> {
                reloadFilterRules()
                return START_STICKY
            }
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildForegroundNotification())
                reloadFilterRules()
                if (vpnInterface == null) {
                    establishVpn()
                }
                return START_STICKY
            }
        }
        return START_STICKY
    }

    private fun establishVpn() {
        try {
            val builder = Builder().apply {
                setSession("PRIME Website Shield")
                // Virtual IPv4 interface address
                addAddress("10.254.1.1", 32)
                // Virtual DNS server address
                addDnsServer("10.254.1.2")
                // Route all DNS traffic destined to virtual server through this TUN interface
                addRoute("10.254.1.2", 32)

                // Route common public DNS addresses to catch hardcoded browser DNS resolvers
                addRoute("1.1.1.1", 32)
                addRoute("1.0.0.1", 32)
                addRoute("8.8.8.8", 32)
                addRoute("8.8.4.4", 32)
                addRoute("9.9.9.9", 32)

                // IPv6 DNS to prevent DNS leaks on IPv6-enabled networks
                try {
                    addAddress("fd00:1:fd00:1::1", 128)
                    addDnsServer("fd00:1:fd00:1::2")
                    addRoute("fd00:1:fd00:1::2", 128)
                    addRoute("2001:4860:4860::8888", 128)
                    addRoute("2001:4860:4860::8844", 128)
                    addRoute("2606:4700:4700::1111", 128)
                    addRoute("2606:4700:4700::1001", 128)
                } catch (_: Exception) {}

                try {
                    addDisallowedApplication(packageName)
                } catch (_: Exception) {}

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setMetered(false)
                }
                setBlocking(true)
            }

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                isServiceRunning = true
                BlockingManager.getInstance(this).updateVpnRunningState(true)
                startPacketProcessingLoop()
                Log.i(TAG, "PRIME Local DNS VPN established successfully")
            } else {
                Log.e(TAG, "Failed to establish VPN interface: prepare() might be required or another VPN active")
                stopVpn()
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception establishing VPN interface", e)
            stopVpn()
            stopSelf()
        }
    }

    private fun startPacketProcessingLoop() {
        val pfd = vpnInterface ?: return
        val inStream = FileInputStream(pfd.fileDescriptor)
        val outStream = FileOutputStream(pfd.fileDescriptor)

        workerJob?.cancel()
        workerJob = serviceScope.launch {
            val packetBuffer = ByteArray(32767)

            while (isActive && isServiceRunning) {
                try {
                    val length = inStream.read(packetBuffer)
                    if (length <= 0) continue

                    val udpPacket = IpPacketHelper.parseIpv4Udp(packetBuffer, length)
                    if (udpPacket != null && udpPacket.destPort == 53) {
                        // DNS query arrived on port 53
                        val filterResult = dnsFilterEngine.processDnsPayload(udpPacket.payload)

                        when (filterResult) {
                            is DnsFilterResult.Blocked -> {
                                val responsePacket = IpPacketHelper.buildIpv4UdpPacket(
                                    sourceIp = udpPacket.destIp,
                                    destIp = udpPacket.sourceIp,
                                    sourcePort = udpPacket.destPort,
                                    destPort = udpPacket.sourcePort,
                                    udpPayload = filterResult.responsePayload
                                )
                                outStream.write(responsePacket)
                            }
                            is DnsFilterResult.Allowed -> {
                                val responsePacket = IpPacketHelper.buildIpv4UdpPacket(
                                    sourceIp = udpPacket.destIp,
                                    destIp = udpPacket.sourceIp,
                                    sourcePort = udpPacket.destPort,
                                    destPort = udpPacket.sourcePort,
                                    udpPayload = filterResult.responsePayload
                                )
                                outStream.write(responsePacket)
                            }
                            is DnsFilterResult.Ignored -> {
                                // No action
                            }
                        }
                    }
                } catch (e: IOException) {
                    if (isActive && isServiceRunning) {
                        Log.w(TAG, "Error in packet processing loop: ${e.message}")
                    }
                    break
                } catch (e: Exception) {
                    if (isActive && isServiceRunning) {
                        Log.w(TAG, "Unexpected error in packet processing loop: ${e.message}")
                    }
                }
            }
        }
    }

    private fun reloadFilterRules() {
        serviceScope.launch {
            val manager = BlockingManager.getInstance(this@PrimeVpnService)
            val activeRules = manager.resolveCurrentActiveRules()
            dnsFilterEngine.updateRules(activeRules, emptySet())
        }
    }

    private fun stopVpn() {
        isServiceRunning = false
        BlockingManager.getInstance(this).updateVpnRunningState(false)
        workerJob?.cancel()
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
        dnsFilterEngine.clearCache()
    }

    override fun onRevoke() {
        Log.w(TAG, "VPN permission revoked by system or superseded by another VPN")
        stopVpn()
        stopSelf()
        BlockingManager.getInstance(this).onVpnRevoked()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        unregisterNetworkCallback()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun registerNetworkCallback() {
        try {
            connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    // Flush cache on network handover (Wi-Fi <-> Mobile)
                    dnsFilterEngine.clearCache()
                }
            }
            networkCallback?.let {
                connectivityManager?.registerDefaultNetworkCallback(it)
            }
        } catch (_: Exception) {}
    }

    private fun unregisterNetworkCallback() {
        try {
            networkCallback?.let {
                connectivityManager?.unregisterNetworkCallback(it)
            }
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PRIME Website Shield",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active status when local website protection is running"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, PrimeVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PRIME Website Shield Active")
            .setContentText("Protecting your focus by filtering distracting web domains locally.")
            .setSmallIcon(R.drawable.prime_logo)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "Stop Shield", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
