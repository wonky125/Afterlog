package com.hackathon.afterlog.feature.guide

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.hackathon.afterlog.feature.home.HomeViewModel
import com.hackathon.afterlog.feature.home.PerspectiveGuideEditor
import com.hackathon.afterlog.service.AfterLogService
import com.hackathon.afterlog.ui.components.NoirButton
import com.hackathon.afterlog.ui.components.NoirIconButton
import com.hackathon.afterlog.ui.theme.NoirColors
import com.hackathon.afterlog.ui.theme.NoirTypography
import kotlinx.coroutines.launch

@Composable
fun GuideScreen(
    homeViewModel: HomeViewModel,
    guideViewModel: GuideViewModel,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val guide by homeViewModel.guideConfig.collectAsState()
    val lowPowerHint by homeViewModel.lowPowerHint.collectAsState()

    var recordingStarted by remember { mutableStateOf(false) }
    val recordingStartedState = rememberUpdatedState(recordingStarted)

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(previewView, lifecycleOwner) {
        guideViewModel.cameraUseCaseManager.setPreviewSurfaceProvider(previewView.surfaceProvider)
        guideViewModel.cameraUseCaseManager.bindToLifecycle(lifecycleOwner)
    }

    DisposableEffect(Unit) {
        onDispose {
            guideViewModel.cameraUseCaseManager.setPreviewSurfaceProvider(null)
            if (!recordingStartedState.value) {
                guideViewModel.cameraUseCaseManager.shutdown()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        PerspectiveGuideEditor(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
            config = guide,
            handleSize = 26.dp,
            isEditable = true,
            showBackground = false,
            showGrid = true,
            showFrame = false,
            lockAspectRatio = false,
            onGuideChanged = homeViewModel::setGuide
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .zIndex(2f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoirIconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PERSPECTIVE GUIDE",
                    style = NoirTypography.h2.copy(fontSize = 18.sp),
                    color = Color.White
                )
                Text(
                    text = "Drag the corners to match your board.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Surface(
                color = Color.White.copy(alpha = 0.08f),
                shape = MaterialTheme.shapes.small
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "LANDSCAPE UP ->",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Text(
                        text = "KEEP TOP EDGE RIGHT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            color = Color.Black.copy(alpha = 0.55f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = lowPowerHint,
                    style = NoirTypography.body,
                    color = NoirColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                NoirButton(
                    text = "CONFIRM",
                    onClick = {
                        recordingStarted = true
                        guideViewModel.cameraUseCaseManager.setPreviewSurfaceProvider(null)
                        guideViewModel.cameraUseCaseManager.shutdown()
                        homeViewModel.confirmLayout()
                        scope.launch {
                            try {
                                val sessionId = homeViewModel.createNewSession()
                                homeViewModel.persistGuide(sessionId)
                                val intent = Intent(context, AfterLogService::class.java).apply {
                                    putExtra(AfterLogService.EXTRA_SESSION_ID, sessionId)
                                    putExtra(
                                        AfterLogService.EXTRA_PERSPECTIVE_GUIDE,
                                        guide.toSerializedString()
                                    )
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                                Toast.makeText(context, "Archive Started", Toast.LENGTH_SHORT).show()
                                onConfirm()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to start session: ${e.message}", Toast.LENGTH_LONG).show()
                                recordingStarted = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
