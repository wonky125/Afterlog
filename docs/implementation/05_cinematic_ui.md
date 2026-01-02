# 05. 시네마틱 리포트 UI (Cinematic Report UI)

## 1. 목적 및 범위
- **목적**: 분석된 로그 데이터를 "20년대 신문 기사" 컨셉으로 시각화하여 사용자 몰입도 증대.
- **범위**: Compose UI 레이아웃, 커스텀 폰트/이미지 적용, 반응형 디자인.

## 2. 화면 구성 (NewspaperView)
- **Container**: `LazyColumn` + `Box` (배경 이미지 Overlay).
- **Background**: `Color(0xFFF5E6D3)` (베이지색 양피지 톤) + 노이즈 텍스처.
- **Header**:
  - `Text("EXTRA!", fontSize = 60.sp, fontFamily = Serif)`
  - 사건 날짜 및 회차 정보.
- **Body**:
  - **Highlights**: 상단 1면 기사 (가장 중요한 사진 + 요약).
  - **Timeline**: 시간순 사건 로그 리스트. 좌측 사진 / 우측 기사 내용.

## 3. 기술 요구사항
- **Coil**: 로컬 파일 경로 (`File(path)`) 이미지 로딩.
- **Font**: Google Fonts (`Playfair Display`, `Cinzel` 등 세리프체 권장).
- **Effect**:
  - `BlendMode.Multiply`를 이용한 텍스처 합성.
  - 리스트 스크롤 시 Fade-in 애니메이션.

## 4. 데이터 구조 (UiState)
```kotlin
data class ReportUiState(
    val isLoading: Boolean = false,
    val title: String = "",
    val items: List<ReportItem> = emptyList(),
    val error: String? = null
)

data class ReportItem(
    val timestamp: Long,
    val imagePath: String,
    val content: String,
    val isImportant: Boolean
)
```

## 5. 구현 우선순위 및 체크리스트
1. [ ] **Mock Data 생성**: 실제 DB 없이 UI 개발이 가능하도록 더미 리스트 생성.
2. [ ] **Typography**: 세리프 폰트 파일 `res/font` 추가 및 적용.
3. [ ] **Layout Structure**: 헤더, 바디, 푸터 영역 잡기.
4. [ ] **Item Composable**: `EvidenceCard` (사진+글+시간) 컴포넌트 구현.
5. [ ] **Polish**: 종이 질감 이미지 구해서 배경에 깔고 오버레이 모드 테스트.
