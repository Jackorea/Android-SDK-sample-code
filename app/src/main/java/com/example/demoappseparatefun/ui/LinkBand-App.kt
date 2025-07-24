/**
 * LinkBand-App.kt - LinkBand 통합 애플리케이션 UI 컴포넌트
 * 
 * 이 파일은 LinkBand 디바이스와의 블루투스 연결, 센서 데이터 수신 및 표시,
 * CSV 기록 기능을 제공하는 Jetpack Compose UI 컴포넌트들을 포함합니다.
 * 
 * 주요 컴포넌트:
 * - LinkBandScannerScreen: 블루투스 디바이스 스캔 및 연결 화면
 * - LinkBandDataScreen: 센서 데이터 표시 및 제어 화면
 * - DeviceItem: 개별 디바이스 연결 관리 컴포넌트
 * - SensorDataCard: 센서 데이터 표시 카드 컴포넌트
 * - ReceivingIndicator: 데이터 수신 상태 표시 컴포넌트
 */
package com.example.demoappseparatefun.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.linkbandsdk.*
import com.example.demoappseparatefun.ui.MainViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import android.util.Log

/**
 * LinkBand 스캐너 화면
 * 
 * 블루투스 디바이스를 스캔하고 LinkBand 디바이스와 연결을 관리하는 화면입니다.
 * 
 * @param viewModel MainViewModel 인스턴스 - 블루투스 연결 및 디바이스 관리
 * @param onDataScreenClick 데이터 화면으로 이동하는 콜백 함수
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkBandScannerScreen(
    viewModel: MainViewModel,
    onDataScreenClick: () -> Unit = {}
) {
    // UI 상태 관리를 위한 StateFlow 값들
    val scannedDevices by viewModel.scannedDevices.collectAsState(initial = emptyList())
    val isScanning by viewModel.isScanning.collectAsState(initial = false)
    val isConnected by viewModel.isConnected.collectAsState(initial = false)
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState(initial = null)
    val isAutoReconnectEnabled by viewModel.isAutoReconnectEnabled.collectAsState(initial = false)
    
    // 연결 상태가 변경되면 자동으로 데이터 표시 페이지로 이동
    LaunchedEffect(isConnected) {
        if (isConnected) {
            onDataScreenClick()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 화면 제목
        Text(
            text = "LinkBand 블루투스 스캐너",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        // 자동 재연결 설정 카드
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "자동 재연결",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isAutoReconnectEnabled) 
                            "연결이 끊어지면 자동으로 재연결됩니다" 
                        else 
                            "수동으로 재연결해야 합니다",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // 자동 재연결 토글 스위치
                Switch(
                    checked = isAutoReconnectEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            viewModel.enableAutoReconnect()
                        } else {
                            viewModel.disableAutoReconnect()
                        }
                    }
                )
            }
        }
        
        // 블루투스 스캔 시작/중지 버튼
        Button(
            onClick = if (isScanning) viewModel::stopScan else viewModel::startScan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isScanning) "스캔 중지" else "스캔 시작")
        }
        
        // 스캔 상태 표시
        if (isScanning) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Text("디바이스를 검색 중...")
            }
        }
        
        // 발견된 디바이스 목록 카드
        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "발견된 디바이스 (${scannedDevices.size}개)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 디바이스가 없을 때 안내 메시지
                if (scannedDevices.isEmpty()) {
                    Text(
                        text = "디바이스를 찾을 수 없습니다.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // 디바이스 목록을 LazyColumn으로 표시
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(scannedDevices) { device ->
                            DeviceItem(
                                device = device,
                                isConnected = isConnected,
                                onConnect = { viewModel.connectToDevice(device) },
                                onDisconnect = { viewModel.disconnect() }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * LinkBand 데이터 화면
 * 
 * 연결된 LinkBand 디바이스로부터 수신된 센서 데이터를 표시하고
 * 센서 제어 및 CSV 기록 기능을 제공하는 화면입니다.
 * 
 * @param viewModel MainViewModel 인스턴스 - 센서 데이터 및 제어 관리
 * @param onDisconnect 연결 해제 시 호출되는 콜백 함수
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkBandDataScreen(
    viewModel: MainViewModel,
    onDisconnect: () -> Unit = {}
) {
    // UI 상태 관리를 위한 StateFlow 값들
    val isConnected by viewModel.isConnected.collectAsState(initial = false)
    val selectedSensors by viewModel.selectedSensors.collectAsState(initial = emptySet())
    val isReceivingData by viewModel.isReceivingData.collectAsState(initial = false)
    val eegData by viewModel.eegData.collectAsState(initial = emptyList())
    val ppgData by viewModel.ppgData.collectAsState(initial = emptyList())
    val accData by viewModel.accData.collectAsState(initial = emptyList())
    val batteryData by viewModel.batteryData.collectAsState(initial = null)
    val isRecording by viewModel.isRecording.collectAsState(initial = false)
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState(initial = null)
    
    // 수집 시작 시점의 선택된 센서 스냅샷 (UI 표시용)
    var startedSensors by remember { mutableStateOf<Set<SensorType>>(emptySet()) }
    
    // 센서 활성화 요청 상태 (버튼 클릭 시점)
    var activationRequested by remember { mutableStateOf(false) }
    
    // 수집 시작/중지 시점에 스냅샷 갱신
    LaunchedEffect(isReceivingData) {
        if (isReceivingData) {
            startedSensors = selectedSensors.toSet()
            activationRequested = false
        } else {
            startedSensors = emptySet()
            activationRequested = false
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 상단 앱바
        TopAppBar(
            title = {
                Text("LinkBand 데이터")
            }
        )
        
        // 메인 콘텐츠 영역
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 연결 상태 표시 카드
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConnected) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (isConnected && connectedDeviceName != null) {
                                "$connectedDeviceName ${viewModel.getConnectionStatus()}"
                            } else {
                                viewModel.getConnectionStatus()
                            },
                            fontWeight = FontWeight.Medium
                        )
                        if (isConnected) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "샘플링 레이트 \n EEG 250Hz \n PPG 50Hz \n ACC 25Hz",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // 배터리 정보 표시 카드
            batteryData?.let { battery ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                battery.level > 50 -> MaterialTheme.colorScheme.primaryContainer
                                battery.level > 20 -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "배터리",
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${battery.level}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
            
            // 센서 선택 및 제어 카드
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "센서 선택",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // 센서 선택 체크박스들 (EEG, PPG, ACC)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // EEG 센서 선택
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedSensors.contains(SensorType.EEG),
                                    onCheckedChange = { checked ->
                                        if (checked) viewModel.selectSensor(SensorType.EEG) 
                                        else viewModel.deselectSensor(SensorType.EEG)
                                    }
                                )
                                Column {
                                    Text("EEG")
                                    // 수신 중이지만 시작되지 않은 센서에 대한 표시
                                    if ((activationRequested || isReceivingData) && selectedSensors.contains(SensorType.EEG) && !startedSensors.contains(SensorType.EEG)) {
                                        ReceivingIndicator()
                                    }
                                }
                            }
                            
                            // PPG 센서 선택
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedSensors.contains(SensorType.PPG),
                                    onCheckedChange = { checked ->
                                        if (checked) viewModel.selectSensor(SensorType.PPG) 
                                        else viewModel.deselectSensor(SensorType.PPG)
                                    }
                                )
                                Column {
                                    Text("PPG")
                                    if ((activationRequested || isReceivingData) && selectedSensors.contains(SensorType.PPG) && !startedSensors.contains(SensorType.PPG)) {
                                        ReceivingIndicator()
                                    }
                                }
                            }
                            
                            // ACC 센서 선택
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedSensors.contains(SensorType.ACC),
                                    onCheckedChange = { checked ->
                                        if (checked) viewModel.selectSensor(SensorType.ACC) 
                                        else viewModel.deselectSensor(SensorType.ACC)
                                    }
                                )
                                Column {
                                    Text("ACC")
                                    if ((activationRequested || isReceivingData) && selectedSensors.contains(SensorType.ACC) && !startedSensors.contains(SensorType.ACC)) {
                                        ReceivingIndicator()
                                    }
                                }
                            }
                        }
                        
                        // 센서 활성화/비활성화 버튼
                        Button(
                            onClick = {
                                if (isReceivingData) {
                                    viewModel.stopSelectedSensors()
                                } else {
                                    activationRequested = true
                                    viewModel.startSelectedSensors()
                                }
                            },
                            enabled = selectedSensors.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isReceivingData) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isReceivingData) "센서 비활성화" else "센서 활성화")
                        }
                    }
                }
            }
            
            // EEG 데이터 표시 카드 (센서가 활성화된 경우에만)
            if (startedSensors.contains(SensorType.EEG)) {
                item {
                    SensorDataCard(
                        title = "EEG 데이터",
                        content = {
                            if (eegData.isNotEmpty()) {
                                // 최근 3개의 EEG 데이터 표시
                                val latest = eegData.takeLast(3)
                                latest.forEach { data ->
                                    Text(
                                        text = "timestamp: ${data.timestamp.time}, ch1uV: ${data.channel1.roundToInt()}µV, ch2uV: ${data.channel2.roundToInt()}µV, leadOff: ${if (data.leadOff) "1" else "0"}",
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = "EEG 데이터를 수신하지 못했습니다",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
            
            // PPG 데이터 표시 카드 (센서가 활성화된 경우에만)
            if (startedSensors.contains(SensorType.PPG)) {
                item {
                    SensorDataCard(
                        title = "PPG 데이터",
                        content = {
                            if (ppgData.isNotEmpty()) {
                                // 최근 3개의 PPG 데이터 표시
                                val latest = ppgData.takeLast(3)
                                latest.forEach { data ->
                                    Text(
                                        text = "timestamp: ${data.timestamp.time}, red: ${data.red}, ir: ${data.ir}",
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = "PPG 데이터를 수신하지 못했습니다",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
            
            // ACC 데이터 표시 카드 (센서가 활성화된 경우에만)
            if (startedSensors.contains(SensorType.ACC)) {
                item {
                    SensorDataCard(
                        title = "ACC 데이터",
                        content = {
                            if (accData.isNotEmpty()) {
                                // 최근 3개의 ACC 데이터 표시
                                val latest = accData.takeLast(3)
                                latest.forEach { data ->
                                    Text(
                                        text = "timestamp: ${data.timestamp.time}, x: ${data.x}, y: ${data.y}, z: ${data.z}",
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = "ACC 데이터를 수신하지 못했습니다",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
            
            // CSV 기록 제어 카드
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "CSV 기록 제어",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // CSV 기록 시작/중지 버튼
                        Button(
                            onClick = {
                                if (isRecording) {
                                    viewModel.stopRecording()
                                } else {
                                    viewModel.startRecording()
                                }
                            },
                            enabled = isConnected && isReceivingData,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) 
                                    MaterialTheme.colorScheme.error 
                                else if (isReceivingData)
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isRecording) "기록 중지" else "CSV 기록 시작")
                        }
                        
                        // CSV 파일 저장 경로 안내
                        Text(
                            text = "저장 경로 : 내 파일 -> 내장 저장공간 -> Download -> LinkBand",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // 기록 상태 표시
                        Text(
                            text = "기록 상태: ${viewModel.getRecordingStatus()}",
                            fontSize = 14.sp
                        )
                        
                        // 기록 중일 때 안내 메시지
                        if (isRecording) {
                            Text(
                                text = "데이터가 실시간으로 CSV 파일에 저장되고 있습니다",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // 연결 해제 버튼
            item {
                Button(
                    onClick = { 
                        viewModel.disconnect()
                        onDisconnect()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("연결 해제")
                }
            }
        }
    }
}

/**
 * 공통 컴포저블들
 */

/**
 * 디바이스 아이템 컴포넌트
 * 
 * 스캔된 블루투스 디바이스를 표시하고 연결/해제 기능을 제공합니다.
 * 
 * @param device 블루투스 디바이스 객체
 * @param isConnected 현재 연결 상태
 * @param onConnect 연결 버튼 클릭 시 호출되는 콜백
 * @param onDisconnect 연결 해제 버튼 클릭 시 호출되는 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceItem(
    device: BluetoothDevice,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 디바이스 정보 표시
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name ?: "알 수 없는 디바이스",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = device.address,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            
            // 연결/해제 버튼
            Button(
                onClick = if (isConnected) onDisconnect else onConnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(if (isConnected) "연결 해제" else "연결")
            }
        }
    }
}

/**
 * 센서 데이터 카드 컴포넌트
 * 
 * 센서 데이터를 표시하는 재사용 가능한 카드 컴포넌트입니다.
 * 
 * @param title 카드 제목
 * @param content 카드 내용을 구성하는 컴포저블
 */
@Composable
fun SensorDataCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            content()
        }
    }
}

/**
 * 데이터 수신 상태 표시 컴포넌트
 * 
 * 센서에서 데이터를 수신 중일 때 애니메이션 효과와 함께 표시됩니다.
 * "수신중..." 텍스트에 점이 0.5초마다 추가/제거되는 애니메이션을 제공합니다.
 */
@Composable
fun ReceivingIndicator() {
    // 점 개수 상태 관리
    var dotCount by remember { mutableStateOf(1) }
    
    // 애니메이션 효과를 위한 LaunchedEffect
    LaunchedEffect(Unit) {
        while (true) {
            delay(500) // 0.5초 대기
            dotCount = (dotCount % 3) + 1 // 1, 2, 3 순환
        }
    }
    
    Text(
        text = "수신중" + ".".repeat(dotCount),
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
} 