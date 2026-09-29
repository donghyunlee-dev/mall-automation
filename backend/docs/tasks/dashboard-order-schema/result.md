# dashboard-order-schema Result

## Delivered Scope

- 대시보드 통합용 주문 데이터 테이블 `godo_order`(주문 헤더), `godo_order_item`(주문상품/추가상품 라인)을 로컬(SQLite)과 운영(PostgreSQL) 양쪽 스키마에 추가.
- 배치 시스템에 `order-sync` 잡을 등록(비활성 상태, `enabled=0`) — 실제 수집 로직(`OrderSyncBatchJob`)은 후속 태스크.
- `BatchJobType`에 `ORDER_SYNC("order-sync")` 상수 추가.
- 기존 테스트(`SqliteBootstrapRunnerTest`)의 테이블 개수 하드코딩 값을 신규 테이블 반영해 수정.

## Changed Files

- `backend/src/main/resources/schema.sql` — `godo_order`, `godo_order_item` DDL + 인덱스 추가
- `backend/src/main/resources/postgresql/schema-order-postgresql.sql` — 신규 파일, PostgreSQL DDL
- `backend/src/main/resources/data.sql` — `order-sync` 배치 시드 행 추가
- `backend/src/main/resources/postgresql/data-postgresql.sql` — 동일 시드 행 추가
- `backend/src/main/java/com/sfood/automation/batch/domain/enums/BatchJobType.java` — `ORDER_SYNC` 추가
- `backend/src/test/java/com/sfood/automation/common/bootstrap/SqliteBootstrapRunnerTest.java` — 테이블 개수 기대값 16→18 수정
- `docs/dashboard-integration/sfood-mall-dashboard-current-state.md` — 날짜 컬럼 타입 결정을 TEXT/VARCHAR로 정정, 구현 완료 항목 반영

## Key Decisions

- **날짜 컬럼 타입**: 분석 문서 초안에서는 `TIMESTAMP`/`DATE` 네이티브 타입을 검토했으나, 로컬(SQLite)·운영(PostgreSQL) 양쪽에서 동일하게 검증 가능해야 한다는 요구와 기존 코드베이스 전체의 검증된 관례(TEXT/VARCHAR + 애플리케이션 파싱, 예: `SqliteInstantParser`)를 따라 문자열 컬럼으로 확정.
- **`order-sync` 배치를 비활성으로 등록**: 실제 실행 로직(`OrderSyncBatchJob`)이 없는 상태에서 `enabled=1`로 등록하면 스케줄러가 매 주기 `IllegalArgumentException`으로 실패하고 Slack에 반복 알림이 갈 위험이 있어, 잡 구현 완료 전까지 `enabled=0`으로 유지하기로 함.
- **PostgreSQL 스키마를 별도 파일로 분리**: 기존 `schema-member-postgresql.sql` 선례를 따라, 공용 `schema-postgresql.sql`에 섞지 않고 도메인별 신규 파일(`schema-order-postgresql.sql`)로 분리.
- **JPA 엔티티는 아직 만들지 않음**: `ddl-auto: validate` 환경에서 엔티티 없이 테이블만 먼저 만들어도 안전하며, 실제 적재/조회 로직 설계와 함께 엔티티를 만드는 것이 더 적절하다고 판단.

## Open Gaps

- `postgresql/schema-order-postgresql.sql`은 아직 실제 RDS에 적용되지 않았다(운영 반영은 별도 배포 작업 필요, 기존 관례상 수동 적용).
- `BatchJobType.ORDER_SYNC`에 대한 전용 단위 테스트는 추가하지 않음(단순 enum 상수 추가라 후속 태스크에서 실제 배치 잡 구현과 함께 다루는 것을 권장).
- 2년 백필용 "기간 지정 수동 수집 기능", 고도몰 주문 조회 API 클라이언트, `OrderSyncBatchJob`, 대시보드 조회 API, React 화면은 전부 미착수 상태(설계는 `docs/dashboard-integration/sfood-mall-dashboard-current-state.md` 9장에 정리됨).

## Next Steps

- 고도몰 주문 조회 API 클라이언트(`GodoApiClient` 확장) 및 `OrderSyncBatchJob` 구현 → 완료 후 `batch_schedule_config.order-sync`를 `enabled=1`로 전환.
- `godo_order`/`godo_order_item` JPA 엔티티 및 리포지토리 설계(실제 적재/조회 로직과 함께).
- 2년 백필용 기간 지정 수동 수집 기능 설계·구현.
- `postgresql/schema-order-postgresql.sql`을 실제 운영 RDS에 적용(배포 담당자 확인 필요).
