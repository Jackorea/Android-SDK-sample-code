// LinkBand-App.kt - LinkBand 통합 애플리케이션
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

// LinkBand 스캐너 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkBandScannerScreen(
    viewModel: MainViewModel,
    onDataScreenClick: () -> Unit = {}
) {
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
        // 디바이스 목록 및 연결 페이지
        Text(
            text = "LinkBand 블루투스 스캐너",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        // 자동 재연결 토글
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
        
        // 스캔 버튼
        Button(
            onClick = if (isScanning) viewModel::stopScan else viewModel::startScan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isScanning) "스캔 중지" else "스캔 시작")
        }
        
        // 스캔 상태
        if (isScanning) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Text("디바이스를 검색 중...")
            }
        }
        
        // 디바이스 목록
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
                
                if (scannedDevices.isEmpty()) {
                    Text(
                        text = "디바이스를 찾을 수 없습니다.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
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

// LinkBand 데이터 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkBandDataScreen(
    viewModel: MainViewModel,
    onDisconnect: () -> Unit = {}
) {
    val isConnected by viewModel.isConnected.collectAsState(initial = false)
    val selectedSensors by viewModel.selectedSensors.collectAsState(initial = emptySet())
    val isReceivingData by viewModel.isReceivingData.collectAsState(initial = false)
    val eegData by viewModel.eegData.collectAsState(initial = emptyList())
    val ppgData by viewModel.ppgData.collectAsState(initial = emptyList())
    val accData by viewModel.accData.collectAsState(initial = emptyList())
    val batteryData by viewModel.batteryData.collectAsState(initial = null)
    val isRecording by viewModel.isRecording.collectAsState(initial = false)
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState(initial = null)
    
    // 수집 시작 시점의 선택된 센서 스냅샷
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
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 연결 상태
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
            
            // 배터리 정보
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
            
            // 센서 선택 및 제어
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
                        
                        // 센서 선택 체크박스들
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
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
                                    if ((activationRequested || isReceivingData) && selectedSensors.contains(SensorType.EEG) && !startedSensors.contains(SensorType.EEG)) {
                                        ReceivingIndicator()
                                    }
                                }
                            }
                            
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
                        
                        // 센서 제어 버튼
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
            
            // EEG 데이터
            if (startedSensors.contains(SensorType.EEG)) {
                item {
                    SensorDataCard(
                        title = "EEG 데이터",
                        content = {
                            if (eegData.isNotEmpty()) {
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
            
            // PPG 데이터
            if (startedSensors.contains(SensorType.PPG)) {
                item {
                    SensorDataCard(
                        title = "PPG 데이터",
                        content = {
                            if (ppgData.isNotEmpty()) {
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
            
            // ACC 데이터
            if (startedSensors.contains(SensorType.ACC)) {
                item {
                    SensorDataCard(
                        title = "ACC 데이터",
                        content = {
                            if (accData.isNotEmpty()) {
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
            
            // CSV 기록 제어
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
                        
                        // 저장 경로
                        Text(
                            text = "저장 경로 : 내 파일 -> 내장 저장공간 -> Download -> LinkBand",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // 기록 상태
                        Text(
                            text = "기록 상태: ${viewModel.getRecordingStatus()}",
                            fontSize = 14.sp
                        )
                        
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

// 공통 컴포저블들
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

@Composable
fun ReceivingIndicator() {
    var dotCount by remember { mutableStateOf(1) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotCount = (dotCount % 3) + 1
        }
    }
    
    Text(
        text = "수신중" + ".".repeat(dotCount),
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
} 