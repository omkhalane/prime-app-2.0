package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DigitalClockView(
    formattedTime: String,
    modifier: Modifier = Modifier
) {
    // Outer retro rounded bezel
    Box(
        modifier = modifier
            .width(280.dp)
            .height(130.dp)
            .shadow(16.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF323A42),
                        Color(0xFF1B2026),
                        Color(0xFF111417)
                    )
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF5A6673),
                        Color(0xFF222830)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner LCD screen
        Box(
            modifier = Modifier
                .width(256.dp)
                .height(106.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF7FA8BA),
                            Color(0xFF8EB6C8),
                            Color(0xFF7A9FA7)
                        )
                    )
                )
                .border(
                    width = 2.5.dp,
                    color = Color(0xFF182229),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Active LCD digits
            Text(
                text = formattedTime,
                color = Color(0xFF11161B),
                fontSize = 58.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        }
    }
}
