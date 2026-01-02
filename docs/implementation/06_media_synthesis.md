# 06. 미디어 재생 및 합성 엔진 (Media Engine)

## 1. 목적 및 범위
- **목적**: 정적 이미지와 오디오, 동적 로그를 결합하여 사용자에게 "영상" 경험 제공.
- **범위**: ExoPlayer 연동, 타임라인 동기화(Sync-Play), 로그 클릭 시 Seek 기능.

## 2. 화면 구성
- **VideoSection**: UI 상단에 고정(`StickyHeader` 또는 `CoordinatorLayout` 유사 구조).
- **Controls**: 재생/일시정지, 타임라인 바.

## 3. 기술 요구사항
### ExoPlayer (Media3)
- **Source**: 
  - 기본: 오디오 파일(AAC) 재생 + 이미지는 UI에서 교체.
  - 심화: `MediaMuxer`로 미리 만들어진 MP4 파일 재생.
- **Sync Logic**:
  - `LaunchedEffect(player.currentPosition)`: 현재 재생 시간을 UI 상태로 전파.
  - `onLogClick(timestamp)`: 플레이어를 해당 위치로 이동 (`seekTo`).

### TTS (Text-to-Speech)
- **Google Cloud TTS API**:
  - `AnalysisResult` `summary` or `logs` -> Audio (MP3).
  - **Caching**: Save local files to save cost.
  - **Voice**: `Studio Voice` or `Neural2` (en-US, British or Mid-Atlantic accent).

### MediaMuxer (Video Synthesis - MVP)
- **Hybrid Strategy**:
  - 로컬 비디오 클립이 있으면 사용, 없으면 이미지(Time-lapse) 사용.
  - TTS 오디오 트랙을 배경음으로 병합.
- **Output**: `final_replay.mp4` 생성.

## 4. 인터페이스
- **PlayerViewModel**:
  - `play()`, `pause()`, `seekTo(ms)`
  - `currentTimestamp: StateFlow<Long>`

## 5. 구현 우선순위 및 체크리스트
1. [ ] **Simple Player**: 오디오 파일 하나만 재생하는 ExoPlayer 구현.
2. [ ] **Image Sync**: `currentTimestamp`에 맞춰 현재 보여줘야 할 이미지를 찾는 로직 (`binarySearch`).
3. [ ] **Interaction**: 리포트 UI의 아이템 클릭 -> 플레이어 Seek 연결.
4. [ ] **Auto-Scroll**: 플레이어 진행 -> 리포트 리스트 자동 스크롤 (선택 사항).
