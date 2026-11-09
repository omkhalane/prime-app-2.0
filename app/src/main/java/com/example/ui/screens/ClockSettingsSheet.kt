package com.example.ui.screens

import android.app.Activity
import android.app.RingtoneManager
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.model.ClockPreferences

private data class ClockThemeOption(val id: String, val title: String, val colors: List<Color>)

private val alarmWallpapers = listOf(
    ClockThemeOption("aurora", "Aurora", listOf(Color(0xFF16142E), Color(0xFF553B82), Color(0xFF101827))),
    ClockThemeOption("sunrise", "Sunrise", listOf(Color(0xFF4D1F32), Color(0xFFB85C43), Color(0xFF24152E))),
    ClockThemeOption("forest", "Forest", listOf(Color(0xFF102D27), Color(0xFF28705A), Color(0xFF101D25))),
    ClockThemeOption("ocean", "Ocean", listOf(Color(0xFF10263D), Color(0xFF176A83), Color(0xFF101827))),
    ClockThemeOption("minimal", "Minimal", listOf(Color(0xFF252832), Color(0xFF414653), Color(0xFF14161C)))
)

private val timerThemes = listOf(
    ClockThemeOption("midnight", "Midnight", listOf(Color(0xFF101827), Color(0xFF34415D))),
    ClockThemeOption("ember", "Ember", listOf(Color(0xFF351C20), Color(0xFFB34B34))),
    ClockThemeOption("ocean", "Ocean", listOf(Color(0xFF102B3B), Color(0xFF1686A0))),
    ClockThemeOption("forest", "Forest", listOf(Color(0xFF15352B), Color(0xFF38856A))),
    ClockThemeOption("lavender", "Lavender", listOf(Color(0xFF2D2144), Color(0xFF8060B3)))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockSettingsSheet(
    preferences: ClockPreferences,
    onSave: (ClockPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var value by remember(preferences) { mutableStateOf(preferences) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val deviceTonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                value = value.copy(
                    defaultRingtoneId = "device_${uri.toString().hashCode()}",
                    defaultRingtoneName = ringtone?.getTitle(context) ?: "Device alarm sound",
                    defaultRingtoneUriString = uri.toString()
                )
            }
        }
    }

    val importTonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
            val name = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                ?: "Imported alarm audio"
            value = value.copy(
                defaultRingtoneId = "imported_${uri.toString().hashCode()}",
                defaultRingtoneName = name,
                defaultRingtoneUriString = uri.toString()
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF14171D),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Clock settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Your default sound, look and behaviour", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                }
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(22.dp).clickable(onClick = onDismiss))
            }

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MusicNote, null, tint = Color(0xFFB69AFF))
                Spacer(Modifier.size(8.dp))
                Text("Default alarm sound", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Text(value.defaultRingtoneName, color = Color(0xFFB7BBC5), fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp, bottom = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose default alarm sound")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    }
                    deviceTonePicker.launch(intent)
                }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF302B43))) { Text("Device tones", fontSize = 12.sp) }
                Button(onClick = { importTonePicker.launch(arrayOf("audio/*")) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF302B43))) { Text("Import audio", fontSize = 12.sp) }
            }

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Wallpaper, null, tint = Color(0xFFB69AFF))
                Spacer(Modifier.size(8.dp))
                Text("Alarm wallpaper", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                alarmWallpapers.forEach { theme ->
                    ThemeSwatch(theme, value.alarmWallpaper == theme.id) { value = value.copy(alarmWallpaper = theme.id) }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Timer colour theme", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                timerThemes.forEach { theme ->
                    ThemeSwatch(theme, value.timerTheme == theme.id) { value = value.copy(timerTheme = theme.id) }
                }
            }

            Spacer(Modifier.height(18.dp))
            SettingSwitch("Fade in alarm sound", value.alarmFadeIn) { value = value.copy(alarmFadeIn = it) }
            SettingSwitch("Vibrate by default", value.alarmVibration) { value = value.copy(alarmVibration = it) }
            SettingSwitch("Pomodoro transition sounds", value.sessionSoundsEnabled) { value = value.copy(sessionSoundsEnabled = it) }

            Text("Default alarm volume · ${value.alarmVolume}%", color = Color(0xFFD8DCE5), fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
            Slider(value = value.alarmVolume.toFloat(), onValueChange = { value = value.copy(alarmVolume = it.toInt().coerceIn(0, 100)) }, valueRange = 0f..100f)

            Button(
                onClick = { onSave(value) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B6CE0))
            ) {
                Icon(Icons.Rounded.Check, null)
                Spacer(Modifier.size(8.dp))
                Text("Save clock settings", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun ThemeSwatch(theme: ClockThemeOption, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).weight(1f)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(theme.colors))
                .then(if (selected) Modifier.border(2.dp, Color.White, RoundedCornerShape(12.dp)) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White)
        }
        Text(theme.title, color = if (selected) Color.White else Color(0xFF9CA3AF), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFFD8DCE5), fontSize = 13.sp)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
