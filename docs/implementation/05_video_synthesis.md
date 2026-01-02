# 05. 영상 합성 및 TTS (Video Synthesis & TTS)

## 1. 목적 및 범위
- **목적**: AI가 생성한 텍스트 서사(Script)를 음성(TTS)으로 변환하고, 수집된 시각 자료(Video/Image)와 결합하여 최종 MP4 리플레이 영상을 생성.
- **범위**: Google Cloud TTS API, Android MediaMuxer/MediaCodec, ExoPlayer 프리뷰.

## 2. 데이터 흐름
1. **Input**:
   - `AnalysisResult` (Gemini가 만든 대본).
   - `SessionMedia` (로컬에 저장된 Video Chunks + Images).
2. **Process**:
   - **Step 1 (TTS)**: 대본 -> 오디오 파일(.mp3/wav) 변환.
   - **Step 2 (Visuals)**: 타임스탬프에 맞춰 비디오 클립 또는 이미지 배치.
   - **Step 3 (Muxing)**: 오디오와 비디오 트랙을 하나의 MP4로 병합.
3. **Output**:
   - `final_replay_{sessionId}.mp4`

## 3. 기술 요구사항

### 🗣️ Google Cloud TTS (Text-to-Speech)
- **API**: `https://texttospeech.googleapis.com/v1/text:synthesize`
- **Voice**: `en-US-Neural2-D` (남성 탐정 톤) or `en-US-Neural2-F` (여성 톤).
- **Format**: MP3 or Linear16 (WAV).
- **Optimization**: 전체 텍스트를 한 번에 요청하기보다, 문단 단위로 요청하여 캐싱 및 병렬 처리.

### 🎞️ MediaMuxer & Codec
- **Video Track**:
  - **Case A (Video Exists)**: `MediaExtractor`로 원본 영상에서 트랙 추출 후 Muxer에 write.
  - **Case B (Image Only)**: `MediaCodec` (SurfaceInput)을 사용하여 이미지를 5초간 인코딩하여 비디오 프레임 생성 (Ken Burns 효과 적용 가능 시 시도).
- **Audio Track**:
  - TTS 오디오 파일을 `MediaExtractor`로 읽어서 Muxing.
  - 배경음(BGM) 믹싱 필요 시 `AudioTrack` 믹싱 후 인코딩 필요 (MVP 단계에선 TTS만).

## 4. 인터페이스 설계

### `VideoSynthesizer`
```kotlin
interface VideoSynthesizer {
    suspend fun generateReplay(
        sessionId: String, 
        script: List<ValidationLog>
    ): File
}
```

### `TtsRepository`
```kotlin
interface TtsRepository {
    suspend fun synthesize(text: String): File // Returns audio file path
}
```

## 5. 구현 우선순위 및 체크리스트
1. [ ] **Cloud Console**: Google Cloud Project에서 TTS API 활성화 및 API Key 확보.
2. [ ] **TTS Client**: Retrofit을 사용하여 텍스트 -> 오디오 파일 저장 테스트.
3. [ ] **Muxer Prototype**: 정지 이미지 1장 + 오디오 파일 1개를 합쳐 5초짜리 MP4 만드는 로직 구현.
4. [ ] **Video Stitching**: 여러 개의 비디오 청크를 하나로 이어 붙이는 로직 구현.
5. [ ] **Full Pipeline**: `Gemini Result` -> `TTS` -> `Muxing` -> `ExoPlayer Playback` 흐름 통합.
