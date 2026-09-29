# ATCS-213 Tasks

## Development Tasks

- [x] 현재 사용자 정보 타입과 API 함수 추가
- [x] 사용자 알림 설정 전용 훅 추가
- [x] AvatarDropdown에 이메일/Teams 알림 라디오 UI 추가
- [x] 변경 즉시 저장 로직 및 오류 처리 추가
- [x] 관련 테스트 추가

## Test Cases

- [x] 현재 사용자 응답값으로 라디오 초기 상태가 반영된다
- [x] 이메일 알림 라디오 변경 시 알림 설정 API가 호출된다
- [x] Teams 알림 라디오 변경 시 알림 설정 API가 호출된다
- [x] 저장 실패 시 오류 메시지가 표시된다

## Modules To Modify

- `src/types/auth.ts`
- `src/api/auth.ts`
- `src/hooks/useUserNotifications.ts`
- `src/components/layout/AvatarDropdown.tsx`
- `src/components/layout/AvatarDropdown.module.scss`
- `src/components/layout/Header.tsx`
- `src/pages/DashboardPage.tsx`
- `src/components/layout/AvatarDropdown.test.tsx`

## Integration Points

- `GET /api/auth/me`
- `PUT /api/auth/me/notifications`
- Header > AvatarDropdown

## Result

### Changed Files

- `docs/tasks/ATCS-213/plan.md`
- `docs/tasks/ATCS-213/spec.md`
- `docs/tasks/ATCS-213/tasks.md`
- `src/types/auth.ts`
- `src/api/auth.ts`
- `src/hooks/useUserNotifications.ts`
- `src/hooks/useUserNotifications.test.tsx`
- `src/components/layout/AvatarDropdown.tsx`
- `src/components/layout/AvatarDropdown.module.scss`
- `src/components/layout/AvatarDropdown.test.tsx`
- `src/components/layout/Header.tsx`
- `src/components/layout/Header.test.tsx`
- `src/pages/DashboardPage.tsx`

### Summary

- 현재 사용자 조회 및 알림 설정 수정 API를 프론트에 연결했다.
- AvatarDropdown에 이메일/Teams 알림 수신 여부 라디오 버튼을 추가하고, 변경 시 즉시 저장되도록 구현했다.
- 저장 실패 시 이전 값으로 롤백하고 오류 메시지를 드롭다운에 표시하도록 처리했다.

### Test Results

- `npm test -- --run` ✅ 통과
- `npm run build` ✅ 통과
- `npm run lint` ✅ 통과

### Review Notes

- 사용자 기존 수정인 `src/components/modals/ProductEditModal.tsx`는 건드리지 않았다.
- 저장 로직은 `DashboardPage`에서 훅을 통해 조합하고, `AvatarDropdown`은 UI와 이벤트 전달만 담당한다.
