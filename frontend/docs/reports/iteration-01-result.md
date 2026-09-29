# Iteration 01 Result — 프로젝트 기반 설정

> 날짜: 2026-04-01  
> 담당: Sohee Kang  
> 상태: ✅ 완료

---

## 요약

Vite + React + TypeScript strict 기반 프로젝트를 초기화하고, 공통 스타일 토큰, Axios 인스턴스, 라우팅 기반 구성을 완료했다. `npm run build` / `npm run lint` 오류 없이 통과.

---

## 변경 파일

| 파일 | 내용 |
|---|---|
| `package.json` | 프로젝트 설정, 스크립트, 의존성 목록 |
| `vite.config.ts` | Vite 설정 + Vitest 설정 |
| `tsconfig.json` | TypeScript 프로젝트 참조 설정 |
| `tsconfig.app.json` | TypeScript strict 모드, Vite 타입 포함 |
| `tsconfig.node.json` | vite.config.ts 전용 TypeScript 설정 |
| `index.html` | 진입점 HTML |
| `.env` | 로컬 개발 환경변수 (`VITE_API_BASE_URL`) |
| `.env.production` | 운영 환경변수 템플릿 |
| `eslint.config.js` | ESLint 설정 (TypeScript, React Hooks, no-any) |
| `src/vite-env.d.ts` | Vite 타입 레퍼런스 + SCSS 모듈 선언 |
| `src/main.tsx` | React 진입점, global.scss import |
| `src/App.tsx` | BrowserRouter + Routes (`/login`, `/`) |
| `src/styles/_variables.scss` | CSS 변수 디자인 토큰 (style-guide.md 기준) |
| `src/styles/_reset.scss` | 전역 CSS 리셋 |
| `src/styles/global.scss` | `@use variables`, `@use reset` |
| `src/api/client.ts` | Axios 인스턴스, `withCredentials: true`, 401 인터셉터 |
| `src/pages/LoginPage.tsx` | 플레이스홀더 (Iteration 02에서 구현) |
| `src/pages/DashboardPage.tsx` | 플레이스홀더 (Iteration 03에서 구현) |
| `src/test/setup.ts` | Vitest setup — `@testing-library/jest-dom` |

---

## 실행 명령

```bash
npm run build   # ✅ 통과
npm run lint    # ✅ 통과
```

---

## 핵심 결정 사항

- TypeScript strict 모드 유지 (`noUnusedLocals`, `noUnusedParameters` 포함)
- ESLint 규칙에 `@typescript-eslint/no-explicit-any: error` 추가 — `any` 타입 사용 차단
- Axios 인터셉터에서 `/api/auth/` 경로 제외 로직으로 무한 리다이렉트 방지
- Vitest + React Testing Library 설치 완료 (testing-strategy.md 요구사항 반영)

---

## Next Steps (현재 Iteration 범위 외)

- Iteration 02: 로그인 페이지 (`LoginPage.tsx`) OTP 인증 UI 구현
- `.env.production`의 `EC2_HOST`, `PORT` 값 실제 배포 시 설정 필요
