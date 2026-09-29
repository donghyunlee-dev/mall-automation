# ATCS-210 Plan

## Requirement Summary

- 전체 상품 목록의 판매 상태는 ERP 연동 여부가 아니라 고도몰 연동 결과를 기준으로 표시한다.
- `godoSyncYn`이 `N`인 경우에만 `확인불가`로 본다.
- `godoSyncYn`이 `Y`이면 `soldOutYn` 값에 따라 `품절` 또는 `판매중`을 표시한다.

## Business Context

- 판매 상태는 고도몰 품목 상태를 반영하는 값이다.
- ERP 연동 상태는 별도의 코드 검증 상태 컬럼으로 이미 표현되고 있으므로 판매 상태 판단에 섞이면 안 된다.

## Scope

- 상품 목록 판매 상태 뱃지 기준 수정
- 상품 목록 상태 필터 기준 수정
- 대시보드 품절 집계 기준 수정
- 관련 회귀 테스트 추가

## Out Of Scope

- 코드 검증 상태 컬럼 정의 변경
- 백엔드 API 또는 DB 스키마 변경
- 상품 수정 모달 변경

## Impacted Modules

- `src/components/dashboard/ProductTable.tsx`
- `src/hooks/useGoods.ts`
- `src/pages/DashboardPage.tsx`
- `src/utils/goodsSaleStatus.ts`
- `src/utils/goodsSaleStatus.test.ts`

## Architecture Considerations

- 판매 상태 기준이 테이블, 필터, 집계에 중복되어 있으므로 공용 유틸로 통일한다.
- ERP 검증 상태와 판매 상태의 책임을 분리해 향후 표시 오류를 줄인다.

## Risk Analysis

- 필터 기준과 테이블 표시 기준이 다르게 남으면 동일 상품이 상충된 상태로 보일 수 있다.
- 요약 카드 집계가 기존 로직을 유지하면 테이블 결과와 숫자가 달라질 수 있다.
