# ATCS-199 Spec

## Feature Specification

- 상품 수정 모달에 `가용재고 기준`, `잔여율 기준` 입력을 추가한다.
- 모달 오픈 시 기존 `item.availableQty`, `item.safetyRate` 값을 표시한다.
- 저장 시 `PUT /api/goods/{id}` 요청 body에 두 값을 포함한다.

## API Changes

- 기존 `PUT /api/goods/{id}` body에 아래 필드를 포함한다.
  - `availableQty`
  - `safetyRate`

## Database Changes

- 없음

## Domain Model Changes

- `GoodsUpdateBody` 타입에 `availableQty`, `safetyRate` 추가

## Validation Rules

- 두 입력은 숫자값으로 저장되어야 한다.
- `0` 값은 유효한 값으로 취급한다.
- `null` 또는 빈 문자열 입력은 저장 전에 `0`으로 정규화한다.

## Edge Cases

- 기존 품목 값이 비어 있거나 falsy여도 UI에는 `0`으로 표시한다.
- 저장 중에는 기존과 동일하게 입력을 비활성화한다.

## Exception Scenarios

- 수정 API 실패 시 기존 오류 메시지를 유지한다.

## Acceptance Criteria Mapping

- 품목 데이터 정상 표시: 모달 초기값 바인딩
- 변경 후 재조회 시 데이터 변경: 저장 body에 변경값 포함
- 0값, null 체크: 입력 정규화로 충족
