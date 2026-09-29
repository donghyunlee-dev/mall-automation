# ATCS-173 Spec

## Feature Specification

- 사용자가 AvatarDropdown에서 `로그아웃`을 누르면 확인 모달이 열린다.
- 확인 모달에서 `로그아웃`을 누르면 `POST /api/auth/logout`을 호출한다.
- 응답이 `204 No Content`이면 `/login`으로 이동한다.
- 실패 시 모달 내 오류 메시지를 보여주고 현재 화면에 유지한다.

## API Changes

- 신규 사용 API: `POST /api/auth/logout`
- 응답 본문 없음

## Database Changes

- 없음

## Domain Model Changes

- 없음

## Validation Rules

- 중복 로그아웃 요청은 막는다.
- 요청 진행 중에는 확인 버튼과 취소 버튼을 비활성화한다.

## Edge Cases

- 네트워크 오류 시 오류 메시지를 노출한다.
- 이미 세션이 만료된 상태의 401 응답은 로그인 화면 이동으로 처리한다.

## Exception Scenarios

- 401: 세션이 이미 유효하지 않음, 로그인 화면으로 이동
- 기타 실패: `로그아웃에 실패했어요. 다시 시도해주세요.` 표시

## Acceptance Criteria Mapping

- 로그아웃 클릭 시 backend API 호출: `src/api/auth.ts`, `src/hooks/useLogout.ts`
- 대시보드에서 로그인 화면 이동: `src/hooks/useLogout.ts`
- 세션 정리/새로고침 시 로그인 화면: 백엔드 세션 무효화 + `/login` 이동
