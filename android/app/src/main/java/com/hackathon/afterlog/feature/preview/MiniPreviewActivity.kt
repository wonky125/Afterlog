package com.hackathon.afterlog.feature.preview

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hackathon.afterlog.data.model.PerspectiveGuideConfig
import com.hackathon.afterlog.data.repository.LocalRepository
import com.hackathon.afterlog.feature.home.PerspectiveGuideEditor
import com.hackathon.afterlog.service.AfterLogService
import com.hackathon.afterlog.service.CameraUseCaseManager
import com.hackathon.afterlog.ui.components.NoirButton
import com.hackathon.afterlog.ui.components.NoirSurface
import com.hackathon.afterlog.ui.theme.AfterLogTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MiniPreviewActivity : ComponentActivity() {

    @Inject
    lateinit var cameraUseCaseManager: CameraUseCaseManager

    @Inject
    lateinit var localRepository: LocalRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val guideJson = intent.getStringExtra(AfterLogService.EXTRA_PERSPECTIVE_GUIDE)
        val initialGuide = PerspectiveGuideConfig.fromSerializedString(guideJson)

        setContent {
            AfterLogTheme {
                val context = LocalContext.current
                var guide by remember { mutableStateOf(initialGuide ?: PerspectiveGuideConfig.default()) }

                LaunchedEffect(initialGuide) {
                    if (initialGuide == null) {
                        localRepository.getLastSavedPerspectiveGuide()?.let { guide = it }
                    }
                }

                val previewView = remember {
                    PreviewView(context).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                }

                DisposableEffect(previewView) {
                    cameraUseCaseManager.setPreviewSurfaceProvider(previewView.surfaceProvider)
                    onDispose {
                        cameraUseCaseManager.setPreviewSurfaceProvider(null)
                    }
                }

                NoirSurface {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f))
                    ) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(320.dp)
                                .height(240.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.9f)
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "Mini Layout Preview",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(12.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    AndroidView(
                                        factory = { previewView },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    PerspectiveGuideEditor(
                                        modifier = Modifier.fillMaxSize(),
                                        config = guide,
                                        handleSize = 12.dp,
                                        isEditable = false,
                                        showBackground = false,
                                        showGrid = false,
                                        showFrame = false,
                                        lockAspectRatio = false,
                                        onGuideChanged = {}
                                    )
                                }
                            }
                        }

                        NoirButton(
                            text = "CLOSE",
                            onClick = { (context as? Activity)?.finish() },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(24.dp)
                        )
                    }
                }
            }
        }
    }
}
