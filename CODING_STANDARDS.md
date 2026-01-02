# 📏 Afterlog Coding Standards & Antigravity Rules

이 문서는 **Antigravity(AI)와 인간 개발자**가 함께 지켜야 할 절대 규칙입니다.
초보자가 가장 많이 하는 실수(스파게티 코드, 메인 스레드 차단, N+1 쿼리)를 방지하기 위해 설계되었습니다.

---

## 🤖 1. Antigravity AI Rules (For System)
**Antigravity는 코드를 작성할 때 다음 규칙을 최우선으로 준수해야 합니다.**

```markdown
You are an expert Android Developer using Kotlin, Jetpack Compose, and CameraX.
Follow these specific rules to prevent spaghetti code:

1.  **Strict MVVM Architecture**:
    - NEVER put business logic in MainActivity or Composables.
    - UI only observes StateFlow from ViewModel.
    - ViewModel never holds references to Context, Activity, or View.

2.  **Room Database Safety**:
    - NEVER use `forEach` loop to query related data (N+1 Problem).
    - ALWAYS use `@Relation` and `Data Class` to fetch relational data in ONE query.
    - Database operations MUST be executed on `Dispatchers.IO`.

3.  **Composable Constraints**:
    - Break down Composables if they exceed 100 lines.
    - Always use `Modifier` as the first optional parameter.
    - Separate event handling (lambdas) from UI rendering.

4.  **Error Handling**:
    - Never catch generic `Exception` without logging or handling.
    - Use `runCatching` or specific `try-catch` blocks.
```

---

## 🚨 2. 초보자용 절대 금지 (Anti-Patterns)

### ❌ 1. "DB에서 하나씩 꺼내오기" (N+1 문제)
게임 세션이 10개인데, 각 세션의 로그를 가져오려고 쿼리를 10번 더 날리는 실수입니다.
**결과**: 앱이 버벅거리고 폰이 뜨거워집니다.

*   **Bad Code 👎**
    ```kotlin
    // 절대 금지: for문 안에서 DB 호출
    val sessions = sessionDao.getAll()
    sessions.forEach { session ->
        val logs = logDao.getLogs(session.id) // 쿼리 폭탄 💣
    }
    ```
*   **Good Code 👍**
    ```kotlin
    // 해결책: 관계(Relation)를 정의해서 한 방에 가져오기
    data class SessionWithLogs(
        @Embedded val session: GameSession,
        @Relation(parentColumn = "id", entityColumn = "sessionId")
        val logs: List<Log>
    )
    // DAO
    @Transaction
    @Query("SELECT * FROM session")
    fun getSessionWithLogs(): List<SessionWithLogs> // 쿼리 1방 ⚡
    ```

### ❌ 2. "메인 액티비티에 다 때려박기" (God Activity)
`MainActivity.kt`가 500줄이 넘어간다면 **이미 망한 코드**입니다.
**결과**: 뭐 하나 고치면 다른 게 터짐. 유지보수 불가능.

*   **해결책 (Rule)**:
    - `MainActivity`는 오직 **"앱을 켜고, 내비게이션을 세팅"**하는 역할만 합니다.
    - 버튼 클릭 로직? -> `ViewModel`로.
    - 데이터 저장? -> `Repository`로.

### ❌ 3. "잠깐 멈춰봐" (Main Thread Blocking)
UI를 그리는 스레드(Main Thread)에서 DB를 읽거나 파일을 저장하면 앱이 **"응답 없음(ANR)"**으로 죽습니다.

*   **Bad Code 👎**
    ```kotlin
    fun onButtonClick() {
        db.save(data) // 🚫 UI 멈춤!
    }
    ```
*   **Good Code 👍**
    ```kotlin
    fun onButtonClick() {
        viewModelScope.launch(Dispatchers.IO) { // ✅ 백그라운드에서 실행
            db.save(data)
        }
    }
    ```

---

## 🏗️ 3. 코드 작성 체크리스트 (Before Commit)

코드를 다 짰다면, 커밋하기 전에 이 5가지를 확인하세요.

- [ ] **ViewModel 분리**: `MainActivity` 안에 `if`, `for` 같은 로직이 남아있지 않은가?
- [ ] **하드코딩 제거**: "저장을 완료했습니다" 같은 문자열이 코드에 박혀있지 않은가? (`strings.xml` 사용)
- [ ] **메인 스레드 보호**: DB나 파일 접근 코드에 `Dispatchers.IO`가 적용되었는가?
- [ ] **리소스 해제**: 카메라, 오디오 레코더를 쓴 뒤 `release()`나 `unbind()`를 호출했는가?
- [ ] **Log 정리**: `Log.d("test", "aaaa")` 같은 의미 없는 로그를 지웠는가?

---

## 🛠️ 4. 폴더 구조 (Package Structure)
이 구조를 벗어나서 파일을 만들지 마세요.

```text
com.example.afterlog
├── data            (데이터 관련: Room, API, Repository)
│   ├── local       (Room DAO, Entity)
│   ├── remote      (Retrofit, Gemini API)
│   └── repository  (데이터를 ViewModel에 전달하는 창구)
├── di              (의존성 주입: Hilt Module)
├── domain          (비즈니스 로직, 모델: 순수 Kotlin 코드)
├── feature         (화면 단위 패키지)
│   ├── camera      (CameraScreen, CameraViewModel)
│   ├── gallery     (GalleryScreen, GalleryViewModel)
│   └── viewer      (NewspaperViewer, ViewerViewModel)
└── ui              (공통 UI)
    ├── theme       (Color, Type)
    └── components  (재사용 버튼, 카드 등)
```

---

## 🏆 5. Hackathon Specific Rules (Special)

### 🇺🇸 5.1 English-First Policy
**Rule**: This project is for an International Hackathon.
- **UI Strings**: MUST be in English (use `strings.xml`).
- **Code Comments**: English ONLY.
- **Commit Messages**: English ONLY.
- **Internal Docs**: Korean is allowed for team communication, but final artifacts must be English.

### 🤖 5.2 Gemini Integration Safety
**Rule**: AI features must fail gracefully.
- **No Hardcoding**: API Keys must be in `local.properties` (BuildConfig).
- **Graceful Failure**: If Gemini fails (Network/Safety), show a fallback UI or error message. Do NOT crash.
- **Mock Mode**: Implement a "Mock Data" logic for testing without using API quota.

### 🖥️ 5.3 Compose Preview Rule
**Rule**: Accelerate UI Dev.
- All Screen-level Composables MUST have a `@Preview` with dummy data.
- Use `PreviewParameterProvider` for complex data.
