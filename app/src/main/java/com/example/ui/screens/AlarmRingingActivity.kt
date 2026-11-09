package com.example.ui.screens

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.alarm.AlarmSchedulerHelper
import com.example.alarm.AlarmSoundManager
import com.example.model.AlarmMode
import com.example.model.ChallengeCategory
import com.example.model.ChallengeConfig
import com.example.model.ChallengeDifficulty
import com.example.ui.theme.OpalBlack
import com.example.ui.theme.OpalTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn screen on and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            km?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val alarmId = intent.getStringExtra(AlarmSchedulerHelper.EXTRA_ALARM_ID) ?: "default"
        val alarmName = intent.getStringExtra(AlarmSchedulerHelper.EXTRA_ALARM_NAME) ?: "Wake Up"
        val alarmModeStr = intent.getStringExtra(AlarmSchedulerHelper.EXTRA_ALARM_MODE) ?: "NORMAL"
        val challengeCatStr = intent.getStringExtra(AlarmSchedulerHelper.EXTRA_ALARM_CHALLENGE_CAT) ?: "MATH"
        val challengeDiffStr = intent.getStringExtra(AlarmSchedulerHelper.EXTRA_ALARM_CHALLENGE_DIFF) ?: "MEDIUM"
        val challengeCount = intent.getIntExtra(AlarmSchedulerHelper.EXTRA_ALARM_CHALLENGE_COUNT, 3)
        val snoozeMin = intent.getIntExtra(AlarmSchedulerHelper.EXTRA_ALARM_SNOOZE_MINUTES, 10)

        val alarmMode = try { AlarmMode.valueOf(alarmModeStr) } catch (_: Throwable) { AlarmMode.NORMAL }
        val challengeCat = try { ChallengeCategory.valueOf(challengeCatStr) } catch (_: Throwable) { ChallengeCategory.MATH }
        val challengeDiff = try { ChallengeDifficulty.valueOf(challengeDiffStr) } catch (_: Throwable) { ChallengeDifficulty.MEDIUM }

        val challengeConfig = ChallengeConfig(
            category = challengeCat,
            difficulty = challengeDiff,
            requiredConsecutive = challengeCount
        )

        setContent {
            OpalTheme {
                AlarmRingingScreen(
                    alarmId = alarmId,
                    alarmName = alarmName,
                    alarmMode = alarmMode,
                    challengeConfig = challengeConfig,
                    snoozeMinutes = snoozeMin,
                    onDismiss = {
                        AlarmSoundManager.stopPlayback(this)
                        finish()
                    },
                    onSnooze = {
                        AlarmSoundManager.stopPlayback(this)
                        AlarmSchedulerHelper.scheduleSnooze(this, alarmId, alarmName, snoozeMin)
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AlarmSoundManager.stopPlayback(this)
    }
}

@Composable
fun AlarmRingingScreen(
    alarmId: String,
    alarmName: String,
    alarmMode: AlarmMode,
    challengeConfig: ChallengeConfig,
    snoozeMinutes: Int,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isChallengeActive by remember { mutableStateOf(false) }
    var isSoundMuted by remember { mutableStateOf(false) }
    val alarmContext = LocalContext.current
    val wallpaperTheme = remember {
        alarmContext.getSharedPreferences("prime_focus_prefs", Context.MODE_PRIVATE)
            .getString("clock_alarm_wallpaper", "aurora") ?: "aurora"
    }
    val wallpaperColors = when (wallpaperTheme) {
        "sunrise" -> listOf(Color(0xFF4D1F32), Color(0xFFB85C43), Color(0xFF24152E))
        "forest" -> listOf(Color(0xFF102D27), Color(0xFF28705A), Color(0xFF101D25))
        "ocean" -> listOf(Color(0xFF10263D), Color(0xFF176A83), Color(0xFF101827))
        "minimal" -> listOf(Color(0xFF252832), Color(0xFF414653), Color(0xFF14161C))
        else -> listOf(Color(0xFF16142E), Color(0xFF553B82), Color(0xFF101827))
    }
    val wallpaperAccent = wallpaperColors[1]
    val wallpaperDrawable = when (wallpaperTheme) {
        "sunrise" -> R.drawable.img_preset_meeting
        "forest" -> R.drawable.img_preset_nature
        "ocean" -> R.drawable.img_preset_commute
        "minimal" -> R.drawable.img_preset_powernap
        else -> R.drawable.img_timer_hero
    }

    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val liveClock = remember(currentTimeMillis) {
        SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(currentTimeMillis))
    }
    val amPm = remember(currentTimeMillis) {
        SimpleDateFormat("a", Locale.getDefault()).format(Date(currentTimeMillis))
    }
    val liveDate = remember(currentTimeMillis) {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(currentTimeMillis))
    }

    // Animated pulsing aura
    val infiniteTransition = rememberInfiniteTransition(label = "ringing_aura")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    if (isChallengeActive) {
        ChallengeScreen(
            config = challengeConfig,
            isTestMode = false,
            onChallengeCompleted = onDismiss,
            onDismissTest = { isChallengeActive = false }
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Image(
                painter = painterResource(wallpaperDrawable),
                contentDescription = "Selected alarm wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(wallpaperColors.map { it.copy(alpha = 0.62f) })
                )
            )
            // Pulsing background ambient aura
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height * 0.4f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            wallpaperAccent.copy(alpha = 0.38f * pulseScale),
                            wallpaperColors[0].copy(alpha = 0.22f * pulseScale),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = size.width * 0.7f * pulseScale
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(wallpaperAccent.copy(alpha = 0.25f))
                            .border(2.dp, wallpaperAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Ringing",
                            tint = wallpaperColors[1].copy(alpha = 0.95f),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = alarmName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = liveDate,
                        color = Color(0xFFD1C4E9),
                        fontSize = 14.sp
                    )
                }

                // Center Clock
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = liveClock,
                            color = Color.White,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("ringing_clock")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = amPm,
                            color = Color(0xFFB388FF),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    if (alarmMode != AlarmMode.NORMAL) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFF5252).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Challenge Required to Dismiss",
                                color = Color(0xFFFF8A80),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Bottom Action Buttons: Snooze & Stop
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Snooze Button
                        Button(
                            onClick = onSnooze,
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF261D36),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("alarm_snooze_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = "Snooze",
                                tint = Color(0xFFD1C4E9),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Snooze (${snoozeMinutes}m)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Stop Button
                        Button(
                            onClick = {
                                if (alarmMode != AlarmMode.NORMAL) {
                                    isChallengeActive = true
                                } else {
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF5252),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("alarm_stop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (alarmMode != AlarmMode.NORMAL) "Solve & Stop" else "Stop",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF211A2D))
                            .clickable {
                                isSoundMuted = !isSoundMuted
                                AlarmSoundManager.setMuted(alarmContext, isSoundMuted)
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                            .testTag("alarm_mute_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSoundMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isSoundMuted) "Unmute alarm" else "Mute alarm",
                            tint = Color(0xFFD1C4E9),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSoundMuted) "Sound muted" else "Mute sound",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
