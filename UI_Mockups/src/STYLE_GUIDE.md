# ARKHAM LOG 스타일 가이드

이 문서를 AI에게 복사해서 제공하면 동일한 스타일로 새로운 화면/컴포넌트를 만들 수 있습니다.

---

## 프롬프트 템플릿

```
다음 ARKHAM LOG 디자인 시스템을 사용해서 [원하는 화면/기능]을 만들어줘:

**색상 팔레트:**
- Primary Accent: #8B0000 (Blood Wine Red)
- Main Background: #0a0a0a (Deep Black)
- Card Background: #1a1a1a (Charcoal Grey)
- Borders: #2a2a2a (Dark Grey)
- Secondary Text: #666666 (Medium Grey)
- Body Text: #999999 (Light Grey)
- Headings: #ffffff (White)

**타이포그래피:**
- H1: text-[#8B0000] tracking-[0.25em]
- H2: text-white tracking-wide
- H3: text-white tracking-wide
- Body: text-[#999999] text-sm
- Labels: text-[#8B0000] text-xs tracking-[0.3em]
- Small Text: text-[#666666] text-xs tracking-wide

**컴포넌트 스타일:**
- Primary Button: bg-[#8B0000] hover:bg-[#A00000] text-white px-6 py-3 rounded-xl transition-colors
- Card: bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f] border border-[#2a2a2a] rounded-2xl p-4
- Icon Button: p-3 hover:bg-white/5 rounded-full transition-colors
- Hover Border: hover:border-[#8B0000]

**레이아웃:**
- Border Radius: rounded-xl (12px), rounded-2xl (16px)
- Container Padding: px-6
- Card Gap: gap-3
- Section Spacing: mb-8

**효과:**
- Transitions: transition-colors, transition-all duration-300
- Gradients: bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f]
- Background Overlays: bg-gradient-to-t from-[#0a0a0a] via-black/60 to-transparent

**디자인 원칙:**
- Material 3 기반의 현대적인 UI
- 1920s 오컬트/러브크래프트 공포 분위기
- 어두운 배경에 혈색 강조
- 넓은 자간(letter-spacing)으로 고딕/신비로운 느낌
```

---

## 사용 예시

### 새 화면 만들기
```
ARKHAM LOG 스타일로 프로필 설정 화면을 만들어줘. 
사용자 아바타, 이름, 이메일 입력 필드, 저장 버튼이 필요해.
```

### 새 컴포넌트 만들기
```
ARKHAM LOG 스타일로 알림 카드 컴포넌트를 만들어줘.
아이콘, 제목, 설명, 시간이 표시되어야 해.
```

### 기존 코드 수정하기
```
이 코드를 ARKHAM LOG 스타일로 바꿔줘:
[기존 코드 붙여넣기]
```

---

## 빠른 참조 - 자주 쓰는 클래스

### 배경
- `bg-[#0a0a0a]` - 메인 배경
- `bg-gradient-to-br from-[#1a1a1a] to-[#0f0f0f]` - 카드 그라데이션
- `bg-[#8B0000]` - 강조색 배경

### 텍스트
- `text-[#8B0000]` - 강조 텍스트
- `text-white` - 제목
- `text-[#999999]` - 본문
- `text-[#666666]` - 보조 텍스트

### 보더
- `border border-[#2a2a2a]` - 기본 보더
- `hover:border-[#8B0000]` - 호버시 강조
- `border-b-2 border-[#8B0000]` - 헤더 하단 강조선

### 자간
- `tracking-[0.25em]` - 메인 타이틀 (가장 넓음)
- `tracking-wide` - 일반 제목
- `tracking-wider` - 라벨

### 라운드
- `rounded-xl` - 버튼, 작은 카드
- `rounded-2xl` - 큰 카드
- `rounded-full` - 아이콘 버튼, 배지

---

## 핵심 요소

1. **어두운 배경**: 항상 #0a0a0a 또는 #1a1a1a 사용
2. **혈색 강조**: 중요한 요소에 #8B0000 사용
3. **넓은 자간**: 모든 텍스트에 tracking 적용
4. **그라데이션 카드**: from-[#1a1a1a] to-[#0f0f0f]
5. **부드러운 전환**: transition-colors 또는 duration-300
6. **Material 3 라운드**: rounded-xl, rounded-2xl 사용

---

## ❌ 피해야 할 것

- 밝은 배경색 (흰색, 회색)
- 파란색, 초록색 등 다른 Primary 색상
- 작은 자간 (tracking-tight)
- 각진 모서리 (rounded-none)
- 네온/밝은 색상 효과
