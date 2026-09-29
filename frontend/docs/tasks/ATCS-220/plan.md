# ATCS-220 Plan

## Requirement Summary

- 상품 수정 모달에서 구분을 바꾸면 선택한 구분의 상태 기준 값을 즉시 반영한다.
- 상태 기준 설정 저장 후 상품 목록을 다시 불러와 이후 수정 팝업이 최신 기준 값을 사용하게 한다.

## Business Context

- 운영자가 범용/전용 전환과 상태 기준 설정을 함께 사용한다.
- 기준 값이 화면과 저장 payload에 즉시 반영되지 않으면 잘못된 재고 기준이 서버에 저장될 수 있다.

## Scope

- `ProductEditModal`의 구분 변경 동작 수정
- 상품 목록 재조회 흐름 보강
- 관련 회귀 테스트 추가

## Out Of Scope

- 백엔드 기준 재계산 로직 변경
- 상품 추가/엑셀 업로드 UI 구조 변경
- 상태 기준 설정 API 스펙 변경

## Impacted Modules

- `src/components/modals/ProductEditModal.tsx`
- `src/pages/DashboardPage.tsx`
- `src/hooks/useGoods.ts`
- `src/components/modals/ProductEditModal.test.tsx`

## Architecture Considerations

- 상품 목록 갱신은 `useGoods`가 단일 진입점으로 유지한다.
- 구분 변경 시 기준 값 반영은 수정 모달 내부에서 명시적으로 처리한다.

## Risk Analysis

- 자동 반영이 기존 수동 입력을 의도치 않게 덮어쓸 수 있으므로 구분 변경 시점에만 실행한다.
- 재조회 시점이 비동기 완료 전에 종료되면 stale data 문제가 남을 수 있으므로 `reload`를 await 가능하게 만든다.
