# 03. 백그라운드 수집 서비스 (Background Collection Service)

## 1. 목적 및 범위
- **목적**: 사용자가 다른 앱을 쓰거나 화면을 꺼도(일부 조건) 끊김 없이 게임 상황을 기록.
- **범위**: Foreground Service, CameraX (Preview-less), AudioRecord (dB Trigger).

## 2. 화면 구성 (테스트용)
- **ServiceControlScreen**:
  - `Start Service` / `Stop Service` 버튼.
  - 현재 상태 표시: "녹화 중 (xx장 촬영, 마지막 dB: 45)".
- **Overlay UI (Floating Button)**:
  - 다른 앱 위에 떠 있는 플로팅 버튼 (Overlay Permission 필요).
  - 기능: 게임 중 언제든 '하이라이트(비명)' 수동 마킹 또는 서비스 상태 확인.

## 3. 기술 요구사항
### Foreground Service
- **Notification**: "Afterlog is recording your session..." (English)
- **ServiceType**: `camera`, `microphone` (Android 14 대응).

### CameraX (Time-lapse)
- **Lifecycle**: `ProcessCameraProvider`를 서비스 수명주기에 바인딩 (LifecycleService 활용).
- **Strategy**: 
  - PreviewUseCase 없이 `ImageCapture` UseCase만 바인딩.
  - `takePicture`를 `Timer` 또는 `Coroutines`로 5초마다 호출.
  - 셔터 소음 제거 불가 시 `setSoundMode` 확인 또는 시스템 볼륨 제어 로직 검토.

### Audio Recording (Continuous + Trigger)
- **Strategy**: `MediaRecorder` with AAC/MPEG-4.
- **File**: Save full session audio to `session_audio_{id}.m4a`.
- **Scream Logic**:
  - Poll `mediaRecorder.maxAmplitude` every 100ms.
  - `20 * log10(maxAmplitude)` calculation.
  - > 80dB Trigger: Log `SCREAM_EVENT`.

### Video Recording (Rolling Buffer)
- **Goal**: Capture 2.5 minutes BEFORE and AFTER a scream event.
- **Strategy**: "Segmented Recording" (Chunking).
  - Record video (Video-Only, No Audio) in 30-second chunks: `temp_vid_1.mp4`, `temp_vid_2.mp4`...
  - Keep a sliding window of the last 6 files (3 minutes).
  - On `SCREAM_EVENT`: Mark current + previous 5 chunks as "Keep".
  - **Conflict**: Audio is recorded separately via `MediaRecorder` to avoid mic conflict.

### Time Synchronization (NTP)
- **Library**: `com.lyft.kronos:kronos-android`
- **Logic**: 
  - On App Start: Sync with NTP pool (time.google.com).
  - All timestamps (`Session`, `MediaLog`) must use `KronosClock.getCurrentTimeMs()`.
  
## 4. 모듈 인터페이스
- **Input**:
  - `StartCommand(sessionId)`: 서비스 시작 시 세션 ID 주입.
- **Output (to Repository)**:
  - `saveImage(file)`: 촬영된 사진 파일 저장.
  - `saveEvent(type, dB)`: 비명 감지 시 메타데이터 저장.
  - `saveAudio(file)`: 오디오 청크 저장.

## 5. 구현 우선순위 및 체크리스트
1. [x] **Service Skeleton**: `LifecyleService` 상속 및 Notification 채널 생성.
2. [x] **권한 요청 로직**: 런타임 권한(`CAMERA`, `RECORD_AUDIO`, `POST_NOTIFICATIONS`) 획득 로직.
3. [x] **Audio Meter**: `AudioRecord` 루프 스레드 구현 및 로그캣으로 dB 변화 확인.
4. [x] **Camera Logic**: 5초 간격 촬영 -> `Context.filesDir` 저장 확인.
5. [x] **DB 연결**: Repository 주입받아 `MediaLog` Insert 확인.
6. [ ] **NTP Sync**: Kronos 라이브러리 연동 및 타임스탬프 대체.
7. [ ] **Video Rolling Buffer**: 30초 단위 청크 녹화 및 순환 삭제 로직.
