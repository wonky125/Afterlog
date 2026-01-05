# 04. Gemini AI 클라이언트 (Gemini Client Module)

## 1. 목적 및 범위
- **목적**: 수집된 원본 데이터(Multi-modal)를 Gemini 3 Pro Preview에 전송하여 구조화된 사건 로그와 서사를 생성.
- **범위**: Google AI Client SDK 연동, 안전 설정(Safety Settings), 프롬프트 엔지니어링, JSON 파싱.

## 2. 데이터 구조
### Request (Prompt Construction)
- **Model**: `gemini-2.5-flash` (개발용, 높은 할당량) / `gemini-3-pro-preview` (최종 제출용, Long Context).
- **Parts**:
  - `Text`: System Prompt (Persona: MOTH_ER - Station AI of Aegis-7).
  - `List<Bitmap>`: Resized image list (max ~50 images, Grayscale processing recommended).
  - `FileRef`: **Files API URI** (Large Audio File uploaded via REST).
    - SDK direct upload is limited (20MB). Full audio requires `https://generativelanguage.googleapis.com/upload/v1beta/files`.

### Response (DecryptedLogJson)
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

## 3. 기술 요구사항
- **Generative AI SDK**: 
  - `implementation("com.google.ai.client.generativeai:generativeai:x.x.x")`
- **JSON Parser**: `Kotlinx Serialization` (Strict Mode 끄기 권장).
- **API Key Management**: `local.properties` 또는 `BuildConfig`로 관리.

## 4. 인터페이스
- **GeminiRepository**:
  - `suspend fun analyzeSession(sessionId: String): Result<DecryptedLogEntity>`
  - 내부 로직:
    1. DB에서 해당 세션의 모든 이미지/오디오 경로 조회.
    2. 데이터 샘플링 (너무 많으면 1/N로 줄이기).
    3. Gemini 호출 (`generateContent`).
    4. 응답 JSON 파싱 및 DB 저장.

## 5. 구현 우선순위 및 체크리스트
1. [X] **API Key 발급**: Google AI Studio에서 키 발급 및 프로젝트 적용.
2. [X] **Simple Test**: 텍스트 프롬프트로 "Hello" 응답 확인.
3. [X] **Multimodal Test**: 로컬 이미지 1장 + "이게 뭐니" 질문 테스트.
4. [X] **Prompt Tuning**: Apply "Station AI (MOTH_ER)" persona & Tone adjustment.
5. [X] **Serialization**: JSON 응답 파싱 로직 및 예외 처리 (JSON 형식이 깨져서 올 경우 대비).
