# ATCS-173 Plan

## Requirement Summary

- 대시보드 상단 로그아웃 동작을 실제 백엔드 API와 연결한다.
- 로그아웃 성공 시 로그인 화면으로 이동한다.

## Business Context

- 현재 로그아웃은 UI placeholder만 있어 세션 종료를 보장하지 못한다.
- 운영 화면에서 계정 전환과 세션 종료를 명확히 지원해야 한다.

## Scope

- `POST /api/auth/logout` 프론트 API 함수 추가
- 로그아웃 요청 훅 추가
- Header 로그아웃 확인 모달에 실제 확인/실패 상태 연결
- 최소 회귀 테스트 추가

## Out Of Scope

- 인증 라우팅 구조 전면 개편
- Protected Route 도입
- 로그인 흐름 리팩터링

## Impacted Modules

- `src/api/auth.ts`
- `src/hooks/`
- `src/components/layout/`
- `src/pages/DashboardPage.tsx`

## Architecture Considerations

- API 호출은 `src/api/` 계층에 둔다.
- 비동기 로그아웃 로직은 커스텀 훅으로 분리한다.
- Header는 UI와 사용자 상호작용 표시만 담당한다.

## Risk Analysis

- 로그아웃 API 401/네트워크 실패 시 사용자가 현재 화면에 남을 수 있다.
- 인증 라우팅 구조는 그대로이므로 화면 전환 깜빡임 문제는 본 작업 범위 밖이다.
