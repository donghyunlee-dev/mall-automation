# Iteration 26 계획 — 주문 동기화 타임아웃을 실패로 오인하는 문제 수정

> 작성일: 2026-09-09
> 심각도: 운영 UX 버그(데이터 유실 아님) — "주문 동기화 실행에 실패했어요"가 뜨지만 실제로는 데이터가 정상 저장됨

## 원인

- `POST /api/admin/batch-schedules/order-sync/run`은 동기 방식 — HTTP 요청 스레드가 `OrderSyncBatchJob.run()`(21일치 주문을 페이지 단위로 조회 + 주문/상품라인 건별 upsert) 완료까지 블로킹된다.
- 프론트(`runOrderSyncNow`)는 이 호출에 120초 타임아웃을 걸어뒀는데, 실제 운영 주문량에서는 건별 upsert(N+1 패턴)로 인해 120초를 넘을 수 있다.
- 클라이언트가 타임아웃으로 포기해도 백엔드는 요청 스레드에서 계속 실행되어 결국 데이터를 정상 저장한다 — 그 사이 프론트는 `catch`에서 타임아웃과 실제 실패를 구분하지 않고 항상 "주문 동기화 실행에 실패했어요."를 띄운다.

## 범위

- `runOrderSyncNow`의 타임아웃을 120초 → 300초로 늘려 여유를 둔다(근본 해결은 아님 — 언젠가 다시 초과할 수 있음)
- `useOrderSync`의 `runNow` catch에서 axios 타임아웃(`ECONNABORTED`)과 그 외 실제 실패를 구분해 다른 메시지를 보여준다

## 범위 제외(더 큰 구조 개선, 이번엔 미착수)

- `/run`을 비동기로 바꿔 즉시 응답하고 프론트가 상태 조회 API로 폴링하는 방식(타임아웃 개념 자체를 없앰) — 사용자가 "임시방편"으로 결정, 근본 해결은 추후 별도 작업으로 남김
- `GodoOrderRepositoryImpl.upsert`/`GodoOrderItemRepositoryImpl.upsert`의 건별 N+1 패턴 개선(배치 upsert) — 이번 범위 아님

## 완료 기준

- [x] TDD: 타임아웃(`ECONNABORTED`)과 일반 실패를 구분하는 테스트 작성 → RED 확인 → 구현 → GREEN
- [x] `npm run build`/`lint`/`test` 통과
