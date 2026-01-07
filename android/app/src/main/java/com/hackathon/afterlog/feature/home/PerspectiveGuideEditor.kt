package com.hackathon.afterlog.feature.home

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hackathon.afterlog.data.model.PerspectiveGuideConfig
import com.hackathon.afterlog.data.model.PerspectiveGuidePoint
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.roundToInt

@Composable
fun PerspectiveGuideEditor(
    modifier: Modifier = Modifier,
    config: PerspectiveGuideConfig,
    handleSize: Dp = 20.dp,
    isEditable: Boolean = true,
    onGuideChanged: (PerspectiveGuideConfig) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .border(1.dp, Color.White.copy(alpha = 0.3f))
    ) {
        val areaWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val areaHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val density = LocalDensity.current
        val handleSizePx = with(density) { handleSize.toPx() }
        val handleRadius = handleSizePx / 2f

        Canvas(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f))) {
            // Draw background grid
            val gridPaint = Color.Gray.copy(alpha = 0.3f)
            repeat(4) { index ->
                val posY = areaHeight * ((index + 1) / 5f)
                drawLine(gridPaint, Offset(0f, posY), Offset(areaWidth, posY), strokeWidth = 1f)
                val posX = areaWidth * ((index + 1) / 5f)
                drawLine(gridPaint, Offset(posX, 0f), Offset(posX, areaHeight), strokeWidth = 1f)
            }

            val lineColor = Color(0xFFCE3F80)
            val points = config.points.map {
                Offset(it.x * areaWidth, it.y * areaHeight)
            }
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(path, lineColor.copy(alpha = 0.55f))
            drawPath(path, Color.White.copy(alpha = 0.85f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
        }

        config.points.forEachIndexed { index, point ->
            val offsetX = (point.x * areaWidth).roundToInt()
            val offsetY = (point.y * areaHeight).roundToInt()

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (offsetX - handleRadius).roundToInt(),
                            (offsetY - handleRadius).roundToInt()
                        )
                    }
                    .size(handleSize)
                    .background(Color.White, CircleShape)
                    .then(
                        if (isEditable) {
                            Modifier.pointerInput(index) {
                                detectDragGestures { change, dragAmount ->
                                    val deltaX = dragAmount.x / areaWidth
                                    val deltaY = dragAmount.y / areaHeight
                                    val updatedPoint = PerspectiveGuidePoint(
                                        (point.x + deltaX).coerceIn(0f, 1f),
                                        (point.y + deltaY).coerceIn(0f, 1f)
                                    )
                                    onGuideChanged(config.withUpdatedPoint(index, updatedPoint))
                                    change.consume()
                                }
                            }
                        } else Modifier
                    )
            )
        }
    }
}

@Composable
fun LayoutPreviewDialog(
    guide: PerspectiveGuideConfig,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color.Black,
            tonalElevation = 8.dp,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Mini Layout Preview",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                PerspectiveGuideEditor(
                    config = guide,
                    handleSize = 16.dp,
                    isEditable = false,
                    onGuideChanged = {}
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun AutoLevelIndicator(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var angle by remember { mutableStateOf(0f) }
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values.getOrNull(0) ?: 0f
                val z = event.values.getOrNull(2) ?: 0f
                angle = atan2(x.toDouble(), z.toDouble()).toFloat()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val degrees = (angle * (180f / PI.toFloat())).coerceIn(-45f, 45f)
    val normalized = (degrees + 45f) / 90f

    Column(
        modifier = modifier
            .border(1.dp, Color.White.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Text(
            text = "Auto Level ${"%.1f".format(degrees)}°",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = normalized,
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF69F0AE),
            trackColor = Color.White.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (degrees.absoluteValue < 2f) "Balanced" else "Adjust tilt slightly",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
