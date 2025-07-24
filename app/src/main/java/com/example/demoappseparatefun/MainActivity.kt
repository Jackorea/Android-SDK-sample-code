// MainActivity.kt - 기능별 독립 샘플 메인 액티비티
package com.example.demoappseparatefun

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.example.demoappseparatefun.ui.*
import com.example.demoappseparatefun.ui.MainViewModel

// 메인 액티비티
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        
        setContent {
            val permissionState = rememberMultiplePermissionsState(permissions)
            var currentScreen by remember { mutableStateOf("scanner") }
            
            LaunchedEffect(permissionState.allPermissionsGranted) {
                if (!permissionState.allPermissionsGranted) {
                    permissionState.launchMultiplePermissionRequest()
                }
            }
            
            if (permissionState.allPermissionsGranted) {
                when (currentScreen) {
                    "scanner" -> LinkBandScannerScreen(
                        viewModel = viewModel,
                        onDataScreenClick = { currentScreen = "data" }
                    )
                    "data" -> LinkBandDataScreen(
                        viewModel = viewModel,
                        onDisconnect = { currentScreen = "scanner" }
                    )
                    else -> LinkBandScannerScreen(
                        viewModel = viewModel,
                        onDataScreenClick = { currentScreen = "data" }
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "권한이 필요합니다",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "블루투스 및 위치 권한을 허용해주세요",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { permissionState.launchMultiplePermissionRequest() }
                    ) {
                        Text("권한 요청")
                    }
                }
            }
        }
    }
} 