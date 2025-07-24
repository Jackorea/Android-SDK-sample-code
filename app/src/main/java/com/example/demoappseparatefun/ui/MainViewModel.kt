// MainViewModel.kt - 기능별 독립 샘플 메인 ViewModel
package com.example.demoappseparatefun.ui

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.linkbandsdk.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// 메인 ViewModel - 모든 샘플의 공통 기능 관리
class MainViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val bleManager = BleManager(application)
    
    // 공통 상태들
    val isConnected: StateFlow<Boolean> = bleManager.isConnected
    val connectedDeviceName: StateFlow<String?> = bleManager.connectedDeviceName
    val isReceivingData: StateFlow<Boolean> = bleManager.isReceivingData
    val batteryData: StateFlow<BatteryData?> = bleManager.batteryData
    
    // 블루투스 스캔 관련
    val scannedDevices: StateFlow<List<android.bluetooth.BluetoothDevice>> = bleManager.scannedDevices
    val isScanning: StateFlow<Boolean> = bleManager.isScanning
    
    // 센서 관련
    val selectedSensors: StateFlow<Set<SensorType>> = bleManager.selectedSensors
    val eegData: StateFlow<List<EegData>> = bleManager.eegData
    val ppgData: StateFlow<List<PpgData>> = bleManager.ppgData
    val accData: StateFlow<List<AccData>> = bleManager.accData
    
    // CSV 기록 관련
    val isRecording: StateFlow<Boolean> = bleManager.isRecording
    
    // 연결 관리
    val isAutoReconnectEnabled: StateFlow<Boolean> = bleManager.isAutoReconnectEnabled
    
    // 블루투스 스캔 기능
    fun startScan() {
        viewModelScope.launch {
            bleManager.startScan()
        }
    }
    
    fun stopScan() {
        viewModelScope.launch {
            bleManager.stopScan()
        }
    }
    
    // 디바이스 연결 기능
    fun connectToDevice(device: android.bluetooth.BluetoothDevice) {
        viewModelScope.launch {
            bleManager.connectToDevice(device)
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            bleManager.disconnect()
        }
    }
    
    // 자동 재연결 기능
    fun enableAutoReconnect() {
        viewModelScope.launch {
            bleManager.enableAutoReconnect()
        }
    }
    
    fun disableAutoReconnect() {
        viewModelScope.launch {
            bleManager.disableAutoReconnect()
        }
    }
    
    // 센서 선택 기능
    fun selectSensor(sensor: SensorType) {
        viewModelScope.launch {
            bleManager.selectSensor(sensor)
        }
    }
    
    fun deselectSensor(sensor: SensorType) {
        viewModelScope.launch {
            bleManager.deselectSensor(sensor)
        }
    }
    
    // 센서 활성화/비활성화
    fun startSelectedSensors() {
        viewModelScope.launch {
            bleManager.startSelectedSensors()
        }
    }
    
    fun stopSelectedSensors() {
        viewModelScope.launch {
            bleManager.stopSelectedSensors()
        }
    }
    
    // CSV 기록 기능
    fun startRecording() {
        viewModelScope.launch {
            bleManager.startRecording()
        }
    }
    
    fun stopRecording() {
        viewModelScope.launch {
            bleManager.stopRecording()
        }
    }
    
    // 공통 유틸리티 함수들
    fun resetConnection() {
        viewModelScope.launch {
            // 연결 상태 초기화
            bleManager.disconnect()
            bleManager.stopScan()
        }
    }
    
    fun getConnectionStatus(): String {
        return when {
            isConnected.value -> "연결됨"
            isScanning.value -> "스캔 중"
            else -> "연결 해제됨"
        }
    }
    
    fun getDataStatus(): String {
        return when {
            isReceivingData.value -> "데이터 수신 중"
            selectedSensors.value.isNotEmpty() -> "센서 선택됨"
            else -> "데이터 수신 안됨"
        }
    }
    
    fun getRecordingStatus(): String {
        return if (isRecording.value) "기록 중" else "기록 중지됨"
    }
} 