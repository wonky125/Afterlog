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
object NewspaperColors {
    // Paper Backgrounds
    val Parchment = Color(0xFFF5E6D3)       // Slightly warm cream
    val FreshPaper = Color(0xFFFAF7F2)      // Clean newsprint
    val AgedPaper = Color(0xFFEDE4D4)       // Light sepia tint
    
    // Ink Colors
    val InkBlack = Color(0xFF1A1A1A)        // Rich black ink
    val InkGray = Color(0xFF4A4A4A)         // Faded ink
    val HeadlineBlack = Color(0xFF000000)   // Pure black for headlines
    
    // Accent Colors
    val HeadlineRed = Color(0xFFAA2222)     // "EXTRA!" banner
    val MastheadGold = Color(0xFFB8860B)    // Dark goldenrod
    val VictorianPurple = Color(0xFF4A2040) // Noir accent
    
    // Divider Colors  
    val RuleLine = Color(0xFF2A2A2A)        // Thin black lines
    val LightRule = Color(0xFFCCCCCC)       // Subtle dividers
    
    // Speaker Colors (for timeline)
    val SpeakerA = Color(0xFF553311)        // Leather brown
    val SpeakerB = Color(0xFF224455)        // Deep blue-gray
    
    // Status Colors
    val SuccessGreen = Color(0xFF2D5A27)    // Vintage success
    val WarningAmber = Color(0xFFB8860B)    // Vintage amber
    val ErrorRed = Color(0xFF8B0000)        // Blood red
}

// Backwards compatibility alias
val Parchment = NewspaperColors.Parchment
