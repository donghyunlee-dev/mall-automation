# ATCS-217 Tasks

## Development Tasks

- Add common status config frontend types.
- Add API functions for loading and updating common status config.
- Implement a dashboard-scoped hook for config load/save.
- Add a settings modal and entry button on the dashboard.
- Show config reference text in product add/edit modals.

## Test Cases To Implement

- Hook loads config and derives save payload correctly.
- Hook keeps previous state when save fails.
- Product add/edit modal renders scope-based reference text.

## Modules To Modify

- `src/types`
- `src/api`
- `src/hooks`
- `src/pages`
- `src/components/modals`

## New Classes Or Components

- `useCommonStatusConfig`
- `StatusConfigModal`

## Integration Points

- Dashboard product-list action area
- Product add modal
- Product edit modal
- Backend common status config APIs

## Result

- Changed files:
  - `src/types/commonStatusConfig.ts`
  - `src/api/commonStatusConfig.ts`
  - `src/hooks/useCommonStatusConfig.ts`
  - `src/hooks/useCommonStatusConfig.test.tsx`
  - `src/components/modals/StatusConfigModal.tsx`
  - `src/components/modals/ProductAddModal.tsx`
  - `src/components/modals/ProductAddModal.test.tsx`
  - `src/components/modals/ProductEditModal.tsx`
  - `src/components/modals/ProductEditModal.test.tsx`
  - `src/components/modals/Modal.module.scss`
  - `src/pages/DashboardPage.tsx`
  - `docs/tasks/ATCS-217/plan.md`
  - `docs/tasks/ATCS-217/spec.md`
  - `docs/tasks/ATCS-217/tasks.md`
- Summary:
  - 대시보드에 `상태 기준 설정` 버튼과 설정 모달을 추가해 `GENERAL`, `EXCLUSIVE` 공통 판매 상태 기준을 조회/수정할 수 있게 했다.
  - 프론트에 Common Status Config 타입, API, 훅을 추가했고 저장 시 `판매 기준 수량 = 품절 기준 수량 + 1` 규칙을 고정 적용했다.
  - 상품 추가/수정 모달에서 현재 선택된 구분 기준의 참조 문구를 표시하도록 연결했다.
  - PR 피드백을 반영해 설정 모달을 가로 배치 테이블 구조로 바꾸고 중복 안내 문구를 제거했다.
  - 추가 요청에 맞춰 `구분`을 세로 행으로, 기준 항목을 가로 열로 재배치하고 모달 폭과 표 구조를 조정해 스크롤 없이 표시되도록 수정했다.
- Test results:
  - `npm test -- --run src/hooks/useCommonStatusConfig.test.tsx src/components/modals/ProductAddModal.test.tsx src/components/modals/ProductEditModal.test.tsx`
  - `npm run build`
  - `npm run lint`
- Review notes:
  - 백엔드 API 및 DB 스키마 변경은 없었다.
  - 초기 PR `#71`의 디자인 피드백을 반영하는 후속 수정이 포함됐다.
  - 머지된 PR에 마지막 가로/세로 재배치 수정이 누락돼 별도 후속 PR이 필요하다.
