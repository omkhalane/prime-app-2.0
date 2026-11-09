package com.example.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.model.BlockedAppItem
import com.example.ui.theme.OpalBlack
import com.example.ui.theme.OpalCardBg
import com.example.ui.theme.OpalCardBorder
import com.example.ui.theme.OpalTextSecondary
import com.example.util.FaviconManager
import com.example.util.formatAppName
import java.io.File

@Composable
fun AppIconBadge(
    item: BlockedAppItem,
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val displayName = formatAppName(item.name)

    // Website: fetch & show real favicon, save to local storage, and use web logo as fallback only
    if (item.isWebsite) {
        val rawDomain = if (item.name.isNotBlank()) item.name else item.packageName
        val domain = FaviconManager.cleanDomain(rawDomain)

        var localFileExists by remember(domain) {
            mutableStateOf(FaviconManager.hasLocalFavicon(context, domain))
        }

        // Proactively download & cache favicon in local storage for future offline use
        LaunchedEffect(domain) {
            if (!localFileExists) {
                val saved = FaviconManager.saveFaviconToLocalStorage(context, domain)
                if (saved) {
                    localFileExists = true
                }
            }
        }

        val cachedFile = remember(domain, localFileExists) {
            FaviconManager.getCachedFaviconFile(context, domain)
        }

        val imageSource: Any = remember(domain, localFileExists) {
            if (localFileExists && cachedFile.exists() && cachedFile.length() > 0) {
                cachedFile
            } else {
                FaviconManager.getFaviconUrl(domain)
            }
        }

        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.28f))
                .background(Color(0xFF1E2228))
                .border(1.dp, OpalCardBorder, RoundedCornerShape(size * 0.28f))
                .testTag("website_favicon_badge_${domain}"),
            contentAlignment = Alignment.Center
        ) {
            coil.compose.SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageSource)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(size * 0.72f)
                    .clip(RoundedCornerShape(size * 0.2f)),
                // Fallback to web logo ONLY when favicon is missing or fails to load
                error = {
                    Box(
                        modifier = Modifier.size(size),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Web Logo Fallback",
                            tint = Color.White,
                            modifier = Modifier.size(size * 0.55f)
                        )
                    }
                },
                loading = {
                    Box(
                        modifier = Modifier.size(size),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = OpalTextSecondary,
                            modifier = Modifier.size(size * 0.5f)
                        )
                    }
                }
            )
        }
        return
    }

    // Try loading real system icon from PackageManager if package name is valid
    val realAppBitmap = remember(item.packageName) {
        if (item.packageName.isNotBlank()) {
            try {
                val pm = context.packageManager
                val iconDrawable: Drawable = pm.getApplicationIcon(item.packageName)
                iconDrawable.toBitmap(128, 128).asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    if (realAppBitmap != null) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.26f))
                .background(Color(0xFF1E2226)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = realAppBitmap,
                contentDescription = displayName,
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(size * 0.26f))
            )
        }
        return
    }

    // Fallback stylized icons
    when (item.name.lowercase()) {
        "instagram" -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(size * 0.28f))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF833AB4),
                                Color(0xFFFD1D1D),
                                Color(0xFFFCB045)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(size * 0.55f)
                        .border(width = (size * 0.06f), color = Color.White, shape = RoundedCornerShape(size * 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(size * 0.25f)
                            .border(width = (size * 0.05f), color = Color.White, shape = CircleShape)
                    )
                }
            }
        }
        "linkedin" -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color(0xFF0077B5)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "in",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.42f).sp
                )
            }
        }
        "reddit" -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color(0xFFFF4500)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "•‿•",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp
                )
            }
        }
        "x", "twitter" -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(1.dp, OpalCardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "𝕏",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.52f).sp
                )
            }
        }
        "youtube" -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color(0xFFFF0000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "YouTube",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.65f)
                )
            }
        }
        "phone" -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color(0xFF25D366)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Phone",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
        "calculator" -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Calculator",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        }
        else -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(size * 0.26f))
                    .background(Color(item.brandColorHex)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.45f).sp
                )
            }
        }
    }
}

@Composable
fun StackedAppIcons(
    items: List<BlockedAppItem>,
    maxVisible: Int = 3,
    iconSize: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val visibleItems = items.take(maxVisible)
    val remainingCount = items.size - visibleItems.size

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        visibleItems.forEachIndexed { index, app ->
            Box(
                modifier = Modifier
                    .offset(x = (-index * 6).dp)
                    .border(1.5.dp, OpalBlack, CircleShape)
            ) {
                AppIconBadge(item = app, size = iconSize)
            }
        }
        if (remainingCount > 0) {
            Box(
                modifier = Modifier
                    .offset(x = (-visibleItems.size * 6).dp)
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(OpalCardBg)
                    .border(1.dp, OpalCardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$remainingCount",
                    color = OpalTextSecondary,
                    fontSize = (iconSize.value * 0.45f).sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
