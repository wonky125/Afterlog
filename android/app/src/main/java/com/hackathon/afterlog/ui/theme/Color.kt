package com.hackathon.afterlog.ui.theme

import androidx.compose.ui.graphics.Color

// 1920s Lovecraftian Noir Palette (Target)
object NoirColors {
    val BloodRed = Color(0xFF8B0000)
    val BloodRedHover = Color(0xFFA00000) // Lighter for hover
    val DeepBlack = Color(0xFF0A0A0A)
    val CardStart = Color(0xFF1A1A1A)
    val CardEnd = Color(0xFF0F0F0F)
    val Border = Color(0xFF2A2A2A)
    val TextHeading = Color(0xFFFFFFFF)
    val TextBody = Color(0xFF999999)
    val TextSecondary = Color(0xFF666666)
}

// Legacy Palettes (To be removed after refactoring)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Direct reference aliases for backward compatibility during migration
val BloodRed = NoirColors.BloodRed
val DeepBlack = NoirColors.DeepBlack
val CharcoalGrey = NoirColors.CardStart
val BorderGrey = NoirColors.Border
val TextGray = NoirColors.TextBody

// 1920s Newspaper Palette (Keeping for Newspaper view)
object NewspaperColors {
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

// Space Horror Terminal Palette (DEPRECATED - REMOVE after refactor)
object SpaceTerminalColors {
    val Background = Color(0xFF050505)
    val Surface = Color(0xFF0D1117)
    
    val PrimaryGreen = Color(0xFF39FF14)
    val SecondaryCyan = Color(0xFF00F3FF)
    val WarningRed = Color(0xFFFF3131)
    val AlertOrange = Color(0xFFFF9D00)
    
    val TextMain = Color(0xFFD1D5DB)
    val TextDim = Color(0xFF6B7280)
    
    val ScanlineColor = Color(0xFF000000).copy(alpha = 0.3f)
    val GlowGreen = Color(0xFF39FF14).copy(alpha = 0.15f)
}

// Backwards compatibility alias
val Parchment = NewspaperColors.Parchment
