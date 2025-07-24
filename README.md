# LinkBand Demo App - 기능별 독립 샘플

LinkBand 디바이스와의 블루투스 연결, 센서 데이터 수신, CSV 기록 기능을 제공하는 Android 애플리케이션입니다.

## 📱 주요 기능

- **블루투스 디바이스 스캔 및 연결**
  - 주변 LinkBand 디바이스 자동 검색
  - 원클릭 연결/해제
  - 자동 재연결 기능

- **다중 센서 데이터 수신**
  - EEG (뇌파) 데이터: 250Hz 샘플링
  - PPG (맥파) 데이터: 50Hz 샘플링  
  - ACC (가속도계) 데이터: 25Hz 샘플링
  - 실시간 데이터 표시

- **CSV 파일 기록**
  - 실시간 센서 데이터 CSV 저장
  - 자동 파일 생성 및 관리
  - Download/LinkBand 폴더에 저장

- **권한 관리**
  - Android 버전별 블루투스 권한 자동 처리
  - 위치 권한 요청 및 관리

## 📁 프로젝트 구조

```
app/src/main/java/com/example/demoappseparatefun/
├── MainActivity.kt                    # 메인 액티비티
└── ui/
    ├── MainViewModel.kt               # ViewModel (비즈니스 로직)
    └── LinkBand-App.kt               # UI 컴포넌트들
```

## 📄 샘플 코드 파일

### 1. MainActivity.kt
**역할**: 앱의 진입점, 권한 관리, 화면 전환

**주요 기능**:
- 블루투스 및 위치 권한 요청
- 스캐너 화면 ↔ 데이터 화면 전환
- MainViewModel 인스턴스 관리

**핵심 코드**:
```kotlin
// Android 버전별 권한 설정
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
```

### 2. MainViewModel.kt
**역할**: 비즈니스 로직 관리, BleManager 통신

**주요 기능**:
- 블루투스 연결 상태 관리
- 센서 데이터 수신 및 처리
- CSV 기록 제어
- UI 상태 제공

**핵심 코드**:
```kotlin
class MainViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val bleManager = BleManager(application)
    
    // 상태 관리
    val isConnected: StateFlow<Boolean> = bleManager.isConnected
    val selectedSensors: StateFlow<Set<SensorType>> = bleManager.selectedSensors
    val eegData: StateFlow<List<EegData>> = bleManager.eegData
    
    // 블루투스 제어
    fun startScan() { viewModelScope.launch { bleManager.startScan() } }
    fun connectToDevice(device: BluetoothDevice) { ... }
    fun startSelectedSensors() { ... }
}
```

### 3. LinkBand-App.kt
**역할**: Jetpack Compose UI 컴포넌트

**주요 기능**:
- LinkBandScannerScreen: 디바이스 스캔 및 연결 UI
- LinkBandDataScreen: 센서 데이터 표시 및 제어 UI
- 공통 컴포넌트: DeviceItem, SensorDataCard, ReceivingIndicator

**핵심 코드**:
```kotlin
@Composable
fun LinkBandScannerScreen(
    viewModel: MainViewModel,
    onDataScreenClick: () -> Unit = {}
) {
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    
    // UI 구성...
}

@Composable
fun LinkBandDataScreen(
    viewModel: MainViewModel,
    onDisconnect: () -> Unit = {}
) {
    val eegData by viewModel.eegData.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    
    // 센서 데이터 표시 및 제어 UI...
}
```

## 🔧 LinkBand SDK 함수 설명

### BleManager 클래스

#### 블루투스 스캔 관련
```kotlin
// 스캔 시작
bleManager.startScan()

// 스캔 중지  
bleManager.stopScan()

// 스캔된 디바이스 목록
val scannedDevices: StateFlow<List<BluetoothDevice>>

// 스캔 상태
val isScanning: StateFlow<Boolean>
```

#### 디바이스 연결 관련
```kotlin
// 특정 디바이스 연결
bleManager.connectToDevice(device: BluetoothDevice)

// 연결 해제
bleManager.disconnect()

// 연결 상태
val isConnected: StateFlow<Boolean>

// 연결된 디바이스 이름
val connectedDeviceName: StateFlow<String?>
```

#### 자동 재연결 관련
```kotlin
// 자동 재연결 활성화
bleManager.enableAutoReconnect()

// 자동 재연결 비활성화
bleManager.disableAutoReconnect()

// 자동 재연결 상태
val isAutoReconnectEnabled: StateFlow<Boolean>
```

#### 센서 제어 관련
```kotlin
// 센서 선택/해제
bleManager.selectSensor(sensor: SensorType)
bleManager.deselectSensor(sensor: SensorType)

// 선택된 센서 목록
val selectedSensors: StateFlow<Set<SensorType>>

// 센서 활성화/비활성화
bleManager.startSelectedSensors()
bleManager.stopSelectedSensors()

// 데이터 수신 상태
val isReceivingData: StateFlow<Boolean>
```

#### 센서 데이터 관련
```kotlin
// EEG 데이터 (뇌파)
val eegData: StateFlow<List<EegData>>
// EegData 구조: timestamp, channel1, channel2, leadOff

// PPG 데이터 (맥파)
val ppgData: StateFlow<List<PpgData>>
// PpgData 구조: timestamp, red, ir

// ACC 데이터 (가속도계)
val accData: StateFlow<List<AccData>>
// AccData 구조: timestamp, x, y, z

// 배터리 데이터
val batteryData: StateFlow<BatteryData?>
// BatteryData 구조: level (0-100)
```

#### CSV 기록 관련
```kotlin
// CSV 기록 시작/중지
bleManager.startRecording()
bleManager.stopRecording()

// 기록 상태
val isRecording: StateFlow<Boolean>
```

## 🚀 사용 방법

### 1. 앱 실행
1. 앱을 실행하면 권한 요청 화면이 표시됩니다
2. "권한 요청" 버튼을 눌러 필요한 권한을 허용합니다

### 2. 디바이스 연결
1. 스캐너 화면에서 "스캔 시작" 버튼을 누릅니다
2. 발견된 LinkBand 디바이스 목록에서 원하는 디바이스를 선택합니다
3. "연결" 버튼을 눌러 디바이스에 연결합니다
4. 연결 성공 시 자동으로 데이터 화면으로 전환됩니다

### 3. 센서 데이터 수신
1. 데이터 화면에서 원하는 센서를 선택합니다 (EEG, PPG, ACC)
2. "센서 활성화" 버튼을 눌러 데이터 수신을 시작합니다
3. 실시간으로 수신되는 센서 데이터를 확인합니다

### 4. CSV 기록
1. 센서 데이터 수신 중에 "CSV 기록 시작" 버튼을 누릅니다
2. 데이터가 실시간으로 CSV 파일에 저장됩니다
3. "기록 중지" 버튼을 눌러 기록을 종료합니다

### 5. 연결 해제
1. "연결 해제" 버튼을 눌러 디바이스와의 연결을 해제합니다
2. 스캐너 화면으로 돌아갑니다

## 📋 권한 요구사항

### Android 12 (API 31) 이상
- `BLUETOOTH_SCAN`: 블루투스 디바이스 스캔
- `BLUETOOTH_CONNECT`: 블루투스 디바이스 연결
- `ACCESS_FINE_LOCATION`: 정확한 위치 정보 (블루투스 스캔에 필요)

### Android 11 이하
- `BLUETOOTH`: 블루투스 기능 사용
- `BLUETOOTH_ADMIN`: 블루투스 관리
- `ACCESS_FINE_LOCATION`: 정확한 위치 정보

## 🛠️ 빌드 및 실행

### 필수 요구사항
- Android Studio Arctic Fox 이상
- Android SDK API 21 이상
- LinkBand SDK 라이브러리

### 빌드 방법
1. 프로젝트를 Android Studio에서 엽니다
2. Gradle 동기화를 완료합니다
3. LinkBand SDK 의존성이 올바르게 설정되었는지 확인합니다
4. 앱을 빌드하고 실행합니다

### 실행 환경
- Android 5.0 (API 21) 이상
- 블루투스 4.0 이상 지원 기기
- LinkBand 디바이스

## 🛠️ 기술 스택

- **언어**: Kotlin
- **UI 프레임워크**: Jetpack Compose
- **아키텍처**: MVVM (Model-View-ViewModel)
- **상태 관리**: StateFlow, LiveData
- **비동기 처리**: Coroutines
- **권한 관리**: Accompanist Permissions
- **블루투스 통신**: LinkBand SDK

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 🤝 기여

버그 리포트, 기능 요청, 풀 리퀘스트를 환영합니다.

## 📞 지원

문제가 발생하거나 질문이 있으시면 이슈를 생성해 주세요.
