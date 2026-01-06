# 🎞️ Afterlog: 안드로이드 구현 가이드 (Space Horror Terminal)

"시네마틱 리플레이" 기능을 우주 기지 생존 호러 컨셉의 **"블랙박스 터미널 (Black Box Terminal)"**로 구현하기 위한 기술 가이드입니다.

---

## 1. 디자인 시스템 (Design Tokens)

우주 기지의 노후화된 터미널 시스템을 표현하기 위한 테마 정보입니다.

### 🎨 Color Palette (SpaceTerminalColors)
- **Primary Green (Terminal Text):** `#00FF41` (기본 텍스트, 시스템 정상 상태)
- **Secondary Cyan (Info/Header):** `#008F11` (보조 정보, 헤더)
- **Warning Red (Alert/Decibel):** `#FF3D00` (경고, 80dB 이상 비명, 중요 사건)
- **Void Black (Background):** `#050505` (CRT 모니터의 깊은 검정)

### ✍️ Typography (SpaceTerminalTypography)
- **System Status:** `Monospace`, `letterSpacing = 0.1.em`, `color = PrimaryGreen`
- **Log Body:** `Sans-Serif`, `fontSize = 14.sp`, `color = Color(0xFFCCCCCC)`
- **Glitch Title:** `Monospace`, `fontWeight = Bold`, `color = WarningRed` (글리치 효과 적용)

---

## 2. 핵심 UI 컴포넌트 구조

### ① SpaceTerminalView (Main Container)
전체 화면을 **CRT 모니터**처럼 연출합니다. 스캔라인(Scanlines) 오버레이와 미세한 화면 떨림(Flicker) 효과가 항상 적용됩니다.

### ② VisualFragment (Evidence Card)
노이즈가 낀 CCTV 스틸컷 혹은 손상된 데이터 조각입니다.
이미지 주변에 터미널 메타데이터(타임스탬프, 카메라 ID)가 텍스트로 표시됩니다.

### ③ ReportDetailScreen (Decrypted Log View) - [UPDATED]
기존의 신문 뷰를 대체하는 **해독된 보안 로그** 화면입니다.

```kotlin
@Composable
fun SpaceTerminalView(
    report: GeminiReport,
    onClose: () -> Unit
) {
    // CRT 터미널 효과 배경 적용
    TerminalSurface {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // [Header] 시스템 부팅 메시지 & 기지 상태
            item { SystemHeader("STATUS: CRITICAL FAILURE") }
            
            // [Main Log] AI의 사건 분석 텍스트
            item { TerminalLogText(report.article) }
            
            // [Sequence Logs] 타임라인 리스트
            items(report.timeline) { log ->
                 LogSequenceEntry(log) // 터미널 스타일 로그 엔트리
            }
            
            // [Footer] 연결 종료 메시지
            item { ConnectionFooter() }
        }
    }
}
```

---

## 3. 심사위원을 위한 'Wow Factor' 구현 팁

### 🎯 Tip 1: Glitch & Typing Effect (글리치 및 타이핑 효과)
텍스트가 한 번에 뜨지 않고, 타자기로 치듯 `T_y_p_i_n_g...` 효과와 함께 나타나게 합니다.
중요한 단어(RED)는 간헐적으로 지직거리는(Glitch) 애니메이션을 줍니다.

### 🎯 Tip 2: Audio Reactive (오디오 반응형)
음성 로그 재생 시, 파형(Visualizer)이나 모니터 테두리가 음량에 맞춰 반응하도록 하여 생동감을 줍니다.

### 🎯 Tip 3: "DATA CORRUPTED" 연출
AI가 확신하지 못하는 정보는 `"DATA SEGMENT MISSING..."` 혹은 `"ENCRYPTED"`로 표시하여 호러 분위기를 고조시킵니다.

### 🎯 Tip 4: 로그 추출 (Log Extraction) - [CORE FEATURE]
**Gemini 3 Pro**를 사용하여 우주 기지 내의 사건을 복원합니다.
- 예: "15:30:21 - 생명 반응 소실. [승무원 A]가 격리 구역을 이탈함."

---

## 4. 데이터 구조 (Gemini 연동용 JSON)

우주 기지 로그 형식의 JSON 구조입니다.

```json
{
  "headline": "LOG_ENTRY: HULL BREACH DETECTED",
  "summary": "Multiple hostiles detected in Sector 4.",
  "atmosphere": "Oxygen levels dropping. Panic undetectable in biometrics.",
  "article": "Station AI Analysis: Subject 'Leo' initiated manual override...",
  "timeline": [
    {
      "timestamp": "15:30:12",
      "event": "EVENT: AIRLOCK_OPEN",
      "description": "Unauthorized access detected.",
      "decibel": 92
    }
  ],
  "verdict": "CHANCE OF SURVIVAL: 0%"
}
```

---

## 5. [NEW] 페르소나 프롬프트 전략 (System Instructions)

**MOTH_ER (Station AI)** 페르소나를 적용하여 건조하고 기계적인 분석 톤을 유지합니다.

**System Instruction 예시:**
> 당신은 폐허가 된 우주 정거장 Aegis-7의 메인 AI 'MOTH_ER'입니다.
> 1. 제공된 **보안 카메라 자료(Vision)**와 **블랙박스 음성(Audio)**을 분석하여 사고 경위를 기록하십시오.
> 2. 감정적인 표현 대신 생체 신호(Heart Rate), 데시벨(dB), 환경 수치(Oxygen Level)로 상황을 묘사하십시오.
> 3. 비명이 감지된 구간은 "생체 위협 신호 감지(Bio-Threat Detected)"로 분류하십시오.
> 4. 생존자의 이름과 직책(추정)을 태그로 붙여주십시오.

---

## 6. 정확도 보완 기술 전략 (Accuracy Assurance)

1.  **키워드 트리거 (Keyword Trigger)**
    - "도망쳐", "죽었어", "뭐야 저게" 같은 패닉 키워드가 STT로 검출되면 `Warning Red` 로그로 강조합니다.

2.  **휴먼-인-더-루프 (Override Protocol)**
    - AI가 잘못 분석한 로그는 사용자가 터미널에서 `[OVERRIDE DATA]` 버튼을 눌러 직접 수정할 수 있게 합니다.

---

## 7. Gemini API 연동 포인트

### 요청 구조 (Request Payload)
1.  **System Prompt**: "You are MOTH_ER AI..."
2.  **Images**: CCTV 스타일로 흑백 처리하거나 노이즈가 추가된 스틸컷 권장.
3.  **Audio**: 전체 세션 녹음 파일.

### 응답 처리 (Response Handling)
Gemini가 생성한 JSON을 파싱하여 `SpaceTerminalView`에 렌더링하고, **MOTH_ER AI 보이스(여성형 기계음)**로 TTS를 재생합니다.
