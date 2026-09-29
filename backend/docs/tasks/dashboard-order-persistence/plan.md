# dashboard-order-persistence Plan

## Summary

- 기존 `goods`/`member` 모듈의 Entity/JpaRepository/Port/RepositoryImpl 패턴을 그대로 따라 `dashboard` 모듈을 신설하고, TDD로 upsert 동작을 검증한다.

## Impacted Modules

- `com.sfood.automation.dashboard.domain.model` (신규): `GodoOrder`, `GodoOrderItem`
- `com.sfood.automation.dashboard.domain.port` (신규): `GodoOrderRepository`, `GodoOrderItemRepository`
- `com.sfood.automation.dashboard.infrastructure.persistence` (신규): `GodoOrderEntity`, `GodoOrderJpaRepository`, `GodoOrderRepositoryImpl`, `GodoOrderItemEntity`, `GodoOrderItemJpaRepository`, `GodoOrderItemRepositoryImpl`

## Architecture Approach

- Clean Architecture 레이어 유지: Domain(Port/Model)은 프레임워크 의존 없음, Infrastructure가 JPA로 구현. Presentation/Application 레이어는 이번 태스크에서 다루지 않음(아직 조회 API/배치가 없음).

## Data And API Impact

- 신규 코드 없이 기존 `dashboard-order-schema`에서 만든 테이블만 매핑. 스키마 변경 없음.

## Validation Plan

- TDD로 각 리포지토리의 upsert 동작을 먼저 실패하는 테스트로 작성 → 구현 → 통과 확인.
- `./gradlew test` 전체 스위트로 회귀 확인.

## Risks

- Risk 1: SQLite와 PostgreSQL에서 `BigDecimal` 컬럼 매핑 동작이 다를 수 있음 — 이번 태스크는 SQLite 테스트로만 검증했고, PostgreSQL 실제 검증은 운영 반영 시점에 별도 확인 필요.

## Rollback Considerations

- 신규 파일만 추가되었으므로(기존 파일 수정 없음), 문제가 생기면 `com.sfood.automation.dashboard` 패키지 전체를 삭제하는 것만으로 원복 가능.
