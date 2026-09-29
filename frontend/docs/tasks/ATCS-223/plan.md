# ATCS-223 Plan

## Requirement Summary

- 상품 목록 테이블에 품목별 자동 고도몰 상태 동기화 차단 상태를 표시한다.
- 운영자가 행 단위로 차단/해제 액션을 실행할 수 있어야 한다.
- 상태 변경 전 확인 UI와 실패 시 오류 처리 흐름을 제공한다.

## Business Context

- 특정 품목은 외부 API로 판매 상태 변경 요청을 보내면 안 되는 운영 상황이 있다.
- 기존 화면에는 품목별 전송 제어 지점이 없어 전체 배치 기준만으로 운영해야 했다.
- 판매 상태와 전송 제어 상태를 같은 행에서 관리할 수 있어야 운영 실수가 줄어든다.

## Scope

- `godoSyncBlockedYn` 필드를 프론트 타입과 API 응답에 반영
- 상품 목록 테이블에 전송 차단 컬럼 추가
- 행 단위 차단/해제 버튼 및 확인 모달 추가
- API 성공 시 목록 재조회, 실패 시 오류 토스트 처리
- 차단 상태 필터 추가
- 관련 테스트와 작업 문서 업데이트

## Out Of Scope

- 백엔드 API 구현
- 배치 엔진의 실제 전송 차단 로직 변경
- 별도 상세 페이지 또는 대량 일괄 차단 UI

## Impacted Modules

- `src/types/goods.ts`
- `src/api/goods.ts`
- `src/hooks/useGoods.ts`
- `src/components/dashboard/ProductSearchBar.tsx`
- `src/components/dashboard/ProductTable.tsx`
- `src/components/modals/*`
- `src/pages/DashboardPage.tsx`

## Architecture Considerations

- 차단 상태는 품목 속성이므로 전역 배치 설정이 아니라 상품 목록 행 컨텍스트에 둔다.
- API 성공 응답을 신뢰해도 되지만 기존 패턴과 맞추기 위해 저장 후 목록 재조회로 일관성을 유지한다.
- 차단 활성화는 백엔드 제약이 있으므로 UI는 선제 비활성화와 서버 오류 처리 둘 다 고려한다.

## Risk Analysis

- 테이블 컬럼 추가로 폭이 좁아져 모바일 가독성이 떨어질 수 있다.
- 상태 변경 API 실패 시 낙관적 업데이트를 쓰면 잘못된 상태가 보일 수 있다.
- 차단 가능 조건을 UI와 서버가 다르게 해석하면 운영 혼선이 생길 수 있다.
