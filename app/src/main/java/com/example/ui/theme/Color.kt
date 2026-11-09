package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// PRIME — Brand & Apple-Inspired Dark System
// ==========================================

// Base Backgrounds & Surfaces (Opal Pure Black Foundation)
val PrimeBackground = Color(0xFF000000)          // Pure black #000000 root
val PrimeBackgroundSecondary = Color(0xFF070707) // Restrained sub-black
val PrimeSurface = Color(0xFF111111)             // Primary surface #111111
val PrimeSurfaceElevated = Color(0xFF171717)     // Secondary surface #171717
val PrimeSurfaceHigher = Color(0xFF222222)       // Elevated surface #222222
val PrimeSurfaceNeutral = Color(0xFF3A3A3A)      // Neutral component surface #3A3A3A

// Restrained Surfaces & Minimal Borders
val PrimeGlassBackground = Color(0xEE111111)     // Solid-leaning 93% pure dark surface
val PrimeGlassCardBg = Color(0xFF171717)         // Solid secondary surface
val PrimeGlassBorder = Color(0x1FFFFFFF)         // Subtle 12% border
val PrimeGlassBorderHighlight = Color(0x2EFFFFFF)// Subtle highlight
val PrimeGlassOverlay = Color(0xCC000000)        // Pure black dim overlay
val PrimeSubtleBorder = Color(0x1FFFFFFF)        // 12% subtle white border

// Emerald & Mint Accents (PRIME Identity - used intentionally for active/identity states)
val PrimeAccent = Color(0xFF00E59A)              // Primary accent: Vibrant Emerald
val PrimeAccentSecondary = Color(0xFF78F0C0)     // Secondary accent: Mint
val PrimeAccentDark = Color(0xFF003822)          // Deep Emerald Container
val PrimeAccentGlow = Color(0x2600E59A)          // Restrained radial glow (15%)
val PrimeAccentGlowStrong = Color(0x4D00E59A)    // 30% glow

// Typography & Content Hierarchies (Pure White & Opal Neutral Greys)
val PrimeTextPrimary = Color(0xFFFFFFFF)         // Main text #FFFFFF
val PrimeTextSecondary = Color(0xFFBCBBC0)       // Secondary text #BCBBC0
val PrimeTextTertiary = Color(0xFF75757A)        // Supporting neutral grey
val PrimeTextQuaternary = Color(0xFF444448)

// Semantic Tokens
val PrimeSuccess = Color(0xFF36D98B)
val PrimeWarning = Color(0xFFFFB547)
val PrimeError = Color(0xFFFF5B66)
val PrimeInfo = Color(0xFF65A9FF)

// Restrained Gradients (Used only for special achievements, hero accents & focal points)
val PrimeEmeraldGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF00E59A), Color(0xFF78F0C0))
)
val PrimeEmeraldDarkGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF00E59A).copy(alpha = 0.18f), Color(0xFF000000))
)
val PrimeGlassGradient = Brush.verticalGradient(
    colors = listOf(Color(0x1AFFFFFF), Color(0x05FFFFFF))
)
val PrimeGlassCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF171717), Color(0xFF111111))
)
val PrimeFloatingNavGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFA111111), Color(0xFF000000))
)
val PrimeSurfaceBorderBrush = Brush.verticalGradient(
    colors = listOf(Color(0x26FFFFFF), Color(0x0AFFFFFF))
)

// Backward Compatibility Aliases (for seamless integration with existing code)
val OpalBlack = PrimeBackground
val OpalSurfaceDark = PrimeSurface
val OpalSurfaceVariant = PrimeSurfaceElevated
val OpalCardBg = PrimeSurfaceElevated
val OpalCardBorder = PrimeSubtleBorder
val OpalBorderSubtle = PrimeSubtleBorder

val OpalMint = PrimeAccent
val OpalMintLight = PrimeAccentSecondary
val OpalMintGlow = PrimeAccentSecondary
val OpalMintDark = PrimeAccentDark

val OpalFlame = PrimeWarning
val OpalFlameDark = Color(0xFFE65100)
val OpalDanger = PrimeError
val OpalTextPrimary = PrimeTextPrimary
val OpalTextSecondary = PrimeTextSecondary
val OpalTextTertiary = PrimeTextTertiary

// Brand Icon Colors
val BrandInstagram = Color(0xFFE1306C)
val BrandLinkedIn = Color(0xFF0077B5)
val BrandReddit = Color(0xFFFF4500)
val BrandYouTube = Color(0xFFFF0000)
val BrandTwitter = Color(0xFF1DA1F2)
val BrandPhone = Color(0xFF25D366)
val BrandWhatsApp = Color(0xFF25D366)
val BrandGPay = Color(0xFF4285F4)
val BrandTruecaller = Color(0xFF0088CC)
val BrandChrome = Color(0xFFFBBC05)
val BrandCalculator = Color(0xFF4CAF50)
