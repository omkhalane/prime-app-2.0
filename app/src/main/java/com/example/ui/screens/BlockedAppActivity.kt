package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.MainActivity
import com.example.R
import com.example.service.OpalAccessibilityService
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary
import com.example.ui.theme.OpalTheme
import com.example.util.FaviconManager
import kotlinx.coroutines.delay
import java.io.File

class BlockedAppActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_IS_WEBSITE = "extra_is_website"
        const val EXTRA_WEBSITE_DOMAIN = "extra_website_domain"
        const val EXTRA_BLOCKED_URL = "extra_blocked_url"
        const val EXTRA_CATEGORY = "extra_category"

        fun start(context: Context, packageName: String, appName: String) {
            val intent = Intent(context, BlockedAppActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_IS_WEBSITE, false)
            }
            context.startActivity(intent)
        }

        fun startForWebsite(
            context: Context,
            domain: String,
            fullUrl: String = "",
            browserPackage: String = "",
            category: String = "Website Blocked"
        ) {
            val intent = Intent(context, BlockedAppActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_IS_WEBSITE, true)
                putExtra(EXTRA_WEBSITE_DOMAIN, domain)
                putExtra(EXTRA_BLOCKED_URL, fullUrl)
                putExtra(EXTRA_PACKAGE_NAME, browserPackage)
                putExtra(EXTRA_APP_NAME, domain)
                putExtra(EXTRA_CATEGORY, category)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isWebsite = intent.getBooleanExtra(EXTRA_IS_WEBSITE, false)
        val websiteDomain = intent.getStringExtra(EXTRA_WEBSITE_DOMAIN) ?: ""
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "Focus Shield"

        val displayName = if (isWebsite && websiteDomain.isNotBlank()) {
            websiteDomain
        } else {
            val rawName = intent.getStringExtra(EXTRA_APP_NAME) ?: resolveAppName(packageName)
            com.example.util.formatAppName(rawName)
        }

        setContent {
            OpalTheme {
                BlockedAppScreen(
                    isWebsite = isWebsite,
                    websiteDomain = websiteDomain,
                    blockedPackage = packageName,
                    displayName = displayName,
                    categoryName = category,
                    onGoHome = {
                        goToHomeScreen()
                    },
                    onSnooze15Min = {
                        if (isWebsite && websiteDomain.isNotBlank()) {
                            snoozeWebsite(websiteDomain)
                        } else {
                            snoozePackage(packageName)
                        }
                        finish()
                    },
                    onOpenPrime = {
                        val primeIntent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(primeIntent)
                        finish()
                    }
                )
            }
        }
    }

    private fun resolveAppName(pkg: String): String {
        if (pkg.isBlank()) return "Distracting App"
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            pkg.substringAfterLast('.')
        }
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    private fun snoozePackage(pkg: String) {
        if (pkg.isNotBlank()) {
            OpalAccessibilityService.snoozePackage(pkg, 15 * 60 * 1000L)
        }
    }

    private fun snoozeWebsite(domain: String) {
        if (domain.isNotBlank()) {
            OpalAccessibilityService.snoozeWebsite(domain, 15 * 60 * 1000L)
        }
    }
}

@Composable
fun BlockedAppScreen(
    isWebsite: Boolean,
    websiteDomain: String,
    blockedPackage: String,
    displayName: String,
    categoryName: String,
    onGoHome: () -> Unit,
    onSnooze15Min: () -> Unit,
    onOpenPrime: () -> Unit
) {
    val context = LocalContext.current
    var isBreathingActive by remember { mutableStateOf(false) }
    var breathingPhase by remember { mutableStateOf("Inhale") }
    var breathSecondsRemaining by remember { mutableIntStateOf(60) }

    // Intercept hardware/gesture Back: Send user back to Home screen safely
    BackHandler {
        onGoHome()
    }

    // Breathing timer
    LaunchedEffect(isBreathingActive) {
        if (isBreathingActive) {
            while (breathSecondsRemaining > 0) {
                delay(1000L)
                breathSecondsRemaining--
                val cycle = (60 - breathSecondsRemaining) % 8
                breathingPhase = when (cycle) {
                    in 0..3 -> "Inhale slowly..."
                    4 -> "Hold..."
                    else -> "Exhale gently..."
                }
            }
            isBreathingActive = false
        }
    }

    // Pulse animation for gem & breathing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gem_pulse"
    )

    // Load real app icon or website favicon
    val iconBitmap = remember(isWebsite, websiteDomain, blockedPackage) {
        if (isWebsite && websiteDomain.isNotBlank()) {
            try {
                val cachedFile = File(context.filesDir, "favicons/${websiteDomain.replace('.', '_')}.png")
                if (cachedFile.exists()) {
                    BitmapFactory.decodeFile(cachedFile.absolutePath)?.asImageBitmap()
                } else null
            } catch (_: Exception) {
                null
            }
        } else {
            try {
                val pm = context.packageManager
                val iconDrawable: Drawable = pm.getApplicationIcon(blockedPackage)
                iconDrawable.toBitmap(128, 128).asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050607))
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .testTag("prime_blocked_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Prime Shield Header Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xDF14171E))
                    .border(1.dp, Color(0x3300E59A), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isWebsite) Icons.Default.Public else Icons.Default.Lock,
                    contentDescription = "Shield Active",
                    tint = OpalMint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isWebsite) "PRIME WEBSITE SHIELD" else "PRIME FOCUS SHIELD",
                    color = OpalMint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Prime Glowing Orb / Icon
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .scale(if (isBreathingActive) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x4000E59A),
                                Color(0x1500E59A),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(Color(0xDF12151B))
                        .border(1.5.dp, Color(0x8000E59A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = displayName,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    } else if (isWebsite) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Website Icon",
                            tint = OpalMint,
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.prime_logo),
                            contentDescription = "Prime Logo",
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Blocking Statement
            Text(
                text = if (isWebsite) "Prime blocked\n$displayName" else "Prime blocked\n$displayName",
                color = OpalTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isBreathingActive) {
                    breathingPhase
                } else if (isWebsite) {
                    "This website is on your blocked list ($categoryName).\nTake a breath and protect your focus."
                } else {
                    "You set a focus block to protect your time.\nTake a breath and stay intentional."
                },
                color = if (isBreathingActive) OpalMint else OpalTextSecondary,
                fontSize = if (isBreathingActive) 18.sp else 15.sp,
                fontWeight = if (isBreathingActive) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            if (isBreathingActive) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${breathSecondsRemaining}s remaining",
                    color = OpalTextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            // Primary Action: Leave App / Close Page
            Button(
                onClick = onGoHome,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("blocked_leave_app_button")
            ) {
                Text(
                    text = if (isWebsite) "Leave Website" else "Leave App",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary Action: 1-Minute Deep Breath
            OutlinedButton(
                onClick = {
                    isBreathingActive = !isBreathingActive
                    if (isBreathingActive) breathSecondsRemaining = 60
                },
                shape = RoundedCornerShape(28.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = OpalTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = OpalMint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBreathingActive) "Stop Breathing Guide" else "Take a 1-Min Mindful Breath",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Actions: Snooze 15m & Open Prime
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSnooze15Min() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = OpalTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Snooze 15m",
                        color = OpalTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "Adjust in Prime",
                    color = OpalMint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenPrime() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}
