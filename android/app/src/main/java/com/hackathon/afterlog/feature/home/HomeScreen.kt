package com.hackathon.afterlog.feature.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackathon.afterlog.service.AfterLogService
import com.hackathon.afterlog.ui.components.*
import com.hackathon.afterlog.ui.theme.NoirColors
import com.hackathon.afterlog.ui.theme.NoirTypography
import com.hackathon.afterlog.ui.theme.PlayfairDisplayFamily

@Composable
fun HomeScreen(
    onNavigateToReport: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasPermissions by remember { mutableStateOf(checkPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
    }

    val testResult by viewModel.testResult.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

    NoirSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AFTERLOG",
                        style = TextStyle(
                            fontFamily = PlayfairDisplayFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            letterSpacing = 6.sp,
                            color = NoirColors.BloodRed
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "INVESTIGATIVE ARCHIVE SYSTEM",
                        style = TextStyle(
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            color = NoirColors.TextSecondary
                        )
                    )
                }
                NoirIconButton(onClick = { /* TODO: Settings */ }) {
                     Text("⚙️", fontSize = 16.sp)
                }
            }

            if (!hasPermissions) {
                NoirCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "PERMISSION REQUIRED",
                        style = NoirTypography.h2,
                        color = NoirColors.BloodRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The archive system requires access to local sensors to monitor anomalies.",
                        style = NoirTypography.body,
                        color = NoirColors.TextBody
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    NoirButton(
                        text = "GRANT ACCESS",
                        onClick = {
                            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            }
                            permissionLauncher.launch(permissions)
                        }
                    )
                }
            } else {
                val guide by viewModel.guideConfig.collectAsState()
                val lowPowerHint by viewModel.lowPowerHint.collectAsState()
                var showPreview by remember { mutableStateOf(false) }

                NoirSectionHeader("PERSPECTIVE GUIDE")
                NoirCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Align the four corners to help Afterlog observe your board edges.",
                            style = NoirTypography.body,
                            color = NoirColors.TextBody
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PerspectiveGuideEditor(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            config = guide,
                            onGuideChanged = viewModel::setGuide
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NoirButton(
                                text = "CONFIRM LAYOUT",
                                onClick = viewModel::confirmLayout,
                                modifier = Modifier.weight(1f)
                            )
                            NoirButton(
                                text = "OPEN MINI PREVIEW",
                                onClick = { showPreview = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        AutoLevelIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = lowPowerHint,
                            style = NoirTypography.caption,
                            color = NoirColors.TextSecondary
                        )
                    }
                }

                if (showPreview) {
                    LayoutPreviewDialog(
                        guide = guide,
                        onDismiss = { showPreview = false }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                NoirSectionHeader("MONITORING PROTOCOLS")
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NoirButton(
                        text = "START ARCHIVE",
                        onClick = {
                            val layoutGuide = guide
                            viewModel.confirmLayout()
                            scope.launch {
                                try {
                                    val sessionId = viewModel.createNewSession()
                                    viewModel.persistGuide(sessionId)
                                    val intent = Intent(context, AfterLogService::class.java).apply {
                                        putExtra(AfterLogService.EXTRA_SESSION_ID, sessionId)
                                        putExtra(
                                            AfterLogService.EXTRA_PERSPECTIVE_GUIDE,
                                            layoutGuide.toSerializedString()
                                        )
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                    Toast.makeText(context, "Archive Started", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to start session: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NoirButton(
                        text = "STOP ARCHIVE",
                        onClick = {
                            val intent = Intent(context, AfterLogService::class.java)
                            context.stopService(intent)
                            Toast.makeText(context, "Archive Stopped", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                NoirButton(
                    text = "SIMULATE ANOMALY",
                    onClick = {
                       val intent = Intent(context, AfterLogService::class.java).apply {
                            action = AfterLogService.ACTION_SIMULATE_SCREAM
                        }
                        context.startService(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                NoirSectionHeader("CASE ARCHIVES")

                NoirButton(
                    text = "OPEN LATEST CASE",
                    onClick = { onNavigateToReport("last_session") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                NoirButton(
                    text = if (isTesting) "CONNECTING..." else "TEST GEMINI UPLINK",
                    onClick = { viewModel.testGeminiConnection() },
                    enabled = !isTesting,
                    modifier = Modifier.fillMaxWidth()
                )

                // Test Result Display
                testResult?.let { result ->
                    if (result.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        NoirCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "SYSTEM RESPONSE",
                                style = NoirTypography.subtitle,
                                color = NoirColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result,
                                style = NoirTypography.body,
                                color = if (result.contains("Success")) NoirColors.BloodRed else NoirColors.TextBody
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "CASE FILE #1923 // MISKATONIC ARCHIVE SYSTEM",
                style = TextStyle(fontSize = 10.sp, letterSpacing = 1.sp, color = NoirColors.TextSecondary),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
            )
        }
    }
}

private fun checkPermissions(context: Context): Boolean {
    val camera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
    val audio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
    val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        PackageManager.PERMISSION_GRANTED
    }
    return camera == PackageManager.PERMISSION_GRANTED && 
           audio == PackageManager.PERMISSION_GRANTED &&
           notifications == PackageManager.PERMISSION_GRANTED
}
