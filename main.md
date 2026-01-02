# 🎞️ 애프터로그 (Afterlog)
> **AI GM 시네마틱 (Photo-to-Video)**
>
> *"멀티모달 AI가 수집한 비명의 파편을 한 편의 공포 영화로 재구성하다"*

---

## 1. 프로젝트 개요 및 마감 일정 📅

- **프로젝트명**: 애프터로그 (Afterlog)
- **핵심 컨셉**: '광기의 저택' 플레이 중 발생하는 **비명(Audio)**과 **상황 사진(Vision)**을 수집하여, AI GM이 나레이션을 입힌 **하이라이트 동영상(.mp4)**으로 제작.
- **해커톤 마감**: 2026년 2월 9일 오후 5:00 (PST).
- **제출물**:
  - [ ] 신규 안드로이드 앱
  - [ ] 3분 이내 데모 영상
  - [ ] 공개 코드 저장소 (GitHub)

### 💡 기획 의도: "보드게임에도 '리플레이'가 있다면?"
> *"리그 오브 레전드(LoL)는 게임이 끝나면 '내가 이때 개쩔었지(God Play)' 하고 리플레이를 돌려볼 수 있잖아요. 그 쾌감이 엄청나죠. 
> 그런데 보드게임은? 3시간 동안 웃고 떠들어도 게임판을 접으면 모든 기억이 휘발됩니다. 기록을 남기고 싶어도 매번 받아 적을 수도 없고요. 이 간극을 AI로 메우고 싶었습니다."*
 
이 아이디어를 실현하려면 **"게임판의 시각적 변화"**와 **"플레이어들의 육성(비명)"**을 동시에 이해하고 연결해야 합니다. 
과거엔 불가능했지만, 압도적인 **멀티모달 성능**을 가진 **Gemini 3 Pro**가 등장했기 때문에 비로소 시도해볼 수 있게 되었습니다. 
**[Afterlog]**는 휘발되는 아날로그 경험을 영원한 디지털 '시네마틱 리플레이'로 박제해줍니다.

---

## 2. 기술 스택 (Tech Stack) 및 심사 기준 적용 🛠️

구글의 최신 기술을 활용하여 기술적 깊이를 증명합니다.

| 구분 | 기술 선정 | 역할 및 이유 |
| :--- | :--- | :--- |
| **Language & UI** | `Kotlin`, `Compose` | 안드로이드 네이티브 최적화 및 **Material 3** 적용으로 고품질 UX 제공. |
| **AI Model** | `Gemini 3 Pro` | **멀티모달 이미지 시퀀스 및 오디오 분석** (Long Context 활용). |
| **Vision** | `CameraX` | **백그라운드 무음 촬영 및 타임랩스** 구현, 기기 호환성 보장. |
| **Audio** | `Gemini 3.0 Pro Native` | 별도의 라이브러리 없이 **오디오 파일을 통째로 Gemini에 업로드**하여 화자 분리 및 맥락 분석 수행. |
| **Voice (TTS)** | `Google Cloud TTS` | 생성된 서사를 음성으로 읽어주는 **접근성 기능** 및 고품질 나레이션. |
| **Backend** | `Firebase` | **멀티 디바이스(Host-Guest) 데이터 동기화** 및 이미지 저장. |
| **Local DB** | `Room` | Offline-first 전략을 위한 로컬 데이터 저장 및 동기화 관리. |
| **Video Player**| `ExoPlayer` | 하이라이트 영상의 **심리스한 무한 반복 및 타임라인 동기화** 재생. |
| **Processing** | `MediaMuxer/Codec` | 사진 시퀀스와 TTS 오디오를 결합해 최종 MP4 영상 합성. |


---

## 3. 심사 기준 및 전략적 적용 포인트 (Judging Criteria) 🎯

해커톤 승리를 위해 각 평가 항목별 핵심 소구 포인트를 다음과 같이 설계했습니다.

### ① Technical Execution (기술적 실행력 - 40%)
*이 항목은 앱이 얼마나 잘 작동하고 Gemini를 핵심적으로 활용했는지를 평가합니다.*
- **Gemini 3 Pro의 긴 문맥(Long Context) 활용**: 3시간 동안 찍힌 수백 장의 사진을 한 번에 분석하여 일관된 서사를 추출하는 능력.
- **멀티 디바이스 동기화**: NTP 서버 기반 타임스탬프 보정으로 여러 대의 기기에서 찍은 사진을 하나의 타임라인으로 병합.
- **안정적인 백그라운드 서비스**: `Foreground Service`를 사용해 다른 앱 실행 중에도 끊김 없는 데이터 수집 보장.

### ② Innovation/Wow Factor (혁신성/와우 포인트 - 30%)
*아이디어의 신선함과 독창적인 문제 해결 방식을 평가합니다.*
- **오디오 트리거 (Scream Detection)**: 단순히 사진을 찍는 게 아니라, 유저의 감정(비명, 환호)을 감지하여 하이라이트를 자동 포착하는 인터랙션.
- **매거진 스타일 '시네마틱 보고서'**: 단순히 영상을 보여주는 것을 넘어, 신문 기사나 조사 보고서 형식으로 AI 나레이션과 핵심 스틸컷을 결합한 프리미엄 UX.
- **AI 페르소나 스토리텔링**: 단순 요약이 아닌 '1920년대 신문 기자' 혹은 '러브크래프트 작가' 스타일로 게임 결과를 재해석하는 감성적 접근.

### ③ Potential Impact (잠재적 영향력 - 20%)
*실제 시장에서의 유용성과 파급력을 평가합니다.*
- **휘발되는 경험의 자산화**: 즐거운 보드게임 추억을 디지털 기본 소설로 남겨 영구 소장하게 만드는 가치.
- **접근성(Accessibility) 강화**: TTS 기능을 통해 시각 약자나 독서 장애가 있는 유저도 게임의 서사를 즐길 수 있게 배려 (**AI for Everyone**).

### ④ Presentation/Demo (발표 및 데모 - 10%)
*문제 정의의 명확성과 데모 영상의 효과성을 평가합니다.*
- **3분 이내의 임팩트 있는 영상**: "비명 감지 -> 자동 촬영 -> Gemini 분석 -> 리플레이 생성"의 흐름을 실제 플레이와 함께 연출.
- **아키텍처 다이어그램**: 멀티 디바이스와 Gemini API가 어떻게 데이터를 주고받는지 시각화된 문서 제출.

---

## 4. 시스템 아키텍처 블루프린트 (Architecture Blueprint) 🏗️

이 시스템은 여러 대의 카메라가 하나의 지능을 공유하는 **분산형 멀티모달 시스템**으로 설계되었습니다.

### 1) 디바이스 역할 분담 (Distributed Device Roles)
- **Host Device (Main Controller)**
  - 게임 세션(Room) 생성 및 초대 코드(6자리) 발급.
  - Firebase Firestore/Storage 통합 관리.
  - **Gemini 3 Pro API와의 전담 통신 및 데이터 취합**.
  - 최종 리플레이 스토리 생성 및 뷰어 제공.
- **Guest Devices (1~3대)**
  - 전담 각도 촬영 (보드판 전체, 특정 피규어 밀착 등).
  - 실시간 오디오 모니터링: 각 기기에서 주변 소음을 감지하여 '이벤트 로그' 전송.

### 2) 안드로이드 내부 소프트웨어 구조 (Internal App Structure)
- **UI Layer (Jetpack Compose)**: Material 3 디자인을 적용한 **매거진 스타일**의 리플레이 뷰어.
- **Business Logic Layer (MVVM)**
  - `Foreground Service`: 안드로이드의 배터리 최적화로 인한 앱 종료를 방지하며, 공식 보드게임 앱 위에서도 작동하도록 **Overlay Permission** 기반의 플로팅 버튼 제공.
  - `CameraX & AudioRecord`: 백그라운드 무음 촬영 및 실시간 PCM 데이터 분석(비명 감지).
- **Data Layer**
  - `Local Repository (Room)`: 네트워크 불안정 시를 대비해 캡처한 메타데이터를 우선 저장하는 **Offline-first** 전략.
  - `Firebase Integration`: 사진은 Storage에, 로그 및 이벤트 정보는 Firestore에 실시간 동기화.

### 3) 시네마틱 보고서 인터페이스 (Cinematic Report UI)
해커톤의 'UX 완성도'를 극대화하기 위해 **ExoPlayer와 Jetpack Compose**를 결합한 매거진 스타일 레이아웃을 채택합니다.

- **상단 섹션**: `ExoPlayer`를 통한 하이라이트 영상 무한 반복 재생.
- **중단 섹션**: AI가 생성한 '신문 헤드라인' 스타일의 총평.
- **하단 섹션**: 타임라인 기반 카드 리스트. 사진 클릭 시 영상이 해당 시점으로 이동(**Sync-Play**).

```kotlin
@Composable
fun ReplayDetailScreen(videoUri: String, logs: List<InvestigationLog>) {
    // 1. 전체를 스크롤 가능한 Column으로 감쌉니다 (신문 읽듯이)
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ArkhamBlack)
    ) {
        // [섹션 1] 하이라이트 영상 (고정 또는 상단 배치)
        item {
            VideoPlayerSection(videoUri) 
        }

        // [섹션 2] AI 총평 (신문 헤드라인 느낌)
        item {
            Text(
                text = "INVESTIGATION REPORT",
                style = MaterialTheme.typography.displaySmall,
                color = ArkhamPurple,
                modifier = Modifier.padding(16.dp)
            )
        }

        // [섹션 3] 당시 상황 사진 + 텍스트 (신문 기사 스타일)
        items(logs) { log ->
            Row(modifier = Modifier.padding(16.dp)) {
                // 그때 찍힌 사진
                AsyncImage(
                    model = log.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(12.dp))
                // AI의 상황 묘사
                Column {
                    Text(text = log.timestamp, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(text = log.aiDescription, color = Color.White)
                }
            }
            Divider(color = Color.DarkGray) // 구분선으로 신문 느낌 강조
        }
    }
}
```

> [!TIP]
> **디테일 전략**: 배경에 어두운 양피지 질감을 입히고, 보고서 전체를 '이미지로 공유하기' 기능을 제공하여 서비스의 확장성을 증명합니다.

---

## 5. 핵심 데이터 파이프라인 (The Intelligent Pipeline) 🧠

3시간의 방대한 데이터를 효율적으로 처리하기 위한 **2단계 파이프라인** 설계입니다.

### 📸 1단계: 수집 (Capture) - "일단 많이 찍어두자"
**트리거**: 비명/환호 (데시벨 80dB 이상)  
**목적**: 놓칠 수 있는 중요한 순간을 확보하기 위해 **원본 재료를 풍부하게 수집**합니다.

1.  **시간 동기화 (Time-Sync)**: 각 기기의 시간 차이를 극복하기 위해 공용 NTP 서버를 기준으로 모든 사진의 타임스탬프를 보정합니다.
2.  **데이터 수집**:
    - **타임랩스 (Dual Strategy)**: 
        - **촬영 (Viewer용)**: **5초 간격**으로 촬영하여 로컬에 저장 (부드러운 영상 재생 목적).
        - **분석 (AI용)**: **15초 간격**으로 선별(Sampling)하여 Gemini에 전송 (토큰 비용 절감 및 맥락 파악 극대화).
    - **오디오 녹음**: 게임 시작부터 종료까지 **전체 오디오를 통으로 녹음** (AAC/MP3, 저용량 코덱 권장).
    - **비디오 트리거 (Hybrid)**: 비명(80dB+) 감지 시 **전 2.5분 + 후 2.5분 = 총 5분** 영상 클립을 로컬에 저장합니다. (Rolling Buffer 활용)

> [!NOTE]
> **비명 기반 수집의 역할**: 경찰이 현장 보존을 위해 일단 사진 100장을 찍어두는 것처럼, 데시벨 트리거는 "혹시 놓칠까봐" 원본을 확보하는 안전망입니다.

### ✂️ 2단계: 편집 (Edit) - "진짜 중요한 건 이거야"
**트리거**: AI가 생성한 로그의 타임스탬프  
**목적**: 수집된 방대한 원본 중 **맥락상 의미 있는 하이라이트만 최종 선별**합니다.

3.  **Context Injection (Gemini 3.0 Pro)**:
    - **Files API 업로드**: 3시간 분량의 오디오 파일(약 200MB)을 `File API`를 통해 구글 서버에 먼저 업로드하고 URI를 획득합니다.
    - **멀티모달 프롬프트**: "이 오디오(URI)와 이 사진들(Images)을 보고 사건 일지를 작성해. 목소리가 다른 사람을 Speaker A, B로 구분해줘."라고 요청합니다.
    - **Gemini Native Diarization**: 별도 알고리즘 없이 Gemini가 내장된 청각 지능으로 화자를 구분하고 대본을 작성합니다.
4.  **멀티모달 출력 (Accessibility)**:
    - 생성된 텍스트 리플레이를 Google Cloud TTS를 통해 성우 같은 목소리로 읽어주어 시각 약자를 배려하고 몰입감을 극대화합니다.
5.  **영상 합성 (Video Synthesis - Hybrid)**:
    - **Priority 1 (Video)**: 로컬 `Rolling Buffer` 영상이 있는 구간은 실제 비디오 클립을 사용합니다 (Sound & Motion 포함).
    - **Priority 2 (Timelapse)**: 영상이 없는 구간은 5초 간격 타임랩스 사진을 이어 붙여 처리합니다.
    - `MediaMuxer`로 이들을 TTS 오디오와 매끄러운 MP4로 최종 병합합니다.

> [!TIP]
> **2단계 파이프라인 비유**: 탐정이 현장 사진 100장 중 "결정적 증거 5장"만 골라서 보고서에 싣는 것처럼, AI 로그는 최종 결과물에 무엇을 포함할지 판단하는 **편집자 역할**을 합니다.

---

## 6. 상세 프로덕션 로드맵 (Production Roadmap) 🚀

| 단계 | 기간 | 핵심 과제 및 심사 기준 연계 | 버퍼/주의사항 |
| :--- | :--- | :--- | :--- |
| **1주** | 기반 구축 | 안드로이드 프로젝트 세팅, CameraX 및 Gemini API 기본 호출 성공 | - |
| **2주** | 감각 기관 | AudioRecord 비명 감지 및 **Rolling Buffer (5분 영상)** 구현 | ⚠️ 안드로이드 12+ 마이크 권한 제약 확인 필요 |
| **3주** | 백그라운드 | Foreground Service 및 Overlay 플로팅 버튼 구현 | 2주차에서 분리하여 안정성 확보 |
| **4주** | 연결 | Firebase Host-Guest 동기화 + NTP 타임스탬프 보정 | - |
| **5주** | 지능 부여 | Gemini 프롬프트 튜닝 + TTS + 영상 합성 (MediaMuxer) | 데모 영상 촬영 병행 시작 |
| **6주** | 완성 & 제출 | 매거진 UI 폴리싱, 3시간 플레이 테스트, 최종 영상 편집 | 버그 수정 버퍼 확보 |

---

## 7. 위험 관리 전략 (Risk Management) ⚠️

> [!TIP]
> **배터리 이슈**: 3시간 지속을 위해 카메라 프리뷰를 끄고(`setPreviewEnabled(false)`) 스틸 컷만 처리합니다.

> [!NOTE]
> **네트워크 이슈**: 네트워크가 끊기면 로컬 Room DB에 저장 후 Wi-Fi 연결 시 Worker를 통해 비동기 업로드합니다.

> [!IMPORTANT]
> **비용 관리 (Cost Estimates)**: 
> - **예상 비용**: 한 게임(2시간) 당 약 **$2.05 (약 2,800원)** 소요.
>   - **Audio (Gemini 3.0)**: 23만 토큰 ≈ **$0.93**
>   - **Vision (Gemini 3.0)**: 480장(15초 간격) ≈ **$1.05** (고품질 분석)
>   - **TTS (Google Cloud)**: 3,000자(Neural2/WaveNet) ≈ **$0.05**
>   - **Video Gen**: 로컬(무료)
> - **전략**: 해커톤 데모용으로는 충분히 감당 가능한 수준이므로, **개발 속도를 위해 최적화(Chunking 등)는 나중으로 미룹니다.** "Make it work, then make it cheap."
> - **토큰 최적화**: 사진은 리사이징하여 보내고, 오디오는 저비트레이트로 용량을 줄여 업로드 시간을 단축합니다.

> [!IMPORTANT]
> **데이터 전략**: 상시 녹음된 오디오를 전체 전송하면 데이터가 너무 큽니다. 기기 내부(On-device) 또는 경량화된 모델로 1차 텍스트 변환 후 전송합니다.

> [!TIP]
> **성능 최적화 및 UX 전략**:
> 1. **화자 분리(Speaker Diarization)**: 목소리의 주파수 특성을 분석해 "Speaker A(웬디)", "Speaker B(레오)"로 구분합니다. 이를 통해 로그의 주체("누가 비명을 질렀나")를 명확히 합니다.
> 2. **비동기 결과 생성 (Post-Processing)**: 방대한 녹취록과 사진을 분석하는 작업은 시간이 걸리므로, 게임 종료 후 **'분석 대기 시간(Loading View)'**을 UX적으로 풀어냅니다. (예: "탐정 사무소에서 자료를 정리 중입니다...")
> 3. **이미지 리사이징**: AI 분석 및 영상용 사진은 FHD(1080p)로 다운샘플링하여 처리 속도를 높입니다.

> [!NOTE]
> **Plan B (비상 대책)**: 5주 일정으로 동영상 합성까지 충분히 가능할 것으로 예상되나, 만약 치명적인 이슈가 발생할 경우 **'Newspaper View'**를 메인 결과물로 내세우는 안전장치가 마련되어 있습니다.

---

## 8. 해커톤 규정 및 규정 준수 전략 (Official Rules Compliance) ⚖️

Devpost 공식 규정(IP Compliance)을 준수하기 위해 다음 전략을 엄격히 따릅니다.

### 🚫 제3자 IP 및 상업적 콘텐츠 (Third-Party Content)
> "The Submission must not contain any content... that displays any third party advertising, slogan, logo, trademark..."
- **전략:** 'Arkham Horror', 'Mansion of Madness' 등의 **상표명은 절대 사용 금지**. 대신 'Cosmic Horror Boardgame' 등의 일반 명사를 사용합니다.
- **영상:** 보드게임 박스, 로고, 캐릭터 일러스트가 영상에 크게 잡히면 **Blur 처리**하거나, 자체 제작한 UI 위주로 촬영합니다.
- **AI 출력:** Gemini가 특정 저작권 캐릭터 이름을 생성하지 않도록 프롬프트에서 제어합니다 (예: '하비 월터스' -> '늙은 교수').

 
### �📦 제출물 요건 (Submission Requirements)
1.  **데모 영상**: 3분 이내 필수. (3분 초과 시 앞부분만 심사됨). 심사위원 설득을 위해 **영어 자막** 필수.
2.  **공개 링크**: APK 설치가 어렵다면 **Google AI Studio 프로토타입 링크**를 제출하여 심사위원이 웹에서 바로 프롬프트를 테스트해볼 수 있게 합니다.

### 🏆 심사 기준 (Judging Criteria) 
- **Wow Factor (30%)**: Newspaper View의 시각적 완성도와 'Sync-Play' 기능으로 승부.
- **Technical Execution (40%)**: Gemini 3 API(멀티모달, 롱컨텍스트)의 기술적 깊이를 강조.

### 🧠 왜 꼭 `Gemini 3 Pro`여야 하는가? (Core Justification)
> *"그냥 소리 큰 부분만 자르면 되는 거 아냐?"* -> **아닙니다.**
1.  **맥락 이해 (Context)**: 단순 데시벨 트리거는 "성공해서 지르는 환호"와 "망해서 지르는 비명"을 구분 못 합니다. Gemini 3는 **영상과 오디오를 동시에 보고 이 비명의 '의미'를 해석**해서, 진짜 공포스러운 순간만 골라냅니다.
2.  **화자 식별 (Diarization)**: "누가" 말했는지 알아야 로그를 씁니다. Gemini의 멀티모달 능력으로 **[영상 속 인물] + [목소리]**를 매칭합니다.
3.  **내러티브 생성 (Storytelling)**: 단순 편집본이 아니라, 앞뒤 상황을 **"인과관계가 있는 이야기"**로 엮어주는 건 LLM만이 할 수 있습니다. (예: "레오가 문을 열었기 때문에 -> 웬디가 비명을 질렀다"는 2분 간격의 인과를 연결).

---

## 9. MVP 및 기능 우선순위 (MVP & Priorities) 🎯

시간 부족 시 **무엇을 포기할지** 미리 정의하여 팀원 간 혼란을 방지합니다.

### ✅ MVP (필수 기능) - 해커톤 제출 최소 요건
| 기능 | 설명 |
| :--- | :--- |
| **단일 기기 촬영 + 녹음** | Host 1대에서 타임랩스 + **오디오 파일 업로드(Files API)**로 전체 대화 수집 |
| **화자 분리(Speaker Diarization)** | **Gemini 3.0 Pro Native** 기능으로 별도 구현 없이 해결 |
| **Gemini 서사 & 로그 생성**| 전체 오디오와 사진 샘플을 기반으로 맥락을 파악하여 **정확도 높은 사건 일지** 생성 |
| **Newspaper View** | 상세 로그와 서사를 신문 기사 형식으로 보여주는 UI |
| **MP4 영상 합성 (Hybrid)** | **실제 비디오 클립(Rolling Buffer)**과 타임랩스 사진을 우선순위에 따라 병합하여 하이라이트 영상 생성 |
| **TTS 낭독** | 생성된 텍스트를 Google Cloud TTS로 음성 변환 |
| **기본 뷰어 UI** | 생성된 리플레이를 볼 수 있는 간단한 화면 |

### 🌟 Nice-to-Have (시간 여유 시 추가)
| 기능 | 설명 | 우선순위 |
| :--- | :--- | :--- |
| **멀티 디바이스 동기화** | Host-Guest 간 Firebase 실시간 동기화 | 높음 |
| **매거진 스타일 UI 폴리싱** | 카드 뉴스 형태의 프리미엄 UX 디테일 강화 | 중간 |
| **중복 사진 제거 알고리즘** | 픽셀 비교를 통한 토큰 절약 | 낮음 |

> [!TIP]
> **목표**: 5주 내에 **단일 기기 완결형 서비스(영상 합성 포함)**를 100% 완성하는 것을 최우선으로 합니다. 멀티 디바이스 기능은 그 후의 보너스 목표입니다.


