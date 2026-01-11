package com.hackathon.afterlog.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.afterlog.ui.theme.NoirColors
import com.hackathon.afterlog.ui.theme.PlayfairDisplayFamily

/**
 * Standard Noir Surface with deep black background
 */
@Composable
fun NoirSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = NoirColors.DeepBlack,
        content = content
    )
}

/**
 * Noir Card with gradient background and subtle border
 */
@Composable
fun NoirCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val gradient = Brush.linearGradient(
        colors = listOf(NoirColors.CardStart, NoirColors.CardEnd)
    )
    
    val baseModifier = modifier
        .clip(shape)
        .border(1.dp, NoirColors.Border, shape)
        .background(gradient)
        .let {
            if (onClick != null) it.clickable(onClick = onClick) else it
        }
        .padding(16.dp)

    Column(
        modifier = baseModifier,
        content = content
    )
}

/**
 * Primary Action Button in Blood Red
 */
@Composable
fun NoirButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val isMultiLine = text.contains('\n')
    val baseStyle = TextStyle(
        fontFamily = PlayfairDisplayFamily, // Brand font
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp // Widely spaced
    )
    val textStyle = if (isMultiLine) {
        baseStyle.copy(fontSize = 14.sp, letterSpacing = 1.sp)
    } else {
        baseStyle
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp), // Comfortable touch target
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NoirColors.BloodRed,
            contentColor = Color.White,
            disabledContainerColor = NoirColors.CardStart,
            disabledContentColor = NoirColors.TextSecondary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = textStyle,
                maxLines = if (isMultiLine) 2 else 1
            )
        }
    }
}

/**
 * Section Header with decorative tracking and lines
 */
@Composable
fun NoirSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = title.uppercase(),
            style = TextStyle(
                color = Color.White,
                fontSize = 18.sp,
                letterSpacing = 3.sp, // Very wide tracking
                fontFamily = PlayfairDisplayFamily // Brand font
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = NoirColors.Border, thickness = 1.dp)
    }
}

/**
 * Label text for metadata (small, widely spaced, colored)
 */
@Composable
fun NoirLabel(
    text: String,
    color: Color = NoirColors.BloodRed
) {
    Text(
        text = text.uppercase(),
        style = TextStyle(
            color = color,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold
        )
    )
}

/**
 * Circular Icon Button
 */
@Composable
fun NoirIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .background(Color.White.copy(alpha = 0.05f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
    ) {
        content()
    }
}
