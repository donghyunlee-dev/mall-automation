# dashboard-order-schema Plan

## Summary

- 기존 스키마/시드 데이터 관례(SQLite `schema.sql`+`data.sql`, PostgreSQL은 도메인별 `schema-*-postgresql.sql`)를 그대로 따라 주문 관련 테이블 2개를 추가하고, 배치 시스템에 `order-sync` 잡을 비활성 상태로 등록한다.

## Impacted Modules

- `src/main/resources/schema.sql` (SQLite DDL)
- `src/main/resources/data.sql` (SQLite 시드)
- `src/main/resources/postgresql/schema-order-postgresql.sql` (신규, PostgreSQL DDL)
- `src/main/resources/postgresql/data-postgresql.sql` (PostgreSQL 시드, 말미에 추가)
- `src/main/java/com/sfood/automation/batch/domain/enums/BatchJobType.java`

## Architecture Approach

- 이번 태스크는 Infrastructure 레이어의 순수 DDL/시드 데이터와 Domain enum 상수 추가에 한정한다. Application/Presentation 레이어 변경 없음 — Clean Architecture 레이어 의존 방향에 영향 없음.

## Data And API Impact

- 신규 테이블 `godo_order`(주문 헤더), `godo_order_item`(주문상품/추가상품 라인). 컬럼·인덱스는 `docs/dashboard-integration/sfood-mall-dashboard-current-state.md` 9.3.2/9.3.3 기준(날짜 컬럼 타입만 TEXT/VARCHAR로 정정).
- 기존 `batch_schedule_config`에 `order-sync` 행 1개 추가(`enabled=0`). API/컨트롤러 변경 없음 — 기존 `BatchScheduleAdminController`가 그대로 이 행을 조회 가능.
- JPA 엔티티는 만들지 않으므로 `ddl-auto: validate`에 영향 없음(엔티티가 없으면 Hibernate가 이 테이블들을 검증 대상으로 보지 않음).

## Validation Plan

- `./gradlew test` — 기존 테스트 스위트가 스키마 변경으로 깨지지 않는지 확인.
- `./gradlew bootRun`(로컬 SQLite)으로 기동해 `schema.sql` 적용 오류가 없는지 수동 확인(가능한 경우).
- PostgreSQL DDL은 실제 RDS 적용 없이 SQL 문법 리뷰로 검증(운영 반영은 별도 배포 작업).

## Risks

- Risk 1: `batch_schedule_config`에 `order-sync`를 `enabled=1`로 잘못 등록하면, `BatchExecutionService.runJob()`에 해당 분기가 없어 스케줄러가 매 주기 `IllegalArgumentException`으로 실패하고 Slack 알림이 반복 발송된다 → 반드시 `enabled=0`으로 등록.
- Risk 2: PostgreSQL 스키마 파일은 이번 태스크에서 실제 RDS에 적용하지 않으므로, 향후 실제 적용 시점에 별도 확인이 필요하다.

## Rollback Considerations

- SQLite: `schema.sql`/`data.sql`에서 추가한 블록만 제거하면 로컬 DB 파일 재생성 시 원복된다(로컬 DB 파일 자체를 지우고 재기동하는 것이 가장 간단).
- PostgreSQL: 아직 운영에 적용하지 않았으므로 파일만 삭제하면 된다.
- `BatchJobType.ORDER_SYNC` 추가는 다른 코드에서 참조하지 않는 한 제거해도 부작용 없음.
