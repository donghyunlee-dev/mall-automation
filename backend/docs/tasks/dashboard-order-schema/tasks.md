# dashboard-order-schema Tasks

## Implementation Tasks

- [x] `schema.sql`에 `godo_order`, `godo_order_item` 테이블 + 인덱스 추가 (SQLite)
- [x] `postgresql/schema-order-postgresql.sql` 신규 작성 (PostgreSQL)
- [x] `data.sql`에 `order-sync` 배치 시드 행 추가 (`enabled=0`)
- [x] `postgresql/data-postgresql.sql`에 동일 시드 행 추가
- [x] `BatchJobType`에 `ORDER_SYNC("order-sync")` 추가

## Test Tasks

- [x] `./gradlew test` 실행해 기존 테스트 회귀 없음 확인

## Documentation Tasks

- [x] `docs/dashboard-integration/sfood-mall-dashboard-current-state.md` 9.3 날짜 컬럼 타입을 TEXT/VARCHAR로 정정하고 사유 기록
- [x] 본 태스크 문서(spec/plan/tasks/test-result/result) 작성

## Progress Notes

- 날짜 컬럼 타입: 분석 문서 초안에서는 `TIMESTAMP`/`DATE` 네이티브 타입으로 정했었으나, 로컬 SQLite와 운영 PostgreSQL 양쪽에서 동일하게 검증 가능해야 한다는 요구에 따라 기존 코드베이스 관례(TEXT/VARCHAR + 애플리케이션 파싱)로 정정함.

## Review Notes

- `order-sync` 배치 잡은 `enabled=0`으로 등록되어 있어 실제 수집 로직(`OrderSyncBatchJob`) 구현 전까지는 스케줄러가 실행하지 않음. 후속 태스크에서 잡 구현 완료 후 `enabled=1`로 전환 필요.
