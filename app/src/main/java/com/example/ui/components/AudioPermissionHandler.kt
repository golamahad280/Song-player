package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AudioPermissionHandler(
    onPermissionGranted: () -> Unit
) {
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionState = rememberPermissionState(audioPermission)
    var showRationaleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(permissionState.status) {
        when (val status = permissionState.status) {
            is PermissionStatus.Granted -> {
                onPermissionGranted()
            }
            is PermissionStatus.Denied -> {
                if (status.shouldShowRationale) {
                    showRationaleDialog = true
                } else {
                    permissionState.launchPermissionRequest()
                }
            }
        }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text("Audio Library Access Required") },
            text = { 
                Text("Song Player needs access to your device's audio files to automatically scan and import your music library so you can enjoy your favorite tracks.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        permissionState.launchPermissionRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Grant Access")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRationaleDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
