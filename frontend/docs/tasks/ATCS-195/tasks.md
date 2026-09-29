# ATCS-195 Tasks

## Development Tasks
- [ ] ProductTable.module.scss에 `.bErrS` 클래스 추가 (background: #d97706, color: #fff)
- [ ] ProductTable.tsx 판매 상태 뱃지 조건 분기 수정 (status === 'ERROR' → 확인불가)
- [ ] ProductTable 테스트 업데이트

## Modules to Modify
- `src/components/dashboard/ProductTable.tsx`
- `src/components/dashboard/ProductTable.module.scss`

## Test Cases
- status: ERROR → 확인불가 뱃지 렌더링
- status: NORMAL, soldOutYn: N → 판매중 뱃지 렌더링
- status: NORMAL, soldOutYn: Y → 품절 뱃지 렌더링
- status: ERROR, soldOutYn: Y → 확인불가 (soldOutYn 무시)

---

## Result

### Changed Files
- `src/components/dashboard/ProductTable.tsx`
- `src/components/dashboard/ProductTable.module.scss`

### Summary
ProductTable.tsx에 status === 'ERROR' 조건 분기 추가, ProductTable.module.scss에 .bErrS 앰버 뱃지 스타일 추가.

### Test Results
- ProductTable 전용 테스트 파일 없음
- `npm run build` 성공 (tsc + vite build 통과)

### Review Notes
-
