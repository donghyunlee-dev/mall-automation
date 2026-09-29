# ATCS-199 Plan

## Requirement Summary

- 상품 수정 모달에 `availableQty`, `safetyRate` 입력을 추가한다.
- 기존 조회 데이터 값을 표시하고 저장 시 수정된 값을 API에 함께 전달한다.

## Business Context

- 개별 품목 기준을 운영자가 직접 조정할 수 있어야 한다.
- 품목별 가용재고 기준과 잔여율 기준은 품절 판단에 직접 영향을 준다.

## Scope

- 상품 수정 모달에 숫자 입력 2개 추가
- 기존 품목 데이터 표시
- 저장 요청 body에 `availableQty`, `safetyRate` 포함
- 관련 회귀 테스트 추가

## Out Of Scope

- 상품 추가 모달 변경
- 목록 테이블 컬럼 추가
- 배치/품절 판단 로직 변경

## Impacted Modules

- `src/components/modals/ProductEditModal.tsx`
- `src/types/goods.ts`
- `src/components/modals/ProductEditModal.test.tsx`

## Architecture Considerations

- 기존 `item: GoodsItem` props에 이미 값이 있으므로 추가 조회 API는 필요 없다.
- API 호출 계층은 기존 `updateGoods`를 유지하고 body 타입만 확장한다.

## Risk Analysis

- 숫자 입력은 빈 값/0값 처리에서 문자열-숫자 변환 오류가 날 수 있다.
- 기존 ERP 잠금 로직과 같이 동작하므로 저장 가능 조건을 깨지 않도록 확인이 필요하다.
