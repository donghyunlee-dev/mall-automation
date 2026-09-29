# ATCS-178 Spec

## Feature Specification

- 상품 수정 모달이 열릴 때 `item.erpSyncYn` 값을 확인한다.
- `erpSyncYn === 'Y'` 면 ERP 품목코드 입력창을 비활성화한다.
- `erpSyncYn === 'N'` 면 ERP 품목코드 입력창을 활성화한다.
- ERP 연동 완료 상태에서는 비활성화 이유를 안내 문구로 보여준다.

## API Changes

- 없음
- 기존 상품 목록/상세 응답의 `erpSyncYn` 필드를 사용한다.

## Database Changes

- 없음

## Domain Model Changes

- 없음

## Validation Rules

- `erpSyncYn === 'Y'` 이면 ERP 품목코드 입력은 수정 불가
- `erpSyncYn === 'N'` 이면 기존과 동일하게 수정 가능

## Edge Cases

- ERP 코드가 비활성화되어도 상품명/구분은 수정 가능해야 한다.
- 저장 중에는 기존과 동일하게 전체 입력이 비활성화된다.

## Exception Scenarios

- 수정 API 실패 시 기존 오류 메시지를 유지한다.

## Acceptance Criteria Mapping

- 품목 정보 조회 성공 시: 기존 `item` props 기반으로 충족
- `erp_sync_yn` 체크 로직 정상: 모달 ERP 입력 비활성화/활성화로 충족
