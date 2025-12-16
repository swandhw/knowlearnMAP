# 📘 NotebookLM-style React Application

NotebookLM 스타일의 노트북 관리 및 보고서/슬라이드 생성 웹 애플리케이션입니다.

![React](https://img.shields.io/badge/React-18.3.1-61dafb?logo=react)
![Vite](https://img.shields.io/badge/Vite-6.0-646cff?logo=vite)
![License](https://img.shields.io/badge/License-MIT-green)

## 🚀 빠른 시작

```bash
# 저장소 클론
git clone https://github.com/swandhw/notebook.git
cd notebook

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev
```

브라우저에서 `http://localhost:5173` 접속

## ✨ 주요 기능

### 📊 Studio Panel
- NotebookLM 스타일 카드 기반 UI
- 리포트 & 슬라이드 생성 기능
- 반응형 디자인

### 📝 Report Generation
- 8가지 보고서 형식 제공
- 커스텀 및 템플릿 기반 생성
- 다국어 지원 (한국어, 영어, 일본어, 중국어)

### 📑 Slide Creation
- 자세한 자료 / 발표자 슬라이드 형식
- 길이 조절 (짧게/기본값)
- 맞춤 설명 입력

### 📁 Source Management
- 파일 업로드 지원
- Google Drive, 웹사이트, YouTube 연동
- 소스별 선택 및 검색 기능

## 🛠 기술 스택

- **Frontend**: React 18.3.1
- **Build Tool**: Vite 6.0
- **Styling**: Pure CSS
- **Code Quality**: ESLint

## 📁 프로젝트 구조

```
src/
├── components/
│   ├── NotebookDetail.jsx          # 메인 노트북 페이지
│   ├── ReportGenerationModal.jsx   # 보고서 형식 선택
│   ├── ReportCreationModal.jsx     # 보고서 생성
│   ├── SlideCreationModal.jsx      # 슬라이드 생성
│   └── AddSourceModal.jsx          # 소스 추가
├── App.jsx
└── main.jsx
```

## 📖 협업 가이드

자세한 협업 가이드는 [COLLABORATION_GUIDE.md](./COLLABORATION_GUIDE.md)를 참고하세요.

### Git 워크플로우
```bash
# 새 기능 개발
git checkout -b feature/기능이름
git add .
git commit -m "feat: 기능 설명"
git push origin feature/기능이름
```

## 🎨 디자인 시스템

- **주요 색상**: `#1a73e8` (파란색)
- **배경**: `#fafafa`, `#ffffff`
- **텍스트**: `#202124`, `#5f6368`
- **테두리**: `#e0e0e0`, `#e8eaed`

## 📝 커밋 규칙

- `feat:` - 새로운 기능
- `fix:` - 버그 수정
- `style:` - 코드 스타일링
- `refactor:` - 코드 리팩토링
- `docs:` - 문서 수정

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

MIT License - 자유롭게 사용하세요!

## 👥 개발자

- GitHub: [@swandhw](https://github.com/swandhw)

## 🙏 감사합니다

NotebookLM UI 디자인에 영감을 받았습니다.

---

**Made with ❤️ using React + Vite**
