package com.hackathon.afterlog.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hackathon.afterlog.R

// 1920s Newspaper Font Families
val PlayfairDisplayFamily = FontFamily(
    Font(R.font.playfair_display_regular, FontWeight.Normal),
    Font(R.font.playfair_display_regular, FontWeight.Bold), // Using regular for bold fallback
    Font(R.font.playfair_display_regular, FontWeight.ExtraBold)
)

val LoraFamily = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_bold, FontWeight.Bold)
)

val SpecialEliteFamily = FontFamily(
    Font(R.font.special_elite, FontWeight.Normal)
)

// Newspaper-specific Typography Styles
object NewspaperTypography {
    // Masthead - "THE AFTERLOG"
    val masthead = TextStyle(
        fontFamily = PlayfairDisplayFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        letterSpacing = 3.sp
    )
    
    // Headline - Main story title (typewriter effect)
    val headline = TextStyle(
        fontFamily = PlayfairDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.5.sp
    )
    
    // Subheadline - Article summary
    val subheadline = TextStyle(
        fontFamily = LoraFamily,
        fontWeight = FontWeight.Normal,
        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
        fontSize = 18.sp,
        lineHeight = 24.sp
    )
    
    // Body - Article content
    val body = TextStyle(
        fontFamily = LoraFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
    
    // Timestamp - Time indicators
    val timestamp = TextStyle(
        fontFamily = SpecialEliteFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 1.sp
    )
    
    // Section Header - "TIMELINE OF EVENTS"
    val sectionHeader = TextStyle(
        fontFamily = PlayfairDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 2.sp
    )
    
    // Caption - Small text under images
    val caption = TextStyle(
        fontFamily = SpecialEliteFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp
    )
}

// Material 3 Default Typography
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlayfairDisplayFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 44.sp
    ),
    displayMedium = TextStyle(
        fontFamily = PlayfairDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PlayfairDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = LoraFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = LoraFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = LoraFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SpecialEliteFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SpecialEliteFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    )
)
