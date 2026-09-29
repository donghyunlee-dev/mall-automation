# dashboard-order-sync Test Result

## Scope

- `GodoOrderResponseParser`, `GodoOrderApiClient`, `OrderSyncBatchJob`의 단위 테스트 및 `BatchExecutionService` 연동 회귀 확인.
- 전체 백엔드 테스트 스위트 회귀 확인.

## Environment

- Profile: `local` (기본 프로파일)
- Database: SQLite (파서/API클라이언트/배치잡 테스트는 DB 접근 없는 순수 단위 테스트; `BatchExecutionServiceTest`는 Mockito 목 객체 사용)
- External dependencies: 없음 — 모든 테스트는 Fake/Capturing `GodoTransport`와 Fake `ApiTransactionLogRepository`로 대체. 실제 고도몰 API로의 호출은 이번 태스크에서 수행하지 않음.

## Commands

- `./gradlew test --tests "com.sfood.automation.dashboard.infrastructure.external.GodoOrderResponseParserTest"`
- `./gradlew test --tests "com.sfood.automation.dashboard.infrastructure.external.GodoOrderApiClientTest"`
- `./gradlew test --tests "com.sfood.automation.batch.infrastructure.job.OrderSyncBatchJobTest"`
- `./gradlew test` (전체 스위트)

## Results

- `GodoOrderResponseParserTest` (7 tests): 구현을 먼저 완성한 뒤 실행해 전부 첫 실행에 통과. 이후 화이트리스트/비회원 제외 필터를 의도적으로 무력화(`if (false)`)하고 재실행 → 정확히 예상한 2개 테스트(`should_exclude_order_when_status_not_in_whitelist`, `should_exclude_guest_order_without_member_id`)만 실패하는 것을 확인 → 필터 원복 → 재실행 GREEN. 테스트가 실제로 의미 있는 검증을 하고 있음을 확인.
- `GodoOrderApiClientTest` (3 tests): 첫 실행에 통과.
- `OrderSyncBatchJobTest` (3 tests): 클래스 미존재로 컴파일 실패(RED) 확인 → `OrderSyncBatchJob` 구현 → GREEN.
- `BatchExecutionServiceTest`: 생성자 시그니처 변경으로 컴파일 실패 → 목 객체(`orderSyncBatchJob`) 추가 및 생성자 호출 갱신 → GREEN.
- 전체 스위트: **449 tests, 0 failed, 0 errors, 4 skipped** → `BUILD SUCCESSFUL` (6m 34s).
- PASS: 449/449 (스킵 4건은 기존부터 skip 처리, 이번 변경과 무관).
- FAIL: 없음.

## Failure Notes

- 파서/API클라이언트/배치잡 테스트 다수는 구현을 완성한 뒤 실행해 "테스트가 먼저 실패하는 것을 직접 관찰"하지 못했다(엄밀한 TDD 위반). 이를 보완하기 위해 파서의 핵심 필터 로직을 사후에 의도적으로 깨뜨려 테스트가 정확히 예상된 케이스에서만 실패하는지 확인했고, 실제로 그렇게 동작함을 검증했다. `OrderSyncBatchJob`과 `BatchExecutionServiceTest` 갱신은 클래스/시그니처 부재로 인한 컴파일 실패를 RED로 관찰한 뒤 구현한 정석적인 TDD 사이클이었다.

## Retest Results

- 필터 로직 원복 후 파서 테스트 전체 재실행: PASS.
- 전체 스위트 재실행: PASS (449/449).

## Acceptance Criteria Coverage

- AC1 (결제_final 산식): 충족 — `should_compute_payment_final_amount_from_usable_fields_only`.
- AC2 (화이트리스트/비회원 제외): 충족 — 뮤테이션 점검으로 실효성까지 확인.
- AC3 (item_type 태깅): 충족 — `should_tag_add_goods_data_items_with_add_goods_type`.
- AC4 (API 클라이언트 호출/실패 처리): 충족 — `GodoOrderApiClientTest` 3건.
- AC5 (배치 잡 upsert + 요약 메시지): 충족 — `OrderSyncBatchJobTest` 3건.
- AC6 (회귀 없음): 충족 — 449/449 통과.

## 추가 검증 (후속: 페이지네이션 + 21일 롤링 재조회)

- `GodoOrderResponseParserTest`에 3건 추가(`hasMore` true/false, `nextLastOrder` 최솟값 계산) — 기존 3-arg `GodoOrderSearchResult`로 컴파일 실패(RED) 확인 후 4-arg로 구현, GREEN.
- `OrderSyncBatchJobTest` 전면 재작성(4건: 21일 윈도우 범위, 페이지네이션 커서 진행, 전 페이지 upsert, 합산 요약) — 기존 단건 조회 구현으로 4건 모두 RED 확인 후 재구현, GREEN.
- 전체 스위트 재실행: **453 tests, 0 failed, 0 errors, 4 skipped** → `BUILD SUCCESSFUL` (6m 13s).
