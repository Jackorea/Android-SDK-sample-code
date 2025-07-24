/**
 * MainActivity.kt - 기능별 독립 샘플 메인 액티비티
 * 
 * 이 파일은 LinkBand 애플리케이션의 메인 액티비티로, 앱의 진입점 역할을 합니다.
 * 블루투스 및 위치 권한 관리, 화면 전환 로직을 담당하며,
 * Jetpack Compose를 사용하여 UI를 구성합니다.
 * 
 * 주요 기능:
 * - 블루투스 및 위치 권한 요청 및 관리
 * - 스캐너 화면과 데이터 화면 간 전환
 * - MainViewModel 인스턴스 관리
 * - 권한이 없는 경우 권한 요청 UI 표시
 */
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

/**
 * 메인 액티비티
 * 
 * LinkBand 애플리케이션의 메인 액티비티로, 앱의 전체적인 생명주기를 관리합니다.
 * 블루투스 권한 관리와 화면 전환을 담당하며, MainViewModel을 통해
 * 블루투스 연결 및 센서 데이터 관리를 수행합니다.
 */
class MainActivity : ComponentActivity() {
    // MainViewModel 인스턴스 - 블루투스 연결 및 센서 데이터 관리
    private val viewModel: MainViewModel by viewModels()
    
    /**
     * 액티비티 생성 시 호출되는 메서드
     * 
     * 앱 초기화, 권한 요청, UI 설정을 수행합니다.
     * 
     * @param savedInstanceState 액티비티 상태 저장 데이터
     */
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Android 버전에 따른 필요한 권한 목록 설정
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 (API 31) 이상에서 필요한 권한
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,    // 블루투스 스캔 권한
                Manifest.permission.BLUETOOTH_CONNECT, // 블루투스 연결 권한
                Manifest.permission.ACCESS_FINE_LOCATION // 정확한 위치 권한
            )
        } else {
            // Android 11 이하에서 필요한 권한
            listOf(
                Manifest.permission.BLUETOOTH,         // 블루투스 권한
                Manifest.permission.BLUETOOTH_ADMIN,   // 블루투스 관리 권한
                Manifest.permission.ACCESS_FINE_LOCATION // 정확한 위치 권한
            )
        }
        
        // Jetpack Compose UI 설정
        setContent {
            // 다중 권한 상태 관리
            val permissionState = rememberMultiplePermissionsState(permissions)
            
            // 현재 화면 상태 관리 ("scanner" 또는 "data")
            var currentScreen by remember { mutableStateOf("scanner") }
            
            // 권한이 부여되지 않은 경우 자동으로 권한 요청
            LaunchedEffect(permissionState.allPermissionsGranted) {
                if (!permissionState.allPermissionsGranted) {
                    permissionState.launchMultiplePermissionRequest()
                }
            }
            
            // 권한이 모두 부여된 경우 메인 UI 표시
            if (permissionState.allPermissionsGranted) {
                // 현재 화면에 따라 적절한 화면 컴포넌트 표시
                when (currentScreen) {
                    "scanner" -> {
                        // 블루투스 스캐너 화면
                        // 디바이스 연결 시 자동으로 데이터 화면으로 전환
                        LinkBandScannerScreen(
                            viewModel = viewModel,
                            onDataScreenClick = { currentScreen = "data" }
                        )
                    }
                    "data" -> {
                        // 센서 데이터 표시 화면
                        // 연결 해제 시 스캐너 화면으로 돌아감
                        LinkBandDataScreen(
                            viewModel = viewModel,
                            onDisconnect = { currentScreen = "scanner" }
                        )
                    }
                    else -> {
                        // 기본값으로 스캐너 화면 표시
                        LinkBandScannerScreen(
                            viewModel = viewModel,
                            onDataScreenClick = { currentScreen = "data" }
                        )
                    }
                }
            } else {
                // 권한이 부여되지 않은 경우 권한 요청 UI 표시
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 권한 필요 안내 텍스트
                    Text(
                        text = "권한이 필요합니다",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 필요한 권한 설명
                    Text(
                        text = "블루투스 및 위치 권한을 허용해주세요",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 권한 요청 버튼
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