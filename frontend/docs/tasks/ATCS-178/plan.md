# ATCS-178 Plan

## Requirement Summary

- 상품 수정 모달에서 ERP 연동이 완료된 품목은 ERP 품목코드를 수정할 수 없게 한다.
- ERP 연동 미완료 품목만 ERP 품목코드 입력을 허용한다.

## Business Context

- `erpSyncYn` 이 `Y`인 품목은 이미 ERP 코드가 검증된 상태다.
- 검증 완료 후 ERP 코드를 수정하면 이후 재고 연동 기준이 깨질 수 있다.

## Scope

- 상품 수정 모달의 ERP 품목코드 입력 비활성화 처리
- 상태 안내 문구 노출
- 최소 회귀 테스트 추가

## Out Of Scope

- 배치 로직 변경
- 상품 상세 조회 구조 변경
- 수정 API 스펙 변경

## Impacted Modules

- `src/components/modals/ProductEditModal.tsx`
- `src/components/modals/Modal.module.scss`
- `src/components/modals/ProductEditModal.test.tsx`

## Architecture Considerations

- 비활성화 판단은 이미 보유 중인 `GoodsItem.erpSyncYn` 값을 사용한다.
- 추가 API 호출 없이 현재 수정 모달 props 기반으로 처리한다.

## Risk Analysis

- 목록 데이터와 상세 데이터가 나중에 분리되면 판단 기준도 상세 조회 기준으로 옮겨야 한다.
- 비활성화만 추가하고 저장 로직은 그대로 두므로 다른 필드 수정 동작은 유지해야 한다.
