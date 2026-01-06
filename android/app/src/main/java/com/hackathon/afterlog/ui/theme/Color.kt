package com.hackathon.afterlog.ui.theme

import androidx.compose.ui.graphics.Color

// Original Palette
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Afterlog Noir Palette
val BloodRed = Color(0xFF8B0000)
val DeepBlack = Color(0xFF0a0a0a)
val CharcoalGrey = Color(0xFF1a1a1a)
val BorderGrey = Color(0xFF2a2a2a)
val TextGray = Color(0xFF999999)

// 1920s Newspaper Palette
// 1920s Newspaper Palette (Legacy/Pivot)
object NewspaperColors {
    // ... existing colors ...
    val Parchment = Color(0xFFF5E6D3)
    val FreshPaper = Color(0xFFFAF7F2)
    val AgedPaper = Color(0xFFEDE4D4)
    val InkBlack = Color(0xFF1A1A1A)
    val InkGray = Color(0xFF4A4A4A)
    val HeadlineBlack = Color(0xFF000000)
    val HeadlineRed = Color(0xFFAA2222)
    val MastheadGold = Color(0xFFB8860B)
    val VictorianPurple = Color(0xFF4A2040)
    val RuleLine = Color(0xFF2A2A2A)
    val LightRule = Color(0xFFCCCCCC)
    val SpeakerA = Color(0xFF553311)
    val SpeakerB = Color(0xFF224455)
    val SuccessGreen = Color(0xFF2D5A27)
    val WarningAmber = Color(0xFFB8860B)
    val ErrorRed = Color(0xFF8B0000)
}

// Space Horror Terminal Palette
object SpaceTerminalColors {
    val Background = Color(0xFF050505)   // Near absolute black
    val Surface = Color(0xFF0D1117)      // Dark slate surface
    
    val PrimaryGreen = Color(0xFF39FF14) // Classic terminal green
    val SecondaryCyan = Color(0xFF00F3FF) // System cyan
    val WarningRed = Color(0xFFFF3131)   // Hazard red
    val AlertOrange = Color(0xFFFF9D00)  // Caution orange
    
    val TextMain = Color(0xFFD1D5DB)     // Light gray for body
    val TextDim = Color(0xFF6B7280)      // Muted gray
    
    val ScanlineColor = Color(0xFF000000).copy(alpha = 0.3f)
    val GlowGreen = Color(0xFF39FF14).copy(alpha = 0.15f)
}

// Backwards compatibility alias
val Parchment = NewspaperColors.Parchment
