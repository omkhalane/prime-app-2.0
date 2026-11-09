package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.AlarmSoundManager
import com.example.model.RingtoneOption
import com.example.ui.theme.OpalCardBg
import com.example.ui.theme.OpalCardBorder
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneSelectionSheet(
    selectedRingtoneId: String,
    selectedRingtoneUriString: String? = null,
    selectedRingtoneName: String? = null,
    onDismiss: () -> Unit,
    onSelectRingtone: (RingtoneOption) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val deviceRingtones = remember { AlarmSoundManager.getAvailableRingtones(context) }
    val ringtones = remember(deviceRingtones, selectedRingtoneId, selectedRingtoneUriString, selectedRingtoneName) {
        if (selectedRingtoneUriString != null && deviceRingtones.none { it.id == selectedRingtoneId }) {
            deviceRingtones + RingtoneOption(
                id = selectedRingtoneId,
                title = selectedRingtoneName ?: "Imported alarm sound",
                uriString = selectedRingtoneUriString,
                isSynthetic = false
            )
        } else deviceRingtones
    }
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }

    val importAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some document providers do not offer persistable grants.
            }
            val displayName = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Imported alarm sound"
            val option = RingtoneOption(
                id = "imported_${uri.toString().hashCode()}",
                title = displayName,
                uriString = uri.toString(),
                isSynthetic = false
            )
            onSelectRingtone(option)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AlarmSoundManager.stopPlayback(context)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            AlarmSoundManager.stopPlayback(context)
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color(0xFF16181C),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Ringtone",
                        tint = Color(0xFFB388FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Alarm Ringtone",
                        color = OpalTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = {
                    AlarmSoundManager.stopPlayback(context)
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = OpalTextSecondary
                    )
                }
            }

            Text(
                text = "Tap an item to select, tap play to preview sound",
                color = OpalTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF302342))
                        .clickable { importAudioLauncher.launch(arrayOf("audio/*")) }
                        .padding(horizontal = 12.dp, vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Import audio", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF20242B))
                        .clickable {
                            val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:alarm%20music"))
                            try {
                                context.startActivity(spotifyIntent)
                            } catch (_: Exception) {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/alarm%20music"))
                                )
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Explore Spotify", color = Color(0xFF1ED760), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            Text(
                text = "Imported audio is played from this device. Spotify opens for browsing; Spotify tracks cannot be assigned as a native alarm tone by this app.",
                color = OpalTextSecondary,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ringtones) { ringtone ->
                    val isSelected = ringtone.id == selectedRingtoneId
                    val isPlaying = ringtone.id == currentlyPlayingId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0xFF221733) else OpalCardBg)
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFFB388FF).copy(alpha = 0.6f) else OpalCardBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSelectRingtone(ringtone)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Checkmark circle if selected
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color(0xFFB388FF) else Color(0xFF22262B)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = ringtone.title,
                                    color = OpalTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = if (ringtone.isSynthetic) "Prime Harmonic Sound" else "Device Audio",
                                    color = OpalTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Preview Play/Stop button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying) Color(0xFFB388FF) else Color(0xFF262A30))
                                .clickable {
                                    if (isPlaying) {
                                        AlarmSoundManager.stopPlayback(context)
                                        currentlyPlayingId = null
                                    } else {
                                        currentlyPlayingId = ringtone.id
                                        AlarmSoundManager.previewRingtone(context, ringtone) {
                                            currentlyPlayingId = null
                                        }
                                    }
                                }
                                .testTag("preview_ringtone_${ringtone.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop Preview" else "Play Preview",
                                tint = if (isPlaying) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
