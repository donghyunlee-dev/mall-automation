# dashboard-order-sync Plan

## Summary

- `goods` 도메인의 `GodoApiClient`/`GoodsSearchApiCaller` 패턴을 그대로 따라 주문 조회용 API 클라이언트를 만들고, `ProductSyncBatchJob`/`MemberSyncBatchJob` 패턴을 따라 배치 잡을 만든다. 파싱 로직은 별도 클래스(`GodoOrderResponseParser`)로 분리해 HTTP 왕복 없이 단위 테스트한다.

## Impacted Modules

- `com.sfood.automation.dashboard.infrastructure.external` (신규): `GodoOrderResponseParser`, `GodoOrderResponseParseException`, `GodoOrderApiClient`
- `com.sfood.automation.batch.infrastructure.job`: `OrderSyncBatchJob`(신규), `BatchExecutionService`(수정 — 생성자에 `orderSyncBatchJob` 추가, `runJob`에 분기 추가)

## Architecture Approach

- `GodoOrderResponseParser`: 순수 함수(XML 문자열 → `GodoOrderSearchResult`), 외부 의존성 없음 — HTTP 없이 빠르게 단위 테스트.
- `GodoOrderApiClient`: HTTP 왕복 + 트랜잭션 로그만 담당하고 파싱은 파서에 위임(관심사 분리).
- `OrderSyncBatchJob`: 포트(`GodoOrderSearchPort`)와 리포지토리 포트에만 의존 — 인프라 구현에 결합되지 않음(Interface-First Design Rule 준수).

## Data And API Impact

- 신규 외부 API 연동: `POST {GODO_BASE_URL}/godomall5/order/Order_Search.php` (기존 `GODO_PARTNER_KEY`/`GODO_KEY` 자격증명 재사용, 신규 발급 불필요).
- DB 스키마 변경 없음(이미 `dashboard-order-schema`에서 생성됨).

## Validation Plan

- TDD: 파서 → API 클라이언트 → 배치 잡 순서로 각각 실패 테스트 작성 후 구현.
- 필터링 로직(화이트리스트/비회원 제외)은 의도적으로 비활성화한 뒤 테스트가 실제로 실패하는지 확인(뮤테이션 점검)하여 테스트의 유효성을 검증.
- `./gradlew test` 전체 스위트로 회귀 확인.

## Risks

- Risk 1: 페이지네이션 미구현 상태로 실제 운영에 배포하면, 응답 건수가 많은 날짜(예: 세일 기간)에 데이터 누락 가능 — `order-sync`를 `enabled=1`로 전환하기 전에 반드시 페이지네이션을 추가해야 함(Out of Scope에 명시).
- Risk 2: 실제 API 응답이 테스트에서 가정한 필드 형식과 다를 경우(예: 날짜 포맷, 소수점 자리수) 파싱 오류 가능 — 실제 계정으로의 검증 전까지는 리스크로 남음.

## Rollback Considerations

- 신규 파일만 추가되었고, `BatchExecutionService`/`BatchExecutionServiceTest`만 기존 파일 수정(생성자 파라미터 추가) — 되돌리려면 해당 두 파일의 diff만 되돌리고 `dashboard.infrastructure.external`/`OrderSyncBatchJob` 관련 신규 파일을 삭제하면 됨.
- `batch_schedule_config.order-sync`는 여전히 `enabled=0`이므로 이번 배포가 실제 운영 동작에 영향을 주지 않음.
