# ATCS-213 Plan

## Requirement Summary

- 우측 상단 사용자 드롭다운에 이메일 알림 여부와 Teams 알림 여부 설정 UI를 추가한다.
- 각 설정은 라디오 버튼으로 표현하고, 변경 즉시 API를 호출해 현재 사용자 계정 정보를 업데이트한다.

## Business Context

- 배치 완료 후 상태 변경 결과를 어떤 채널로 받을지 사용자가 직접 제어해야 한다.
- 백엔드에 현재 사용자 조회 및 알림 설정 수정 API가 추가되어 프론트 연결이 필요한 상태다.

## Scope

- 현재 사용자 정보 조회 API 연동
- 이메일/Teams 알림 설정 타입 정의 추가
- 드롭다운 내 라디오 버튼 UI 추가
- 설정 변경 시 즉시 저장 및 오류 처리
- 관련 컴포넌트/훅 테스트 추가

## Out Of Scope

- 배치 간격 설정 동작 변경
- 로그아웃 흐름 변경
- 백엔드 API/DB 스키마 변경
- Teams 웹훅 발송 로직 변경

## Impacted Modules

- `src/types/auth.ts`
- `src/api/auth.ts`
- `src/hooks/useUserNotifications.ts`
- `src/components/layout/AvatarDropdown.tsx`
- `src/components/layout/AvatarDropdown.module.scss`
- `src/components/layout/Header.tsx`
- `src/pages/DashboardPage.tsx`
- `src/components/layout/AvatarDropdown.test.tsx`

## Architecture Considerations

- 컴포넌트에 API 호출 로직을 두지 않고, 사용자 알림 설정 상태는 전용 훅으로 캡슐화한다.
- Header와 AvatarDropdown은 UI/이벤트 전달만 담당하고, 실제 상태 갱신은 DashboardPage에서 조합한다.

## Risk Analysis

- 초기 사용자 정보 조회 전 라디오 상태가 불안정하면 잘못된 값이 잠깐 보일 수 있다.
- 변경 시 즉시 저장이므로 API 실패 시 UI와 서버 상태가 불일치할 수 있어 롤백 또는 재동기화가 필요하다.
- 드롭다운 안에 배치 간격 설정과 알림 설정이 함께 들어가므로 레이아웃이 복잡해질 수 있다.
