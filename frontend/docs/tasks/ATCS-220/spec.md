# ATCS-220 Spec

## Feature Specification

- 수정 모달은 현재 상품의 `onlyMallYn`에 맞는 기준 안내 문구를 보여준다.
- 사용자가 수정 모달에서 구분을 변경하면 해당 scope의 `outOfStockQty`를 `availableQty`에, `safetyRate`를 `safetyRate` 입력값에 즉시 반영한다.
- 상태 기준 설정 저장이 끝나면 상품 목록을 다시 조회해 다음 수정 모달 오픈 시 최신 상품 기준 값을 사용한다.

## API Changes

- 없음

## Database Changes

- 없음

## Domain Model Changes

- 없음

## Validation Rules

- 구분 변경 시 값 자동 반영은 상태 기준 설정이 존재할 때만 수행한다.
- 저장 payload에는 자동 반영된 `onlyMallYn`, `availableQty`, `safetyRate`가 그대로 포함되어야 한다.

## Edge Cases

- 상태 기준이 아직 로딩되지 않은 경우 구분은 바뀌지만 입력값은 강제로 덮어쓰지 않는다.
- 다른 상품을 연속으로 수정할 때 이전 모달 state가 남지 않도록 `item` 변경 시 로컬 state를 다시 초기화한다.

## Exception Scenarios

- 상태 기준 저장 실패 시 토스트와 에러 UI만 노출하고 상품 목록 재조회는 성공 처리하지 않는다.
- 상품 수정 저장 실패 시 자동 반영된 값이 서버에 확정 저장된 것으로 간주하지 않는다.

## Acceptance Criteria Mapping

- AC1: 상태 기준 저장 후 상품 목록 재조회로 최신 기준 값 표시
- AC2: 구분 변경 시 선택 scope 기준 값 즉시 반영 및 저장 payload 유지
- AC3: 실패 시 기존 오류 메시지 유지
