# ATCS-178 Tasks

## Development Tasks

- [ ] ERP 연동 상태 기반 입력 비활성화 처리
- [ ] 비활성화 안내 문구 추가
- [ ] 회귀 테스트 추가

## Test Cases

- [ ] `erpSyncYn=Y` 면 ERP 품목코드 입력이 비활성화된다
- [ ] `erpSyncYn=N` 면 ERP 품목코드 입력이 활성화된다

## Modules To Modify

- `src/components/modals/ProductEditModal.tsx`
- `src/components/modals/Modal.module.scss`
- `src/components/modals/ProductEditModal.test.tsx`

## Integration Points

- 상품 목록에서 전달되는 `GoodsItem`
- 수정 모달 저장 흐름

## Result

### Changed Files

- `docs/tasks/ATCS-178/plan.md`
- `docs/tasks/ATCS-178/spec.md`
- `docs/tasks/ATCS-178/tasks.md`
- `src/components/modals/ProductEditModal.tsx`
- `src/components/modals/Modal.module.scss`
- `src/components/modals/ProductEditModal.test.tsx`

### Summary

- `erpSyncYn === 'Y'` 인 품목은 ERP 품목코드 입력을 비활성화했다.
- ERP 연동 완료 상태를 사용자에게 안내하는 문구를 추가했다.
- ERP 연동 미완료 품목은 기존과 동일하게 수정 가능하다.

### Test Results

- `npm test -- --run` ✅ 통과
- `npm run build` ✅ 통과
- `npm run lint` ✅ 통과

### Review Notes

- 현재 모달은 전달받은 `item` 데이터의 `erpSyncYn` 값을 기준으로 판단한다.
- 별도 상세 조회 API 호출은 본 작업 범위에 포함하지 않았다.
