# ATCS-213 Spec

## Feature Specification

- AvatarDropdown 헤더 아래에 사용자 계정 알림 설정 섹션을 추가한다.
- 섹션에는 다음 항목을 포함한다.
  - `이메일 알림 여부`
  - `Teams 알림 여부`
- 각 항목은 `수신` / `미수신` 라디오 버튼으로 구성한다.
- 드롭다운이 렌더링되기 전에 `GET /api/auth/me`로 현재 사용자 설정을 조회한다.
- 라디오 선택이 바뀌면 `PUT /api/auth/me/notifications`를 즉시 호출한다.
- 저장 성공 시 응답값으로 UI 상태를 동기화한다.

## API Changes

- `GET /api/auth/me`
  - 응답: `email`, `emailNotificationYn`, `teamsNotificationYn`, `lastLoginAt`, `createdAt`, `updatedAt`
- `PUT /api/auth/me/notifications`
  - 요청: `emailNotificationYn`, `teamsNotificationYn`
  - 응답: 현재 사용자 정보 전체

## Database Changes

- 없음

## Domain Model Changes

- `CurrentUserResponse` 프론트 타입 추가
- `UpdateUserNotificationsBody` 프론트 타입 추가

## Validation Rules

- 알림 플래그는 `Y` 또는 `N`만 허용한다.
- 라디오 변경 시 선택한 한 항목뿐 아니라 현재 두 항목의 최종 조합을 함께 전송한다.
- 저장 중에는 중복 요청을 막기 위해 해당 라디오 그룹을 비활성화한다.

## Edge Cases

- 초기 조회 실패 시 기존 이메일 표시는 유지하고, 알림 설정 UI에는 오류 메시지를 보여준다.
- 한 항목 저장 실패 시 마지막 정상 응답값으로 UI를 복원한다.
- `GET /api/auth/me`의 이메일은 기존 세션 이메일과 동일해야 하며, 표시 이메일은 현재 사용자 응답을 우선 사용한다.

## Exception Scenarios

- `401`이면 기존 인터셉터 흐름에 따라 로그인 페이지로 이동한다.
- `404` 또는 `400`이면 드롭다운 내 오류 문구를 표시하고 사용자가 다시 시도할 수 있어야 한다.

## Acceptance Criteria Mapping

- 사용자 계정 정보 이메일/Teams 알림 컴포넌트 확인: AvatarDropdown에 새 설정 섹션 추가
- 라디오 버튼 구성 확인: 각 설정별 `수신` / `미수신` 라디오 렌더링
- 변경 시마다 API 호출: 전용 훅의 즉시 저장 로직
- 200 응답 처리: 응답값으로 로컬 상태 갱신 및 오류 해제
