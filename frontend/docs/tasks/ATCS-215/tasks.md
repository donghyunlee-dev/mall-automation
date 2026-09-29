# ATCS-215 Tasks

## Development Tasks

- 로그인 페이지의 불필요한 문구와 버튼 아이콘을 제거한다.
- 헤더에서 갱신 시간 제거, 자동 실행 pill 위치 이동, 동기화 버튼 문구 변경을 적용한다.
- 계정 드롭다운의 자동 실행 관련 문구를 운영 용어로 정리한다.
- 오늘 상태 변경 카드 문구를 운영 용어로 변경한다.
- 상품 목록 테이블에서 동기화 일자 컬럼을 제거하고 레이아웃을 맞춘다.
- 상품 추가/엑셀 업로드 관련 문구와 선택 강조 스타일을 수정한다.
- 닫힌 PR 후속 코멘트에 맞춰 최근 동기화 시간 카드 제목과 상품 추가/수정 구분 보조 문구를 정리한다.
- 관련 테스트를 최소 범위로 보강한다.

## Test Cases To Implement

- 헤더가 갱신 시간을 노출하지 않고 `상태 동기화` 버튼을 보여주는지 확인
- 계정 드롭다운이 `자동 실행 설정`, `실행 시간` 문구를 노출하는지 확인

## Modules To Modify

- `src/pages/LoginPage.tsx`
- `src/pages/DashboardPage.tsx`
- `src/components/layout/Header.tsx`
- `src/components/layout/AvatarDropdown.tsx`
- `src/components/dashboard/SummaryCards.tsx`
- `src/components/dashboard/ProductTable.tsx`
- `src/components/modals/ProductAddModal.tsx`
- `src/components/modals/ProductEditModal.tsx`
- `src/components/modals/ExcelUploadModal.tsx`
- 관련 스타일/테스트 파일

## New Classes Or Components

- 없음

## Integration Points

- 기존 `useAuth`, `useBatch`, `useGoods` 상태 흐름 유지
- 기존 모달 열기/닫기 및 토스트 흐름 유지

## Result

### Changed Files

- `docs/tasks/ATCS-215/plan.md`
- `docs/tasks/ATCS-215/spec.md`
- `docs/tasks/ATCS-215/tasks.md`
- `src/pages/LoginPage.tsx`
- `src/pages/DashboardPage.tsx`
- `src/components/layout/Header.tsx`
- `src/components/layout/Header.module.scss`
- `src/components/layout/Header.test.tsx`
- `src/components/layout/AvatarDropdown.tsx`
- `src/components/layout/AvatarDropdown.test.tsx`
- `src/components/dashboard/SummaryCards.tsx`
- `src/components/dashboard/ProductTable.tsx`
- `src/components/modals/ExcelUploadModal.tsx`
- `src/components/modals/ProductAddModal.tsx`
- `src/components/modals/ProductEditModal.tsx`
- `src/components/modals/Modal.module.scss`

### Summary

- 로그인 화면의 안내 문구와 버튼 아이콘을 정리하고 인증 성공 문구를 짧게 변경했다.
- 헤더에서 갱신 시간을 제거하고 자동 실행 pill을 상태 동기화 버튼 왼쪽으로 이동했으며 버튼 문구를 변경했다.
- 계정 드롭다운, 오늘 상태 변경 카드, 최근 동기화 시간 카드, 엑셀 업로드, 상품 목록, 상품 추가/수정 구분 UI를 Jira 요구사항과 후속 코멘트에 맞게 수정했다.

### Test Results

- `npm run build` 통과
- `npm run lint` 통과
- `npm test -- --run src/components/layout/Header.test.tsx src/components/layout/AvatarDropdown.test.tsx src/components/modals/ProductEditModal.test.tsx` 통과
- Playwright 브라우저 확인 시 `http://localhost:8080/api/auth/session` CORS 오류로 로그인 화면의 실제 세션 확인 이후 단계는 검증하지 못함

### Review Notes

- 닫힌 PR #68 코멘트의 후속 수정 사항을 반영했다.
