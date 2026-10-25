package com.urkinje.dualbudlistner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }

    @Composable
    fun MainScreen() {
        val context = LocalContext.current
        var bluetoothDevices by remember { mutableStateOf<List<AudioDeviceInfo>>(emptyList()) }
        var isCapturing by remember { mutableStateOf(false) }

        // Permission request launcher
        val requestPermissions = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                Toast.makeText(context, "Permissions granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Permissions denied. App won't work.", Toast.LENGTH_LONG).show()
            }
        }

        fun checkAndRequestPermissions() {
            val permissions = mutableListOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            requestPermissions.launch(permissions.toTypedArray())
        }

        fun scanDevices() {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            bluetoothDevices = devices.filter {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || 
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DualBud Listener",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = { checkAndRequestPermissions() },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text("1. Grant Permissions")
            }

            Button(
                onClick = { 
                    scanDevices()
                    Toast.makeText(context, "Scanned for devices", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text("2. Scan Connected Earbuds")
            }

            Text("Found Bluetooth Inputs:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start).padding(top = 16.dp, bottom = 8.dp)))

            if (bluetoothDevices.isEmpty()) {
                Text("No Bluetooth earbuds found. Make sure they are connected.", color = MaterialTheme.colors.error)
            } else {
                bluetoothDevices.forEach { device ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Name: ${device.productName}", style = MaterialTheme.typography.bodyLarge)
                            Text("Type: ${device.type}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val intent = Intent(context, AudioCaptureService::class.java)
                    if (!isCapturing) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        isCapturing = true
                    } else {
                        context.stopService(intent)
                        isCapturing = false
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCapturing) MaterialTheme.colors.error else MaterialTheme.colors.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isCapturing) "Stop Capture" else "3. Start Capture")
            }
        }
    }
}
