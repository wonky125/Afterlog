# 04. Gemini AI 클라이언트 (Gemini Client Module)

## 1. 목적 및 범위
- **목적**: 수집된 원본 데이터(Multi-modal)를 Gemini 3 Pro에 전송하여 구조화된 사건 로그와 서사를 생성.
- **범위**: Google AI Client SDK 연동, 안전 설정(Safety Settings), 프롬프트 엔지니어링, JSON 파싱.

## 2. 데이터 구조
### Request (Prompt Construction)
- **Model**: `gemini-1.5-pro` (Long Context Window 필요).
- **Parts**:
  - `Text`: System Prompt (Persona: 1920s Investigative Journalist in Arkham).
  - `List<Bitmap>`: Resized image list (max ~50 images).
  - `FileRef`: **Files API URI** (Large Audio File uploaded via REST).
    - SDK direct upload is limited (20MB). Full audio requires `https://generativelanguage.googleapis.com/upload/v1beta/files`.

### Response (AnalysisResultJson)
```json
{
  "title": "Madness in the Basement",
  "summary": "The investigators uncovered a dark secret...",
  "logs": [
    {
      "timestamp_ms": 124000,
      "speaker": "Wendy",
      "action": "Scream",
      "description": "Screamed at the sight of the Shoggoth"
    }
  ]
}
```

## 3. 기술 요구사항
- **Generative AI SDK**: 
  - `implementation("com.google.ai.client.generativeai:generativeai:x.x.x")`
- **JSON Parser**: `Kotlinx Serialization` (Strict Mode 끄기 권장).
- **API Key Management**: `local.properties` 또는 `BuildConfig`로 관리.

## 4. 인터페이스
- **GeminiRepository**:
  - `suspend fun analyzeSession(sessionId: String): Result<AnalysisResultEntity>`
  - 내부 로직:
    1. DB에서 해당 세션의 모든 이미지/오디오 경로 조회.
    2. 데이터 샘플링 (너무 많으면 1/N로 줄이기).
    3. Gemini 호출 (`generateContent`).
    4. 응답 JSON 파싱 및 DB 저장.

## 5. 구현 우선순위 및 체크리스트
1. [ ] **API Key 발급**: Google AI Studio에서 키 발급 및 프로젝트 적용.
2. [ ] **Simple Test**: 텍스트 프롬프트로 "Hello" 응답 확인.
3. [ ] **Multimodal Test**: 로컬 이미지 1장 + "이게 뭐니" 질문 테스트.
4. [ ] **Prompt Tuning**: Apply "Investigative Journalist" persona (English).
5. [ ] **Serialization**: JSON 응답 파싱 로직 및 예외 처리 (JSON 형식이 깨져서 올 경우 대비).
