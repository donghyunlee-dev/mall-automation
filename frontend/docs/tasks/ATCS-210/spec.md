# ATCS-210 Spec

## Feature Specification

- 전체 상품 목록의 판매 상태는 다음 규칙으로 표시한다.
  - `godoSyncYn === 'N'` 이면 `확인불가`
  - `godoSyncYn === 'Y' && soldOutYn === 'Y'` 이면 `품절`
  - `godoSyncYn === 'Y' && soldOutYn === 'N'` 이면 `판매중`
- ERP 연동 여부는 판매 상태 판단에 사용하지 않는다.
- `확인불가`, `품절`, `판매중` 필터와 대시보드 품절 집계도 동일 규칙을 따른다.

## API Changes

- 없음

## Database Changes

- 없음

## Domain Model Changes

- 없음

## Validation Rules

- `erpSyncYn === 'N'` 이더라도 `godoSyncYn === 'Y'`이면 판매 상태를 표시해야 한다.
- `확인불가` 필터는 `godoSyncYn === 'N'` 상품만 포함해야 한다.
- 품절 집계는 `godoSyncYn === 'Y' && soldOutYn === 'Y'` 조건만 포함해야 한다.

## Edge Cases

- `godoSyncYn === 'Y' && erpSyncYn === 'N' && soldOutYn === 'N'`이면 `판매중`
- `godoSyncYn === 'Y' && erpSyncYn === 'N' && soldOutYn === 'Y'`이면 `품절`
- `godoSyncYn === 'N'`이면 `soldOutYn` 값과 무관하게 `확인불가`

## Exception Scenarios

- 상품 목록 조회 실패 처리 방식은 기존과 동일하게 유지한다.

## Acceptance Criteria Mapping

- 고도몰 미연동 상품만 확인불가 표시: 판매 상태 유틸 및 테이블 렌더링
- 고도몰 연동 상품은 품절 여부 표시: 테이블 렌더링 및 상태 필터
- 대시보드 집계 일관성 유지: 품절 수 집계 로직 수정
