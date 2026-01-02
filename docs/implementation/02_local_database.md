# 02. 로컬 데이터 모델링 (Local Database with Room)

## 1. 목적 및 범위
- **목적**: 네트워크 연결 없이도 수집된 데이터를 안전하게 저장하고, 앱 재실행 시에도 데이터를 유지하는 "Offline-first" 아키텍처 구현.
- **범위**: Room Database 설정, Entity 설계, DAO 구현, Repository 패턴 적용.

## 2. 데이터 구조 (Entity Modeling)

### 1) Session (게임 세션)
- 게임의 한 판을 의미합니다.
```kotlin
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // 초대 코드 겸용
    val startTime: Long,
    val endTime: Long? = null,
    val title: String? = null // 나중에 AI가 지어준 제목
)
```

### 2) MediaLog (수집된 원본 데이터)
- 개별 사진이나 오디오 이벤트 로그입니다.
```kotlin
@Entity(tableName = "media_logs")
data class MediaLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String, // FK -> Session
    val type: MediaType, // IMAGE, AUDIO_CHUNK, SCREAM_EVENT
    val filePath: String, // 로컬 절대 경로
    val timestamp: Long, // NTP 보정된 시간
    val decibel: Int? = null, // 오디오인 경우
    val isSynced: Boolean = false // 서버 업로드 여부
)
```

### 3) AnalysisResult (AI 분석 결과)
- Gemini가 분석한 JSON 결과를 캐싱합니다.
```kotlin
@Entity(tableName = "analysis_results")
data class AnalysisResultEntity(
    @PrimaryKey val sessionId: String,
    val jsonContent: String, // Gemini Response JSON Raw String
    val summary: String,
    val createdAt: Long
)
```

## 3. 인터페이스 (DAO & Repository)
- **LogDao**:
  - `insertLog(log: MediaLogEntity)`: 고속 삽입.
  - `getLogsBySession(sessionId: String): Flow<List<MediaLogEntity>>`: UI 관찰용.
- **SessionDao**:
  - `createSession(...)`
  - `updateSessionEnd(...)`

## 4. 구현 우선순위 및 체크리스트
1. [ ] **Entity 작성**: 3개 테이블 클래스 및 Enum(`MediaType`) 정의.
2. [ ] **TypeConverter**: `List<String>` 등을 저장하기 위한 컨버터 구현 (필요시).
3. [ ] **DAO 작성**: 필수 CRUD 쿼리 작성.
4. [ ] **Database Class**: `RoomDatabase` 상속 클래스 및 Migration 전략(fallbackToDestructiveMigration) 설정.
5. [ ] **Hilt Module**: `DatabaseModule`에서 `provideLogDao` 등 DI 설정.
6. [ ] **Repository**: `LocalRepository` 클래스 구현 후 `ViewModel`에서 호출 테스트.
