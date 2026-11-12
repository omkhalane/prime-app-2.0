package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalSurfaceDark
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemReauthDialog(
    onDismiss: () -> Unit,
    onLaunchSystemPrompt: (() -> Unit)? = null,
    isSystemLockAvailable: Boolean = true
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OpalSurfaceDark,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.Lock, null, tint = OpalMint, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(16.dp))
            Text("Verify it's you", color = OpalTextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Use your phone's fingerprint, supported biometrics, PIN, pattern or password. PRIME never stores your device credentials.",
                color = OpalTextSecondary, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            if (isSystemLockAvailable && onLaunchSystemPrompt != null) {
                Button(
                    onClick = onLaunchSystemPrompt,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("launch_system_device_credentials_btn")
                ) {
                    Icon(Icons.Rounded.Fingerprint, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Authenticate with device", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    "A secure screen lock is not configured on this device. Set one in Android Settings before changing protected rules.",
                    color = Color(0xFFFF8A80), fontSize = 12.sp, textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel", color = OpalTextSecondary) }
        }
    }
}
