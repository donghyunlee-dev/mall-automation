# ATCS-173 Tasks

## Development Tasks

- [ ] 로그아웃 API 함수 추가
- [ ] 로그아웃 비즈니스 로직 훅 추가
- [ ] Header 확인 모달을 실제 동작으로 연결
- [ ] DashboardPage와 로그아웃 훅 연결
- [ ] 테스트 추가 및 검증

## Test Cases

- [ ] 로그아웃 메뉴 클릭 후 확인 모달이 열린다
- [ ] 확인 클릭 시 로그아웃 콜백이 실행된다
- [ ] 실패 메시지가 모달에 표시된다

## Modules To Modify

- `src/api/auth.ts`
- `src/hooks/useLogout.ts`
- `src/components/layout/Header.tsx`
- `src/components/layout/AvatarDropdown.tsx`
- `src/components/layout/Header.module.scss`
- `src/pages/DashboardPage.tsx`

## Integration Points

- Auth API
- Header dropdown
- Dashboard page session UX

## Result

### Changed Files

- `docs/product/data-spec.md`
- `docs/tasks/ATCS-173/plan.md`
- `docs/tasks/ATCS-173/spec.md`
- `docs/tasks/ATCS-173/tasks.md`
- `src/api/auth.ts`
- `src/hooks/useLogout.ts`
- `src/components/layout/AvatarDropdown.tsx`
- `src/components/layout/Header.tsx`
- `src/components/layout/Header.module.scss`
- `src/components/layout/Header.test.tsx`
- `src/pages/DashboardPage.tsx`

### Summary

- 로그아웃 API 함수를 추가했다.
- 로그아웃 비즈니스 로직을 `useLogout` 훅으로 분리했다.
- Header 로그아웃 확인 모달을 실제 API 호출, 로딩, 오류 표시와 연결했다.
- 성공 시 `/login` 이동, 401 응답도 로그인 화면 이동으로 처리했다.

### Test Results

- `npm test -- --run` ✅ 통과
- `npm run build` ✅ 통과
- `npm run lint` ✅ 통과

### Review Notes

- 현재 인증 라우팅 구조의 초기 깜빡임 문제는 본 작업 범위에 포함하지 않았다.
- 프로덕션 번들 크기 경고는 기존 구조 영향으로 남아 있다.
