/**
 * MainViewModel.kt - 기능별 독립 샘플 메인 ViewModel
 * 
 * 이 파일은 LinkBand 애플리케이션의 메인 ViewModel로, 모든 샘플의 공통 기능을 관리합니다.
 * BleManager를 통해 블루투스 연결, 센서 데이터 수신, CSV 기록 등의 기능을 제공합니다.
 * 
 * 주요 기능:
 * - 블루투스 디바이스 스캔 및 연결 관리
 * - 센서 데이터 수신 및 상태 관리
 * - CSV 파일 기록 제어
 * - 자동 재연결 기능
 * - 연결 상태 및 데이터 상태 모니터링
 */
package com.example.demoappseparatefun.ui

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.linkbandsdk.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 메인 ViewModel - 모든 샘플의 공통 기능 관리
 * 
 * LinkBand 디바이스와의 모든 상호작용을 관리하는 중앙 ViewModel입니다.
 * BleManager를 통해 실제 블루투스 통신을 처리하고, UI에 필요한 상태를 제공합니다.
 * 
 * @param application Android Application 인스턴스
 */
class MainViewModel(application: android.app.Application) : AndroidViewModel(application) {
    // BleManager 인스턴스 - 실제 블루투스 통신 처리
    private val bleManager = BleManager(application)
    
    // ===== 공통 상태들 =====
    
    /**
     * 블루투스 연결 상태
     * true: 연결됨, false: 연결 해제됨
     */
    val isConnected: StateFlow<Boolean> = bleManager.isConnected
    
    /**
     * 연결된 디바이스 이름
     * 연결되지 않은 경우 null
     */
    val connectedDeviceName: StateFlow<String?> = bleManager.connectedDeviceName
    
    /**
     * 센서 데이터 수신 상태
     * true: 데이터 수신 중, false: 데이터 수신 안됨
     */
    val isReceivingData: StateFlow<Boolean> = bleManager.isReceivingData
    
    /**
     * 배터리 데이터
     * 디바이스에서 수신된 배터리 정보
     */
    val batteryData: StateFlow<BatteryData?> = bleManager.batteryData
    
    // ===== 블루투스 스캔 관련 상태 =====
    
    /**
     * 스캔된 디바이스 목록
     * 블루투스 스캔으로 발견된 디바이스들의 리스트
     */
    val scannedDevices: StateFlow<List<android.bluetooth.BluetoothDevice>> = bleManager.scannedDevices
    
    /**
     * 스캔 진행 상태
     * true: 스캔 중, false: 스캔 중지됨
     */
    val isScanning: StateFlow<Boolean> = bleManager.isScanning
    
    // ===== 센서 관련 상태 =====
    
    /**
     * 선택된 센서 목록
     * 사용자가 선택한 센서들의 Set (EEG, PPG, ACC)
     */
    val selectedSensors: StateFlow<Set<SensorType>> = bleManager.selectedSensors
    
    /**
     * EEG 데이터 리스트
     * 수신된 EEG 센서 데이터들의 리스트
     */
    val eegData: StateFlow<List<EegData>> = bleManager.eegData
    
    /**
     * PPG 데이터 리스트
     * 수신된 PPG 센서 데이터들의 리스트
     */
    val ppgData: StateFlow<List<PpgData>> = bleManager.ppgData
    
    /**
     * ACC 데이터 리스트
     * 수신된 ACC 센서 데이터들의 리스트
     */
    val accData: StateFlow<List<AccData>> = bleManager.accData
    
    // ===== CSV 기록 관련 상태 =====
    
    /**
     * CSV 기록 상태
     * true: 기록 중, false: 기록 중지됨
     */
    val isRecording: StateFlow<Boolean> = bleManager.isRecording
    
    // ===== 연결 관리 상태 =====
    
    /**
     * 자동 재연결 활성화 상태
     * true: 자동 재연결 활성화, false: 자동 재연결 비활성화
     */
    val isAutoReconnectEnabled: StateFlow<Boolean> = bleManager.isAutoReconnectEnabled
    
    // ===== 블루투스 스캔 기능 =====
    
    /**
     * 블루투스 디바이스 스캔 시작
     * 
     * 주변의 블루투스 디바이스를 검색합니다.
     * 스캔 결과는 scannedDevices StateFlow를 통해 UI에 전달됩니다.
     */
    fun startScan() {
        viewModelScope.launch {
            bleManager.startScan()
        }
    }
    
    /**
     * 블루투스 디바이스 스캔 중지
     * 
     * 진행 중인 블루투스 스캔을 중지합니다.
     * 배터리 절약을 위해 스캔이 완료되면 자동으로 호출됩니다.
     */
    fun stopScan() {
        viewModelScope.launch {
            bleManager.stopScan()
        }
    }
    
    // ===== 디바이스 연결 기능 =====
    
    /**
     * 특정 디바이스에 연결
     * 
     * @param device 연결할 블루투스 디바이스 객체
     * 
     * 선택된 디바이스와 블루투스 연결을 시도합니다.
     * 연결 성공 시 isConnected가 true로 변경됩니다.
     */
    fun connectToDevice(device: android.bluetooth.BluetoothDevice) {
        viewModelScope.launch {
            bleManager.connectToDevice(device)
        }
    }
    
    /**
     * 현재 연결된 디바이스 연결 해제
     * 
     * 현재 연결된 LinkBand 디바이스와의 연결을 해제합니다.
     * 연결 해제 시 isConnected가 false로 변경됩니다.
     */
    fun disconnect() {
        viewModelScope.launch {
            bleManager.disconnect()
        }
    }
    
    // ===== 자동 재연결 기능 =====
    
    /**
     * 자동 재연결 기능 활성화
     * 
     * 연결이 끊어졌을 때 자동으로 재연결을 시도하도록 설정합니다.
     * 네트워크 불안정이나 일시적인 연결 문제를 자동으로 해결합니다.
     */
    fun enableAutoReconnect() {
        viewModelScope.launch {
            bleManager.enableAutoReconnect()
        }
    }
    
    /**
     * 자동 재연결 기능 비활성화
     * 
     * 자동 재연결 기능을 비활성화합니다.
     * 연결이 끊어지면 수동으로 재연결해야 합니다.
     */
    fun disableAutoReconnect() {
        viewModelScope.launch {
            bleManager.disableAutoReconnect()
        }
    }
    
    // ===== 센서 선택 기능 =====
    
    /**
     * 센서 선택
     * 
     * @param sensor 선택할 센서 타입 (EEG, PPG, ACC)
     * 
     * 특정 센서를 활성화할 센서 목록에 추가합니다.
     * 선택된 센서는 selectedSensors StateFlow에 반영됩니다.
     */
    fun selectSensor(sensor: SensorType) {
        viewModelScope.launch {
            bleManager.selectSensor(sensor)
        }
    }
    
    /**
     * 센서 선택 해제
     * 
     * @param sensor 선택 해제할 센서 타입 (EEG, PPG, ACC)
     * 
     * 특정 센서를 활성화할 센서 목록에서 제거합니다.
     * 선택 해제된 센서는 selectedSensors StateFlow에서 제거됩니다.
     */
    fun deselectSensor(sensor: SensorType) {
        viewModelScope.launch {
            bleManager.deselectSensor(sensor)
        }
    }
    
    // ===== 센서 활성화/비활성화 =====
    
    /**
     * 선택된 센서들 활성화
     * 
     * selectedSensors에 포함된 모든 센서를 활성화하여 데이터 수신을 시작합니다.
     * 센서 활성화 시 isReceivingData가 true로 변경됩니다.
     */
    fun startSelectedSensors() {
        viewModelScope.launch {
            bleManager.startSelectedSensors()
        }
    }
    
    /**
     * 선택된 센서들 비활성화
     * 
     * 현재 활성화된 모든 센서를 비활성화하여 데이터 수신을 중지합니다.
     * 센서 비활성화 시 isReceivingData가 false로 변경됩니다.
     */
    fun stopSelectedSensors() {
        viewModelScope.launch {
            bleManager.stopSelectedSensors()
        }
    }
    
    // ===== CSV 기록 기능 =====
    
    /**
     * CSV 기록 시작
     * 
     * 현재 수신 중인 센서 데이터를 CSV 파일로 기록하기 시작합니다.
     * 기록 시작 시 isRecording이 true로 변경됩니다.
     * 파일은 Download/LinkBand 폴더에 저장됩니다.
     */
    fun startRecording() {
        viewModelScope.launch {
            bleManager.startRecording()
        }
    }
    
    /**
     * CSV 기록 중지
     * 
     * 진행 중인 CSV 기록을 중지하고 파일을 저장합니다.
     * 기록 중지 시 isRecording이 false로 변경됩니다.
     */
    fun stopRecording() {
        viewModelScope.launch {
            bleManager.stopRecording()
        }
    }
    
    // ===== 공통 유틸리티 함수들 =====
    
    /**
     * 연결 상태 초기화
     * 
     * 현재 연결을 해제하고 스캔을 중지하여 모든 상태를 초기화합니다.
     * 앱 재시작이나 오류 복구 시 사용됩니다.
     */
    fun resetConnection() {
        viewModelScope.launch {
            // 연결 상태 초기화
            bleManager.disconnect()
            bleManager.stopScan()
        }
    }
    
    /**
     * 연결 상태 문자열 반환
     * 
     * @return 현재 연결 상태를 나타내는 한국어 문자열
     * 
     * UI에서 연결 상태를 표시할 때 사용되는 유틸리티 함수입니다.
     */
    fun getConnectionStatus(): String {
        return when {
            isConnected.value -> "연결됨"
            isScanning.value -> "스캔 중"
            else -> "연결 해제됨"
        }
    }
    
    /**
     * 데이터 수신 상태 문자열 반환
     * 
     * @return 현재 데이터 수신 상태를 나타내는 한국어 문자열
     * 
     * UI에서 데이터 수신 상태를 표시할 때 사용되는 유틸리티 함수입니다.
     */
    fun getDataStatus(): String {
        return when {
            isReceivingData.value -> "데이터 수신 중"
            selectedSensors.value.isNotEmpty() -> "센서 선택됨"
            else -> "데이터 수신 안됨"
        }
    }
    
    /**
     * CSV 기록 상태 문자열 반환
     * 
     * @return 현재 CSV 기록 상태를 나타내는 한국어 문자열
     * 
     * UI에서 CSV 기록 상태를 표시할 때 사용되는 유틸리티 함수입니다.
     */
    fun getRecordingStatus(): String {
        return if (isRecording.value) "기록 중" else "기록 중지됨"
    }
} 