# [Feature] 시네마틱 신문 UI 구현 및 성능 최적화 (v1.1)

## 📌 요약 (Summary)
기존의 텍스트 기반 리포트 화면을 **1920s 느와르 스타일의 신문 디자인**으로 전면 개편했습니다. 
또한, 앱 진입 시 발생하던 **13초 가량의 메인 스레드 프리징(ANR) 문제를 해결**하고, 로딩 경험(UX)을 대폭 개선했습니다.

## 🛠️ 주요 변경 사항 (Key Changes)
### 1. Visual & UI Overhaul
- **신문 테마 적용**: `NewspaperHeader`, `TexturedBackground` (종이 질감), `NewspaperTypography` (빈티지 폰트) 적용
- **애니메이션 추가**: 
  - 신문이 회전하며 떨어지는 진입 애니메이션 (`NewspaperEntranceAnimation`)
  - 타자기 치는 효과의 헤드라인 (`TypewriterText`)
  - 순차적 데이터 로딩 (`FadeInContent`)

### 2. Performance Optimization 🚀
- **`TexturedBackground` 최적화**: 
  - 기존: `Canvas`에서 16만 번의 `drawCircle` 루프를 돌며 노이즈 생성 -> **13초 프리징 원인**
  - 수정: 루프 제거 및 경량화된 그라데이션 레이어로 대체. 앱 구동 즉시 화면 렌더링.

### 3. UX Improvements
- **시네마틱 로딩**: 
  - 데이터가 너무 빨리 로드되어도 최소 3초간 "Unfolding case file...", "Drafting headline..." 등의 문구를 보여주며 분위기 조성.
  - 로딩 상태와 데이터 로딩 상태 분리하여 자연스러운 전환 유도.
- **로고 정리**: 복잡하고 가독성이 떨어지는 이미지 로고 제거, 텍스트 마스트헤드 중심으로 깔끔하게 정리.

## 📸 스크린샷 (Screenshots)
| Before (Text Only) | After (Cinematic UI) |
|:---:|:---:|
| (기존 스크린샷) | (신문 UI 스크린샷) |

## ✅ 리뷰 포인트 (Check List)
- [ ] `ReportDetailScreen` 진입 시 애니메이션이 부드러운가요?
- [ ] 로딩 화면에서 메시지가 주기적으로 잘 바뀌나요?
- [ ] 다양한 화면 크기에서 신문 레이아웃이 깨지지 않나요?
- [ ] 폰트(`Playfair Display`, `Special Elite`)가 정상적으로 적용되었나요?

---
To: @Reviewer (팀원 태그)
