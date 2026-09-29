# dashboard-order-schema Spec

## Problem Statement

- 에쓰푸드몰 판매 대시보드(Streamlit)를 메인 서비스로 통합하기 위해, 고도몰 주문 데이터를 저장할 신규 테이블이 메인 DB(로컬 SQLite / 운영 PostgreSQL)에 필요하다.
- 상세 분석·설계 근거는 `docs/dashboard-integration/sfood-mall-dashboard-current-state.md`(저장소 루트) 9장에 정리되어 있다. 본 태스크는 그 설계를 실제 스키마로 반영하는 범위만 다룬다.

## Business Context

- 매출 대시보드를 React로 재구현하고, 데이터는 배치가 고도몰 주문 조회 API로 수집해 이 신규 테이블에 적재한다(캐시 없이 직접 조회).
- 이번 태스크는 그 전체 기능 중 "테이블 스키마"와 "배치 시스템에 잡 등록(비활성 상태)"만 다룬다. 실제 수집 로직(API 클라이언트, 배치 잡, 조회 API)은 별도 태스크로 진행한다.

## Scope

- `godo_order`, `godo_order_item` 두 테이블을 로컬(SQLite, `schema.sql`)과 운영(PostgreSQL, 신규 `postgresql/schema-order-postgresql.sql`) 양쪽에 생성한다.
- `BatchJobType`에 `ORDER_SYNC("order-sync")`를 추가한다.
- `batch_schedule_config`에 `order-sync` 잡 설정 행을 시드 데이터로 추가하되, 실제 배치 잡 구현 전까지 `enabled = 0`으로 등록한다(스케줄러가 미구현 잡을 실행 시도해 매번 실패·Slack 알림이 발생하는 것을 방지).

## Out Of Scope

- `OrderSyncBatchJob`, 고도몰 주문 조회 API 클라이언트, 대시보드 조회 API, React 화면 — 모두 별도 태스크.
- 2년 백필 기능(기간 지정 수동 수집) 구현.
- 기존 GoodsPage "대시보드" 명칭 변경(프론트엔드 별도 작업).

## User Or Operator Scenarios

- Scenario 1: 로컬 개발 환경에서 `./gradlew bootRun`(SQLite) 실행 시 `godo_order`/`godo_order_item` 테이블이 자동 생성된다.
- Scenario 2: 운영 배포 담당자가 `postgresql/schema-order-postgresql.sql`을 RDS PostgreSQL에 수동 적용해 동일한 테이블을 만든다(기존 `schema-member-postgresql.sql`과 동일한 운영 방식).
- Scenario 3: 배치 관리 화면에서 `order-sync` 잡이 목록에 나타나되, `enabled=0`이라 스케줄러가 자동 실행하지 않는다.

## Acceptance Criteria

- AC1: 로컬(SQLite) 기동 시 `schema.sql`의 `godo_order`/`godo_order_item` DDL이 오류 없이 적용된다.
- AC2: `postgresql/schema-order-postgresql.sql`의 DDL이 PostgreSQL 문법으로 유효하다(리뷰 기준 확인, 이번 태스크에서 실제 RDS 적용은 하지 않음).
- AC3: `BatchJobType.ORDER_SYNC.jobName()`이 `"order-sync"`를 반환한다.
- AC4: `batch_schedule_config`에 `order-sync` 행이 `enabled=0`으로 존재한다(로컬 SQLite `data.sql` 기준).
- AC5: 기존 백엔드 테스트(`./gradlew test`)가 이번 변경으로 깨지지 않는다.

## Edge Cases

- `godo_order_item`의 `godo_line_no`는 `orderGoodsData.sno`와 `addGoodsData.sno`가 서로 다른 채번 체계라 값이 겹칠 수 있음 → `(item_type, godo_line_no)` 복합 UNIQUE로 처리(단일 UNIQUE 금지).
- `delivered_at`/`oms_shipped_at`은 미출고 주문에서 NULL일 수 있음 → NOT NULL 제약 두지 않음.

## Assumptions

- 날짜/시각 컬럼은 기존 코드베이스 관례(TEXT/VARCHAR + 애플리케이션 레벨 파싱, 예: `SqliteInstantParser`)를 그대로 따른다. SQLite/PostgreSQL 양쪽에서 동일하게 동작해야 한다는 요구사항 때문에, 분석 문서 초안에 있던 `TIMESTAMP`/`DATE` 네이티브 타입 대신 문자열 컬럼으로 확정했다(상세 사유는 분석 문서 9.3 갱신 내용 참고).
- 이번 태스크에서는 JPA 엔티티를 아직 만들지 않는다(엔티티는 실제 조회/적재 로직을 만드는 후속 태스크에서 함께 설계).

## Clarifications

- Question: 로컬(SQLite)과 운영(PostgreSQL)이 다른데 어떻게 둘 다 확인 가능하게 구성하나?
  Answer: 기존 프로젝트 관례와 동일하게 `schema.sql`(SQLite, 로컬 자동 적용)과 `postgresql/schema-*-postgresql.sql`(PostgreSQL, 운영 수동 적용) 두 세트를 유지한다. 날짜 컬럼 타입도 기존 관례(TEXT/VARCHAR)를 따라 두 DB에서 동일하게 동작하도록 한다.
