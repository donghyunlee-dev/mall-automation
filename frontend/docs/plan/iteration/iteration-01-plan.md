# Iteration 01 Plan — 프로젝트 기반 설정

> 작성일: 2026-04-01  
> 담당: Sohee Kang  
> 상태: In Progress

---

## 목표

Vite + React + TypeScript strict 프로젝트를 초기화하고, 공통 스타일 토큰, Axios 인스턴스, 라우팅 기반을 구성한다.

---

## 범위

| # | 항목 | 파일 |
|---|---|---|
| 1 | Vite 프로젝트 초기화 (React + TypeScript strict) | `package.json`, `vite.config.ts`, `tsconfig.json`, `index.html` |
| 2 | 디자인 토큰 | `src/styles/_variables.scss` |
| 3 | 전역 리셋 | `src/styles/_reset.scss` |
| 4 | 전역 스타일 진입점 | `src/styles/global.scss` |
| 5 | Axios 인스턴스 + 401 인터셉터 | `src/api/client.ts` |
| 6 | 라우팅 기반 (React Router) | `src/App.tsx` |
| 7 | 진입점 | `src/main.tsx` |
| 8 | 환경변수 | `.env`, `.env.production` |
| 9 | 플레이스홀더 페이지 | `src/pages/LoginPage.tsx`, `src/pages/DashboardPage.tsx` |

---

## 범위 외

- 로그인 폼 UI (Iteration 02)
- 대시보드 컨텐츠 (Iteration 03+)
- 테스트 도구 설치 (Vitest + React Testing Library — Iteration 01 완료 후 별도 설정)

---

## 구현 계획

1. `npm create vite@latest` 로 프로젝트 초기화 — Done
2. `sass`, `axios`, `react-router-dom` 의존성 설치 — Done
3. `tsconfig.json` strict 모드 확인 — Done
4. 스타일 파일 작성 (`_variables.scss`, `_reset.scss`, `global.scss`) — Done
5. `src/api/client.ts` 작성 — Done
6. `src/App.tsx` 라우팅 기반 작성 — Done
7. `src/pages/LoginPage.tsx`, `src/pages/DashboardPage.tsx` 플레이스홀더 작성 — Done
8. `.env`, `.env.production` 작성 — Done
9. `npm run build` 검증 — Done
10. `npm run lint` 검증 — Done

---

## 완료 기준

- `npm run build` 오류 없음
- `npm run lint` 오류 없음
- CSS 변수가 `docs/guide/style-guide.md` 및 목업 기준과 일치

---

## 산출물

- `docs/plan/iteration/iteration-01-plan.md` ← 이 문서
- `docs/test/results/iteration-01-test-result.md`
- `docs/reports/iteration-01-result.md`
