# dashboard-order-sync Spec

## Problem Statement

- `dashboard-order-persistence`에서 만든 영속성 계층을 실제로 채우려면 고도몰 주문 조회 API를 호출하고 응답을 파싱해 upsert하는 배치 잡이 필요하다.

## Business Context

- 매일 자동으로 전일자 주문을 수집해 대시보드 매출 집계의 원천 데이터를 쌓는다. 설계 근거: `docs/dashboard-integration/sfood-mall-dashboard-current-state.md` 9.2.1/9.2.3/9.4.

## Scope

- 고도몰 `Order_Search.php` 응답(XML) 파서(`GodoOrderResponseParser`): 회원 기반 필터링, 주문상태 화이트리스트 필터링, 결제_final 계산, 배송일 파생.
- `GodoOrderApiClient`(`GodoOrderSearchPort` 구현): URL/요청바디 구성, API 호출, 트랜잭션 로그 기록, 실패 시 예외.
- `OrderSyncBatchJob`: 특정 날짜(기본값 어제, KST) 기준으로 `dateType=order` 단건 조회 → 파싱 결과를 `GodoOrderRepository`/`GodoOrderItemRepository`에 upsert.
- `BatchExecutionService`/`BatchJobType.ORDER_SYNC` 연동(잡 이름으로 실행 가능하도록).

## Out Of Scope

- **21일 롤링 재조회**(9.2.3⑥, 상태 변경 반영용) — 이번 태스크는 "당일 신규 수집"만 구현. 재조회 로직은 후속 태스크.
- **페이지네이션**(`size`+`lastOrder` 커서) — 이번 태스크는 단건 조회만 구현(응답 건수가 `size` 없이 전체 반환되는 것을 전제). 주문량이 많아지면 후속 태스크에서 추가 필요.
- **2년 백필 기능**(기간 지정 수동 수집) — 별도 태스크.
- `batch_schedule_config.order-sync`를 `enabled=1`로 전환하는 것 — 실제 운영 API 호출을 스케줄에 올리는 결정이라 사용자 확인 후 별도 진행.

## User Or Operator Scenarios

- Scenario 1: 배치 관리자가 `order-sync` 잡을 수동 실행하면, 지정한 날짜의 회원 주문만 수집되어 `godo_order`/`godo_order_item`에 저장된다.
- Scenario 2: 같은 날짜로 다시 실행해도(재시도) 중복 행 없이 기존 행이 갱신된다(persistence 계층의 upsert 특성).

## Acceptance Criteria

- AC1: `GodoOrderResponseParser`가 실제 응답 샘플(`ORDER_SAMPLE.xml`)과 동일한 구조의 XML에서 결제_final을 원본 파이프라인과 동일한 산식으로 계산한다.
- AC2: 화이트리스트에 없는 `orderStatus`, 또는 `memId`가 빈 비회원 주문은 파싱 결과에서 제외된다.
- AC3: `orderGoodsData`는 `item_type=GOODS`, `addGoodsData`는 `item_type=ADD_GOODS`로 태깅된다.
- AC4: `GodoOrderApiClient`가 올바른 엔드포인트/파라미터로 호출하고, 응답 코드가 `000`이 아니면 예외를 던진다.
- AC5: `OrderSyncBatchJob.run(date)`가 파싱된 모든 주문/아이템을 upsert하고, 건수를 포함한 요약 메시지를 반환한다.
- AC6: 기존 테스트 스위트 회귀 없음.

## Edge Cases

- `deliveryDt`가 `"0000-00-00 00:00:00"`(미출고)인 경우 `delivered_at`/`oms_shipped_at` 모두 NULL.
- 같은 주문에 `orderGoodsData`와 `addGoodsData`가 모두 있는 경우 둘 다 수집.
- 응답에 `order_data`가 1건뿐이라 XML이 배열이 아닌 단일 객체로 파싱되는 경우(Jackson XML 관례)도 정상 처리.

## Assumptions

- 매출액 산식, 화이트리스트, 배송일 규칙은 9.2.3에서 확정된 것을 그대로 이식한다(임의 변경 없음).
- 트랜잭션 로그는 기존 `ApiTransactionLogRepository`(goods 도메인 소속이지만 범용 개념)를 재사용한다.

## Clarifications

- Question: 실제 API 호출 테스트(라이브 자격증명)를 이번 태스크에서 수행하는가?
  Answer: 아니오. 유닛 테스트는 전부 Fake/Capturing 트랜스포트로 검증했고, 실제 운영 자격증명으로의 호출 검증은 하지 않았다(이전 세션에서 자동 승인 분류기가 차단한 바 있음 — 사용자 승인 후 별도 진행).
