# 📘 NotebookLM-style React 프로젝트 협업 가이드

## 🔗 프로젝트 정보

- **GitHub 저장소**: https://github.com/swandhw/notebook
- **기술 스택**: React + Vite
- **디자인**: NotebookLM 스타일 UI

---

## 🚀 시작하기

### 1. 프로젝트 클론
```bash
git clone https://github.com/swandhw/notebook.git
cd notebook
```

### 2. 의존성 설치
```bash
npm install
```

### 3. 개발 서버 실행
```bash
npm run dev
```

서버가 실행되면 브라우저에서 `http://localhost:5173` (또는 `http://localhost:5174`) 접속

---

## 📁 프로젝트 구조

```
notebook_react/
├── src/
│   ├── components/
│   │   ├── NotebookDetail.jsx          # 노트북 상세 페이지 (메인)
│   │   ├── NotebookDetail.css          # 노트북 상세 스타일
│   │   ├── AddSourceModal.jsx          # 소스 추가 모달
│   │   ├── AddSourceModal.css
│   │   ├── ReportGenerationModal.jsx   # 보고서 형식 선택 모달
│   │   ├── ReportGenerationModal.css
│   │   ├── ReportCreationModal.jsx     # 보고서 생성 모달
│   │   ├── ReportCreationModal.css
│   │   ├── SlideCreationModal.jsx      # 슬라이드 생성 모달
│   │   ├── SlideCreationModal.css
│   │   └── StudioCardEdit.css          # 스튜디오 카드 편집 버튼 스타일
│   ├── App.jsx                         # 메인 앱 컴포넌트
│   ├── App.css
│   └── main.jsx
├── package.json
└── vite.config.js
```

---

## ✨ 구현된 주요 기능

### 1. 📊 Studio Panel (스튜디오 패널)
- **위치**: 노트북 상세 페이지 우측
- **카드**: 리포트, 슬라이드 2개
- **디자인**: NotebookLM 스타일 카드 레이아웃

### 2. 📝 Report Generation Modal (보고서 생성 모달)
- **트리거**: 리포트 카드 클릭
- **기능**:
  - 8가지 보고서 형식 제공
  - 4열 그리드 레이아웃 (반응형: 태블릿 2열, 모바일 1열)
  - 형식: 직접 만들기, 브리핑 문서, 학습 가이드, 블로그 게시물
  - 추천 형식: 설교 요약본, 목회 서신, 핵심 개념 설명, 이야기 형식 해설

**동작 방식**:
- **직접 만들기**: 카드 클릭 → 템플릿 없는 생성 모달
- **다른 형식**: 카드 클릭 → 모달 닫기 / 편집 버튼(✏️) 클릭 → 템플릿 포함 생성 모달

### 3. 📄 Report Creation Modal (보고서 생성 모달)
- **2가지 변형**:
  - **템플릿 없음**: 직접 만들기 클릭 시
  - **템플릿 포함**: 다른 형식의 편집 버튼 클릭 시
- **기능**: 언어 선택, 커스텀 프롬프트 입력

### 4. 📑 Slide Creation Modal (슬라이드 생성 모달)
- **트리거**: 슬라이드 카드 편집 버튼(✏️) 클릭 (호버 시 우측 상단 표시)
- **기능**:
  - 형식 선택: 자세한 자료 / 발표자 슬라이드
  - 언어 선택
  - 길이 토글: 짧게 / 기본값
  - 맞춤 설명 입력

---

## 🎨 UI/UX 가이드라인

### 색상 팔레트
- **주요 색상**: `#1a73e8` (파란색)
- **배경**: `#fafafa`, `#ffffff`
- **테두리**: `#e0e0e0`, `#e8eaed`
- **텍스트**: `#202124` (진한 회색), `#5f6368` (중간 회색)

### 디자인 원칙
1. **NotebookLM 스타일 일관성 유지**
2. **반응형 디자인** (모바일, 태블릿, 데스크톱)
3. **부드러운 애니메이션** (`transition: all 0.2s ease`)
4. **스크롤 최소화** (모든 모달은 스크롤 없이 화면에 맞춤)
5. **접근성** (`aria-label`, 키보드 네비게이션)

---

## 🔧 개발 가이드

### Git 워크플로우

#### 새 기능 개발
```bash
# 1. 최신 코드 받기
git pull origin master

# 2. 새 브랜치 생성
git checkout -b feature/기능이름

# 3. 작업 후 커밋
git add .
git commit -m "feat: 기능 설명"

# 4. 푸시
git push origin feature/기능이름

# 5. GitHub에서 Pull Request 생성
```

#### 커밋 메시지 규칙
- `feat:` - 새로운 기능
- `fix:` - 버그 수정
- `style:` - 코드 포맷팅, 세미콜론 누락 등
- `refactor:` - 코드 리팩토링
- `docs:` - 문서 수정
- `test:` - 테스트 코드

### 코드 스타일
- **들여쓰기**: 4 spaces
- **따옴표**: 작은따옴표 `'` 사용
- **컴포넌트명**: PascalCase
- **CSS 클래스명**: kebab-case
- **함수명**: camelCase

---

## 🐛 주의사항

### 1. 모달 스크롤 방지
- 모달 내용이 화면을 초과하지 않도록 높이 조정
- `textarea`의 `rows` 속성 최소화
- `padding`, `margin` 최적화

### 2. 이벤트 처리
- 편집 버튼 클릭 시 `e.stopPropagation()` 필수
- 카드 클릭 이벤트와 충돌 방지

### 3. CSS Import
- `@import` 순서 주의
- 모달 스타일은 개별 CSS 파일로 분리

---

## 📞 협업 규칙

### Pull Request (PR) 가이드
1. **PR 제목**: `[기능/수정] 간단한 설명`
2. **내용 포함사항**:
   - 변경 사항 요약
   - 스크린샷 (UI 변경 시)
   - 테스트 방법
3. **리뷰어**: 최소 1명 승인 후 머지

### 이슈 관리
- GitHub Issues 사용
- 라벨: `bug`, `enhancement`, `question`, `documentation`

### 소통
- 중요한 변경사항은 사전 논의
- 코드 리뷰 적극 참여

---

## 📚 참고 자료

### NotebookLM 참고
- https://notebooklm.google.com
- UI/UX 디자인 참고

### React 문서
- https://react.dev

### Vite 문서
- https://vitejs.dev

---

## 🆘 문제 해결

### 1. 포트 충돌
```bash
# vite.config.js에서 포트 변경
export default defineConfig({
  server: {
    port: 3000
  }
})
```

### 2. npm install 오류
```bash
# node_modules 삭제 후 재설치
rm -rf node_modules package-lock.json
npm install
```

### 3. Git 충돌
```bash
# 최신 코드 받기
git pull origin master

# 충돌 해결 후
git add .
git commit -m "fix: Resolve merge conflict"
```

---

## 📝 다음 작업 예정

- [ ] 실제 API 연동
- [ ] 보고서/슬라이드 생성 로직 구현
- [ ] 사용자 인증 추가
- [ ] 데이터 저장 기능
- [ ] 검색 기능 개선

---

**질문이나 문제가 있으면 GitHub Issues에 등록해주세요!**

**Happy Coding! 🚀**
