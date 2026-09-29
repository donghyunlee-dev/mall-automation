# dashboard-order-sync Tasks

## Implementation Tasks

- [x] `GodoOrderSearchQuery`, `GodoOrderSearchResult` 도메인 모델, `GodoOrderSearchPort` 포트 작성
- [x] `GodoOrderResponseParser` 구현 (화이트리스트 필터, 비회원 제외, 결제_final 계산, item_type 태깅, 배송일 파생)
- [x] `GodoOrderApiClient` 구현 (`GoodsSearchApiCaller`/`GodoApiClient` 패턴 재사용)
- [x] `OrderSyncBatchJob` 구현 (`MemberSyncBatchJob` 패턴 재사용)
- [x] `BatchExecutionService` 생성자/`runJob`에 `ORDER_SYNC` 분기 추가
- [x] `BatchExecutionServiceTest` 생성자 시그니처 변경 반영

## Test Tasks

- [x] `GodoOrderResponseParserTest` — 7개 케이스(계산식, 화이트리스트 제외, 비회원 제외, 배송일 파생 2종, item_type 태깅, 다건 파싱)
- [x] 필터 로직을 일부러 비활성화해 테스트가 실제로 실패하는지 확인(뮤테이션 점검) 후 원복
- [x] `GodoOrderApiClientTest` — URL/요청바디, 성공 파싱, 실패 코드 예외
- [x] `OrderSyncBatchJobTest` — 날짜 파라미터 전달, upsert 호출, 요약 메시지
- [x] 전체 스위트(`./gradlew test`) 회귀 확인

## Documentation Tasks

- [x] 본 태스크 문서(spec/plan/tasks) 작성 — test-result/result는 전체 테스트 결과 확인 후 마무리

## Progress Notes

- 파서/배치 잡 테스트는 구현을 먼저 완성한 뒤 실행해 첫 실행에 전부 통과했다(진짜 TDD의 "먼저 실패를 본다" 원칙을 엄밀히 지키지 못한 부분). 이를 보완하기 위해 핵심 필터링 로직(화이트리스트, 비회원 제외)을 의도적으로 무력화한 뒤 재실행해 해당 테스트가 실제로 실패하는 것을 확인하고 원복하는 방식으로 테스트의 유효성을 사후 검증했다.
- `OrderSyncBatchJob`은 최초 작성 시 클래스가 없어 컴파일 실패로 RED를 확인한 뒤 구현했다(정석적인 TDD 사이클).

## Review Notes

- ~~Out of Scope로 명시한 21일 롤링 재조회, 페이지네이션~~ → **후속 작업으로 추가 완료**(아래 참고).

## 후속 작업: 21일 롤링 재조회 + 페이지네이션 (추가 완료)

- [x] `GodoOrderSearchResult`에 `nextLastOrder`(전체 order_data 중 최소 주문번호, 필터 여부 무관) 필드 추가
- [x] `GodoOrderResponseParser` — 헤더의 `lastOrder`(true/false) 플래그를 `hasMore`로, 전체 주문번호 최솟값을 `nextLastOrder`로 파싱
- [x] `OrderSyncBatchJob.run(date)`를 "단건 조회"에서 "`date-20`~`date` 21일 윈도우, `size`+`lastOrder` 커서 기반 페이지네이션 루프"로 재구현
  - 매일 실행 시 이 21일 윈도우가 "어제 신규 수집"과 "최근 20일 상태 변경 반영"을 동시에 처리(9.2.3⑥ 결정과 동일한 효과를 더 단순한 구조로 달성 — 원본처럼 신규/재조회 두 번 호출할 필요 없이 upsert 특성상 1회 윈도우 조회로 충분)
- [x] 파서 신규 테스트 3건(hasMore true/false, nextLastOrder 최솟값 계산) — RED 확인 후 구현
- [x] `OrderSyncBatchJobTest` 전면 재작성(4건: 윈도우 범위, 페이지네이션 커서 진행, 전 페이지 upsert, 합산 요약) — 기존 단건 조회 구현으로 4건 모두 RED 확인 후 재구현
- [x] 전체 스위트 회귀 확인

**남은 리스크**: 페이지 크기(`PAGE_SIZE=500`)가 실제 운영 주문량 대비 적정한지, 그리고 고도몰 API가 21일 범위 조회를 문제없이 처리하는지는 실제 호출 없이는 확인 불가 — 라이브 검증 전까지는 리스크로 남음.
