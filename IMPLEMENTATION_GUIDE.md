# 🎞️ Afterlog: 안드로이드 구현 가이드 (Cinematic Report)

제공해주신 고퀄리티 목업을 바탕으로, 안드로이드 Jetpack Compose에서 이 "시네마틱 보고서" 기능을 완벽하게 구현하기 위한 기술 가이드입니다.

---

## 1. 디자인 시스템 (Design Tokens)

목업의 Tailwind 설정을 Compose 코드로 변환한 핵심 테마 정보입니다.

### 🎨 Color Palette
- **Blood Red (Accent):** `#8B0000` (중요 버튼, 헤더, 강조 텍스트)
- **Deep Black (Background):** `#0a0a0a` (메인 배경)
- **Charcoal Grey (Card):** `#1a1a1a` (카드 내부 배경)
- **Border Grey:** `#2a2a2a` (구분선 및 테두리)

### ✍️ Typography (Compose)
- **Main Heading:** `letterSpacing = 0.25.em`, `color = Color(0xFF8B0000)`
- **Body Text:** `fontSize = 14.sp`, `color = Color(0xFF999999)`
- **Label/Classified:** `letterSpacing = 0.3.em`, `fontSize = 10.sp`, `color = Color(0xFF8B0000)`

---

## 2. 핵심 UI 컴포넌트 구조

### ① CinematicReportScreen (Main Container)
전체 화면을 구성하며 상단에 영상을 고정(또는 스크롤)하고 하단에 신문 스타일의 리스트를 배치합니다.

### ② EvidenceCard (Timeline Item)
가장 중요한 "신문 조각" 느낌의 커스텀 컴포넌트입니다.
이미지를 클릭하면 상세 조사 보고서(`NewspaperView`)가 열리도록 구현합니다.

### ③ NewspaperView (Detailed Investigation Report) - [NEW]
목업의 '신문' 스타일을 구현하며, 특히 **Investigation Log** 섹션이 핵심입니다.

```kotlin
@Composable
fun NewspaperView(
    caseData: CaseData,
    gameLogs: List<GameEvent>, // 추출된 게임 로그 데이터
    onClose: () -> void
) {
    // 1920년대 신문 스타일의 배경 컬러 (#F5E6D3) 및 서체 적용
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5E6D3)) // 양피지 색상
            .verticalScroll(rememberScrollState())
    ) {
        // [Report Header] 아컴 크로니클 스타일
        NewspaperHeader(caseData.date)
        
        // [Main Article] 사건 요약 및 사진
        ArticleSection(caseData)
        
        // [INVESTIGATION LOG Section] - 게임 로그 리스트
        InvestigationLogSection(gameLogs)
        
        // [Footer] APD 기밀 문서 경고 문구
    }
}
```

---

## 3. 심사위원을 위한 'Wow Factor' 구현 팁

### 🎯 Tip 1: Sync-Play (동기화 재생)
사용자가 아래 리스트의 '조사 내용' 카드를 클릭하면, 상단의 **ExoPlayer가 해당 사건이 발생한 시간(SeekTo)**으로 즉시 이동하게 구현합니다.
- `exoPlayer.seekTo(item.timestamp)`
- 이 기능은 "단순 영상 재생"을 "인터랙티브 리플레이"로 격상시킵니다.

### 🎯 Tip 2: 양피지 질감 (Parchment Texture)
완전 검정 배경 위에 아주 미세한 노이즈가 섞인 어두운 양피지 텍스처를 **`Alpha 0.05`** 정도로 OVERLAY 하면 호러 매거진의 감성이 살아납니다.

### 🎯 Tip 3: "CLASSIFIED" 워터마크
카드 하단에 `CLASSIFIED`, `CONFIDENTIAL` 같은 텍스트를 아주 넓은 자간으로 배치하여 수사 보고서의 격식을 갖춥니다.

### 🎯 Tip 4: 게임 로그 추출 (Log Extraction) - [CORE FEATURE]
**Gemini 3 Pro**의 **Multimodal Long Context**를 활용하여, 3시간 분량의 사진/오디오를 분석해 게임 내에서 발생한 주요 이벤트를 텍스트 로그로 변환합니다.
- 예: "15:30 - 조사자가 다락방에서 수상한 일기장을 발견함"
- 프롬프트에 "게임 플레이 흐름을 타임스탬프와 함께 사건 일지 형식으로 요약해줘"라고 명령하여 데이터를 추출합니다.

---

## 4. 데이터 구조 (Gemini 연동용 JSON)

Gemini에게 결과물을 아래와 같은 JSON 형식으로 달라고 프롬프트를 짜면 연동이 쉬워집니다.

```json
{
  "project_title": "Afterlog Report",
  "case_number": "1923-AH-047",
  "highlights": [...],
  "game_logs": [
    {
      "time": "15:30",
      "event": "Investigation commenced at abandoned Marsh estate."
    },
    {
      "time": "16:45",
      "event": "Discovered hidden chamber beneath main hall."
    }
  ]
}
```

---

## 5. [NEW] 로그 추출을 위한 프롬프트 전략 (Prompting Strategy)

**Gemini 3 Pro**가 방대한 시퀀스 데이터에서 정확한 로그를 추출하게 만드는 핵심 지침입니다.

**System Instruction 예시 (정확도 강화):**
> 당신은 1920년대 아컴 시의 노련한 수사 기자입니다. 
> 1. 제공된 **사진 시퀀스**와 **음성 녹취록(Audio Transcripts)**을 교차 검증하여 사실관계를 파악하세요.
> 2. 사진에 보이는 카드 텍스트나 주사위 눈을 최대한 있는 그대로 해석하되, 불확실하면 추측하지 마세요.
> 3. 비명이 감지된 시점(오디오 피크)의 전후 대화 내용을 분석하여 정확한 사건 원인을 파악하세요. (예: "으악! 뱀이다!"라는 음성이 있으면 '뱀 몬스터 출현' 기록)
> 4. 모든 결과는 JSON 형식으로 답변하며, 타임스탬프와 함께 사건을 기록하세요.

---

### 6. 정확도 보완 기술 전략 (Accuracy Assurance)
AI의 환각(Hallucination)을 방지하고 사용자가 원하는 정확한 로그를 제공하기 위한 안전장치입니다.

1.  **음성 인식 키워드 매칭 (Audio-Logging)**
    - 단순히 데시벨만 보는 게 아니라, 구글의 온디바이스 STT를 통해 "성공", "실패", "죽었다", "찾았다" 같은 **핵심 키워드**가 들릴 때마다 타임스탬프를 함께 기록해 둡니다.
    - Gemini에게 이 텍스트 로그를 힌트로 제공하면 정확도가 비약적으로 상승합니다.

2.  **휴먼-인-더-루프 (Human-in-the-Loop) 편집 기능**
    - AI가 생성한 1차 초안을 `NewspaperView`에서 사용자가 직접 **수정(Edit)**할 수 있게 합니다.
    - "AI가 80%를 써주고, 사용자가 20%의 디테일을 수정"하는 UX가 가장 현실적이고 만족도가 높습니다.

---

---

## 7. Gemini API 연동 포인트 (Integration Points)

이 프로젝트에서 Gemini API는 **게임 종료 후 '결과 생성' 단계**에서 딱 한 번, 무겁게 호출됩니다. 

### 요청 구조 (Request Payload)
`vertexai.generativeModel.generateContent()` 호출 시 다음 데이터를 리스트로 묶어서 보냅니다.

1.  **System Prompt**: "너는 아컴의 기자다..." (페르소나 정의)
2.  **Images (Vision)**: 선별된 핵심 사진 17~50장 (`Bitmap` or `URI`)
3.  **Audio Transcripts (Context)**: STT로 변환된 타임스탬프별 대화 로그 ("15:30 [웬디]: 비명 소리!")

### 응답 처리 (Response Handling)
Gemini는 이 모든 것을 보고 **하나의 거대한 JSON**을 뱉어냅니다.

```kotlin
// 예시: Gemini가 생성하는 최종 JSON 구조
data class AnalysisResult(
    val story_title: String, // "지하실의 악몽"
    val main_narrative: String, // "웬디는 떨리는 손으로..." (소설 본문)
    val game_logs: List<GameLogItem> // [15:30] 지하실 문 개방 등 사건 일지
)
```
이 JSON을 파싱해서 --> `NewspaperView`에 뿌리고, `TTS`로 읽어줍니다.
