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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackathon.afterlog.service.AfterLogService

@Composable
fun HomeScreen(
    onNavigateToReport: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(checkPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
    }

    // Gemini Test State
    val testResult by viewModel.testResult.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AfterLog Debugger",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!hasPermissions) {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    )
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                        )
                    )
                }
            }) {
                Text("Grant Permissions")
            }
        } else {
            // Service Controls
            Button(onClick = {
                val intent = Intent(context, AfterLogService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Toast.makeText(context, "Service Started", Toast.LENGTH_SHORT).show()
            }) {
                Text("Start Background Service")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val intent = Intent(context, AfterLogService::class.java)
                    context.stopService(intent)
                    Toast.makeText(context, "Service Stopped", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Stop Service")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Simulation Controls
            Button(onClick = {
                val intent = Intent(context, AfterLogService::class.java).apply {
                    action = AfterLogService.ACTION_SIMULATE_SCREAM
                }
                context.startService(intent)
            }) {
                Text("DEBUG: Simulate Scream")
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Gemini Analysis Button
        Text("Gemini Integration", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onNavigateToReport("last_session") },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Analyze Game (Gemini)")
        }
        
        // Test Connection Button
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.testGeminiConnection() },
            enabled = !isTesting
        ) {
            if (isTesting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Test Connection")
            }
        }
        
        // Video Synthesis Test Button
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.testVideoSynthesis() },
            enabled = !isTesting,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            if (isTesting && testResult?.contains("Testing Video") == true) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onTertiary
                )
            } else {
                Text("🎬 Test Video Synthesis (Dummy)")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        
        // Real Pipeline Button
        Button(
            onClick = { viewModel.generateReplay("last_session") },
            enabled = !isTesting,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
             if (isTesting && testResult?.contains("Analyzing Session") == true) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text("🔍 Analyze Last Session (Full Pipeline)", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        
        testResult?.let { result ->
            if (result.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                if (result.contains("✅") || result.contains("Success")) {
                    Text(
                        text = "Video Created Successfully!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val filePath = result.substringAfter("files/").trim()
                    // Simple logic to extract path from log message if possible, 
                    // or just use the ViewModel to hold the last path.
                    // For now, let's assume ViewModel holds it or we parse it.
                    // To keep it simple for this MVP step:
                    
                    var showVideoPlayer by remember { mutableStateOf(false) }
                    
                    Button(onClick = { showVideoPlayer = true }) {
                        Text("▶️ Play Video")
                    }
                    
                    if (showVideoPlayer) {
                        val videoData = remember(result) {
                            try {
                                // Result format: "✅ Video created!\n/path/to/file.mp4"
                                val path = result.substringAfterLast("\n").trim()
                                val file = java.io.File(path)
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                Pair(uri, null)
                            } catch (e: Exception) {
                                Pair(null, e.localizedMessage)
                            }
                        }

                        if (videoData.first != null) {
                             VideoPlayerDialog(videoUri = videoData.first!!, onDismiss = { showVideoPlayer = false })
                        } else {
                             Text("Error loading video: ${videoData.second}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun VideoPlayerDialog(videoUri: android.net.Uri, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Video Preview") },
        text = {
            AndroidView(
                factory = { context ->
                    android.widget.VideoView(context).apply {
                        setVideoURI(videoUri)
                        start()
                        setOnCompletionListener { start() } // Loop
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
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
