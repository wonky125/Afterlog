# 05. 우주 기지 터미널 UI (Space Terminal UI)

## 1. 목적 및 범위
- **목적**: 분석된 로그 데이터를 "복구된 블랙박스 터미널" 컨셉으로 시각화하여 SF 호러 몰입도 증대.
- **범위**: Compose UI 레이아웃, CRT/Glitch 효과 구현, Monospace 폰트 적용.

## 2. 화면 구성 (SpaceTerminalView)
- **Container**: `Box` + `TerminalSurface` (CRTScanlines + Flicker Overlay).
- **Background**: `Color(0xFF050505)` (Void Black - 터미널 암전).
- **Header**:
  - `Text("SYSTEM STATUS: DECRYPTED", fontSize = 24.sp, fontFamily = Monospace)`
  - 기지명(AEGIS-7) 및 보안 레벨 표시.
- **Body**:
  - **Main Log**: 상단 분석 텍스트 (타이핑 효과 적용).
  - **Sequence Logs**: 시간순 데이터 로그. (좌측 타임스탬프 / 우측 분석 내용 / 위험도 색상).

## 3. 기술 요구사항
- **Coil**: 이미지 로딩 시 `Grayscale` 및 `Noise` 필터 적용 (CCTV 느낌).
- **Font**: Google Fonts (`JetBrains Mono`, `Share Tech Mono` 등 Monospace 권장).
- **Effect**:
  - `GlitchEffect`: 텍스트나 이미지가 지직거리는 애니메이션.
  - `TypingAnimation`: 텍스트가 한 글자씩 출력되는 레트로 터미널 효과.

## 4. 데이터 구조 (UiState)
```kotlin
data class TerminalUiState(
    val isDecryptionComplete: Boolean = false,
    val headline: String = "",
    val logs: List<TerminalLogItem> = emptyList(),
    val systemError: String? = null
)

data class TerminalLogItem(
    val timestamp: String,
    val visualFragmentPath: String?,
    val eventDescription: String,
    val threatLevel: ThreatLevel // LOW, MED, CRITICAL (Color Mapping)
)
```

## 5. 구현 우선순위 및 체크리스트
1. [x] **Mock Data 생성**: 실제 DB 없이 테스트 가능한 우주 기지 시나리오 데이터 생성.
2. [x] **Typography**: Monospace 폰트 정의 (`SpaceTerminalTypography`).
3. [x] **Layout Structure**: 헤더, 바디, 터미널 로그 리스트 구조 잡기.
4. [x] **Item Composable**: `LogSequenceEntry` (터미널 로그 줄) 및 `VisualFragment` (CCTV 카드) 구현.
5. [x] **Polish**: CRT 스캔라인 및 플리커 이펙트 (`TerminalSurface`) 구현.
