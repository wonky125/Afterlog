package com.example.afterlog.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.afterlog.service.AfterLogService
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.UUID

@Composable
fun HomeScreen(
    onNavigateToReport: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var isServiceRunning by remember { mutableStateOf(false) } // Simple state for demo

    // Permission Launcher
    val permissionsToRequest = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.values.all { it }
        if (!allGranted) {
            Toast.makeText(context, "Permissions required for recording", Toast.LENGTH_SHORT).show()
        }
    }

    // Check initial permissions
    LaunchedEffect(Unit) {
        val notGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    // Permission Dialog for "Denied Forever" case
    var showPermissionRationale by remember { mutableStateOf(false) }

    if (showPermissionRationale) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Permissions Required") },
            text = { Text("AfterLog needs Camera and Microphone permissions to record. Please grant them in Settings.") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showPermissionRationale = false
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Scream Detection Feedback
    var lastScreamDb by remember { mutableStateOf<Int?>(null) }
    
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.afterlog.SCREAM_DETECTED") {
                    val db = intent.getIntExtra("db", 0)
                    lastScreamDb = db
                    Toast.makeText(context, "🔊 Sound Detected: ${db}dB", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val filter = android.content.IntentFilter("com.example.afterlog.SCREAM_DETECTED")
        // Use receiver flag if needed (Tiramisu+ require export flag), but for local broad cast implicit is tricky. 
        // We'll use Context.registerReceiver with standard flags.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AfterLog Investigation",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        lastScreamDb?.let { db ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "🔊 Last Sound: ${db}dB",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // Check permissions again before starting
                val allGranted = permissionsToRequest.all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }

                if (allGranted) {
                    val intent = Intent(context, AfterLogService::class.java)
                    // Only generate new session ID if we are NOT already running
                    // (Though button is disabled if isServiceRunning is true, this handles edge cases)
                    // Start service without ID -> Service will generate new session and insert to DB
                    // if (!isServiceRunning) {
                    //    intent.putExtra(AfterLogService.EXTRA_SESSION_ID, UUID.randomUUID().toString())
                    // }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                    isServiceRunning = true
                    Toast.makeText(context, "Recording Started", Toast.LENGTH_SHORT).show()
                } else {
                    // If we are here, it means some permissions are missing.
                    // If the user previously checked "Don't ask again", the launcher will return immediately with denied.
                    // We should show a rationale dialog or guide to settings.
                    // Simple heuristic: If we launch request and it returns denied, we'll suggest settings.
                    // But here we are just about to launch.
                    
                    Toast.makeText(context, "Requesting permissions...", Toast.LENGTH_SHORT).show()
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    
                    // Note: We can't easily detect "Denied Forever" in pure Compose without Activity result callback analysis.
                    // So we add a fallback: if user clicks "Start" AGAIN and it's still denied, we show the dialog.
                    showPermissionRationale = true 
                }
            },
            enabled = !isServiceRunning
        ) {
            Text("Start Recording Session")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val intent = Intent(context, AfterLogService::class.java)
                context.stopService(intent)
                isServiceRunning = false
                Toast.makeText(context, "Recording Stopped", Toast.LENGTH_SHORT).show()
            },
            enabled = isServiceRunning,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Stop Recording")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val intent = Intent(context, AfterLogService::class.java).apply {
                    action = AfterLogService.ACTION_SIMULATE_SCREAM
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Toast.makeText(context, "Scream Simulated!", Toast.LENGTH_SHORT).show()
            },
            enabled = isServiceRunning,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("DEBUG: Simulate Scream")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNavigateToReport
        ) {
            Text("View Reports (Test)")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Gemini Connection Test Section
        val testResult by viewModel.testResult.collectAsState()
        val isTesting by viewModel.isTesting.collectAsState()

        Button(
            onClick = { viewModel.testGeminiConnection() },
            enabled = !isTesting,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text(if (isTesting) "Testing Gemini..." else "Test Gemini Connection")
        }

        testResult?.let { result ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = result,
                style = MaterialTheme.typography.bodySmall,
                color = if (result.contains("Success")) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
