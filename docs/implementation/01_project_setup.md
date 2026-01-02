# 01. 프로젝트 초기 설정 (Project Setup)

## 1. 목적 및 범위
- **목적**: 앱 개발을 위한 기반 환경을 구축하고, 의존성 관리 및 공통 아키텍처를 설정합니다.
- **범위**: `build.gradle` 의존성 설정, 앱 권한(Permission) 설정, 테마(Theme), Hilt 의존성 주입(DI), 네비게이션 구조.

## 2. 화면 구성
- **MainActivity**: 앱의 유일한 Entry Point. `NavHost`를 포함.
- **NavHost**: `Home`(빈 화면), `Report`(빈 화면) 경로 설정.

## 3. 기술 요구사항
### Gradle Dependencies (libs.versions.toml 권장 또는 build.gradle.kts)
- **Essential**: `Hilt` (DI), `Navigation Compose`
- **UI**: `Material 3`, `Coil` (Image Loading), `Accompanist Permissions`
- **Media**: `CameraX`, `ExoPlayer`
- **AI**: `Google AI Client SDK (Gemini)`
- **Data**: `Room`, `DataStore`

### Permissions (`AndroidManifest.xml`)
- `CAMERA`: 사진 촬영
- `RECORD_AUDIO`: 데시벨 측정
- `FOREGROUND_SERVICE`: 백그라운드 수집
- `FOREGROUND_SERVICE_CAMERA`, `FOREGROUND_SERVICE_MICROPHONE` (Android 14+)

## 4. 데이터 구조
- **Route (Sealed Class)**
  ```kotlin
  sealed class Screen(val route: String) {
      object Home : Screen("home")
      object ReportDetail : Screen("report_detail")
  }
  ```

## 5. 구현 우선순위 및 체크리스트
1. [ ] **프로젝트 생성 확인**: 패키지명 `com.afterlog.app` (예시)
2. [ ] **Dependencies 추가**: `build.gradle.kts`에 위 라이브러리 일괄 추가 및 Sync.
3. [ ] **Hilt Setup**: 
   - `AfterLogApplication` 클래스 생성 (`@HiltAndroidApp`).
   - `MainActivity`에 `@AndroidEntryPoint` 추가.
4. [ ] **Theme Setup**: 
   - `IMPLEMENTATION_GUIDE.md`의 팔레트 적용 (`Blood Red`, `Deep Black`).
   - `Type.kt`에 커스텀 폰트(세리프 계열) 설정.
5. [ ] **Navigation Shell**: 빈 Composable을 연결하여 앱 실행 시 크래시 없는지 확인.
