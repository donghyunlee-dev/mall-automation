# Iteration 02 Plan — 로그인 페이지

> 작성일: 2026-04-01  
> 담당: Sohee Kang  
> 상태: In Progress

---

## 목표

OTP 이메일 인증 기반 로그인 UI를 구현한다. 세션 확인, OTP 발송/확인 API 연동, 2단계 흐름을 포함한다.

---

## 범위

| # | 항목 | 파일 |
|---|---|---|
| 1 | 인증 API 함수 | `src/api/auth.ts` |
| 2 | 인증 타입 | `src/types/auth.ts` |
| 3 | 인증 훅 | `src/hooks/useAuth.ts` |
| 4 | 로그인 페이지 | `src/pages/LoginPage.tsx` |
| 5 | 로그인 스타일 | `src/pages/LoginPage.module.scss` |

---

## 범위 외

- 대시보드 내용 (Iteration 03)
- 토스트 컴포넌트 분리 (Iteration 03+에서 공통화)

---

## 구현 계획

1. `src/types/auth.ts` 타입 정의 — Done
2. `src/api/auth.ts` API 함수 (requestOtp, confirmOtp, getSession) — Done
3. `src/hooks/useAuth.ts` 로그인 흐름 상태 관리 — Done
4. `src/pages/LoginPage.tsx` 2단계 UI 구현 — Done
5. `src/pages/LoginPage.module.scss` 목업 기준 스타일 — Done
6. `npm run build` 검증 — Done
7. `npm run lint` 검증 — Done

---

## 완료 기준

- UI가 `docs/design/mockup-login.html`과 일치
- OTP 발송 → 확인 → `/` 리다이렉트 흐름 정상 동작
- 401 수신 시 `/login` 리다이렉트 정상 동작
- `npm run build` + `npm run lint` 오류 없음
