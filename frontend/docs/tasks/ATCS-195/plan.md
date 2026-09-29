# ATCS-195 Plan

## Requirement Summary
상품 목록의 판매 상태 뱃지에 "확인불가" 상태를 추가한다.
현재 판매중 / 품절 두 가지만 존재하며, status: ERROR인 상품도 판매중으로 표시되는 문제를 해결한다.

## Business Context
배치 동기화 시 WMS에서 ERP 품목 코드를 찾지 못하면 GoodsStatus.ERROR로 설정된다.
이 경우 재고 확인 자체가 불가능하지만 soldOutYn이 "N"으로 유지되어 운영자가 판매중으로 오인할 수 있다.
운영자가 코드 오류 품목을 즉시 인지할 수 있도록 별도 상태 표시가 필요하다.

## Scope
- ProductTable.tsx: 판매 상태 뱃지 렌더링 로직 수정
- ProductTable.module.scss: 확인불가 뱃지 스타일 추가

## Out of Scope
- 백엔드 API 변경 없음
- GoodsStatus enum 변경 없음
- 필터 기능 변경 없음 (별도 티켓)

## Impacted Modules
- `src/components/dashboard/ProductTable.tsx`
- `src/components/dashboard/ProductTable.module.scss`

## Architecture Considerations
API 응답의 `status` 필드(NORMAL / ERROR / UNVERIFIED / HIDE)를 기반으로 프론트에서 파생 표시.
백엔드 변경 없이 프론트 렌더링 조건만 추가하는 최소 변경.

## Risk Analysis
- 낮음: 렌더링 조건 추가만으로 기존 로직에 영향 없음
- status가 ERROR이면 soldOutYn 값과 무관하게 확인불가 표시 — 기존 판매중/품절 경로는 그대로 유지
