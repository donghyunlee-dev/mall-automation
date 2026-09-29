# ATCS-223 Spec

## Feature Specification

- 전체 상품 목록에 `전송 차단` 컬럼을 추가한다.
- 셀은 `허용` 또는 `차단` 배지로 현재 상태를 표시한다.
- 각 행은 현재 상태에 따라 `차단`, `해제`, `차단 불가` 중 하나의 액션을 노출한다.
- `차단` 또는 `해제` 실행 전 확인 모달을 띄운다.
- API 성공 시 목록을 다시 불러오고 성공 토스트를 표시한다.
- API 실패 시 상태를 유지하고 오류 토스트를 표시한다.
- 검색 필터 바에 `전송 차단` 토글을 추가한다.

## API Changes

- 사용 API: `PATCH /api/goods/{id}/godo-sync-block`
- Request body:
  - `blockedYn: 'Y' | 'N'`
- Response body:
  - `GoodsItem`에 `godoSyncBlockedYn` 포함

## Database Changes

- 프론트 변경 없음
- 백엔드 명세상 `goods.godo_sync_blocked_yn` 사용 전제

## Domain Model Changes

- `GoodsItem`에 `godoSyncBlockedYn: 'Y' | 'N'` 추가
- 전송 차단 변경 요청 타입 추가

## Validation Rules

- 차단 활성화는 `status === 'NORMAL'`인 품목에서만 허용 UI를 노출한다.
- 차단 해제는 현재 상태와 무관하게 허용한다.
- 저장 중 중복 요청을 막는다.

## Edge Cases

- `godoSyncBlockedYn`가 누락되면 기본값 `N`으로 해석한다.
- 차단 불가 상태 품목은 버튼을 비활성화하고 이유를 title로 안내한다.
- 목록 재조회 실패 시 기존 오류 처리 메시지를 따른다.

## Exception Scenarios

- PATCH 요청 실패
- 인증 만료로 인한 401
- `NORMAL`이 아닌 품목에 차단 요청 시 400

## Acceptance Criteria Mapping

- AC1 전송 차단 컬럼 추가: `ProductTable`
- AC2 차단/해제 액션 제공: `ProductTable` + 확인 모달
- AC3 성공 시 상태 반영: `DashboardPage` 재조회 흐름
- AC4 실패 시 상태 유지: 토스트 오류 처리
- AC5 차단 품목 탐색: `ProductSearchBar` + `useGoods` 필터
