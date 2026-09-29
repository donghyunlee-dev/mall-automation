# dashboard-order-sync Result

## Delivered Scope

- 고도몰 주문 조회 응답(XML) 파서 `GodoOrderResponseParser` — 회원 기반 필터링, 주문상태 화이트리스트 필터링, 결제_final 계산, 배송일 파생, 주문상품/추가상품 태깅.
- `GodoOrderApiClient`(`GodoOrderSearchPort` 구현) — 기존 `GodoApiClient`/`GoodsSearchApiCaller` 패턴 재사용.
- `OrderSyncBatchJob` — 지정 날짜(기본 어제, KST) 기준 단건 조회 → upsert, 기존 `MemberSyncBatchJob` 패턴 재사용.
- `BatchExecutionService`에 `ORDER_SYNC` 실행 분기 연결.

## Changed Files

- `backend/src/main/java/com/sfood/automation/dashboard/domain/model/GodoOrderSearchQuery.java` (신규)
- `backend/src/main/java/com/sfood/automation/dashboard/domain/model/GodoOrderSearchResult.java` (신규)
- `backend/src/main/java/com/sfood/automation/dashboard/domain/port/GodoOrderSearchPort.java` (신규)
- `backend/src/main/java/com/sfood/automation/dashboard/infrastructure/external/GodoOrderResponseParser.java` (신규)
- `backend/src/main/java/com/sfood/automation/dashboard/infrastructure/external/GodoOrderResponseParseException.java` (신규)
- `backend/src/main/java/com/sfood/automation/dashboard/infrastructure/external/GodoOrderApiClient.java` (신규)
- `backend/src/main/java/com/sfood/automation/batch/infrastructure/job/OrderSyncBatchJob.java` (신규)
- `backend/src/main/java/com/sfood/automation/batch/infrastructure/job/BatchExecutionService.java` (수정 — `orderSyncBatchJob` 주입 및 `runJob` 분기 추가)
- `backend/src/test/java/com/sfood/automation/dashboard/infrastructure/external/GodoOrderResponseParserTest.java` (신규)
- `backend/src/test/java/com/sfood/automation/dashboard/infrastructure/external/GodoOrderApiClientTest.java` (신규)
- `backend/src/test/java/com/sfood/automation/batch/infrastructure/job/OrderSyncBatchJobTest.java` (신규)
- `backend/src/test/java/com/sfood/automation/batch/infrastructure/job/BatchExecutionServiceTest.java` (수정 — 생성자 시그니처 변경 반영)

## Key Decisions

- **파싱 로직을 HTTP 클라이언트와 분리**: `GodoOrderResponseParser`를 순수 함수로 만들어 XML 구조/필터링/계산 로직만 빠르고 정밀하게 단위 테스트. `GodoOrderApiClient`는 HTTP 왕복과 트랜잭션 로그만 담당.
- **트랜잭션 로그는 기존 `ApiTransactionLogRepository`(goods 도메인) 재사용**: 범용 "API 호출 로그" 개념이라 중복 구현하지 않음.
- **21일 롤링 재조회·페이지네이션은 명시적으로 범위 제외**: 지금은 "하루치 신규 수집"만 동작. 운영 활성화 전 반드시 추가 필요(spec.md Out of Scope 참고).
- **`batch_schedule_config.order-sync`는 여전히 `enabled=0` 유지**: 이번 태스크로 실제 배치 로직은 완성됐지만, 페이지네이션·재조회가 없는 상태로 바로 운영 스케줄에 올리는 것은 위험하다고 판단해 활성화하지 않음 — 사용자 확인 후 별도 결정.

## Open Gaps

- ~~페이지네이션 미구현~~ → **완료**: `size`+`lastOrder` 커서 기반 페이지네이션 구현(전체 453 tests 통과).
- ~~21일 롤링 재조회 미구현~~ → **완료**: `OrderSyncBatchJob.run(date)`가 `date-20`~`date` 21일 윈도우로 조회하도록 재구현.
- ~~실제 운영 자격증명으로의 라이브 API 호출 검증~~ → **완료(2026-09-04)**: 아래 "갱신" 참고.
- 2년 백필(기간 지정 수동 수집) 미구현 — 여전히 별도 태스크.
- 대시보드 조회 API, React 화면 — **완료**(4개 탭 전부, Iteration 11~15).

## Next Steps

- 2년 백필용 기간 지정 수동 수집 기능 설계·구현.
- ~~`enabled=1` 전환 여부 결정~~ → **완료(2026-09-04)**: 사용자 승인으로 `data.sql`/`postgresql/data-postgresql.sql`의 `order-sync` 시드 기본값을 `enabled=1`로 변경. **단, `application-prod.yml`은 `spring.sql.init.mode: never`라 이 시드는 운영 DB에는 적용되지 않음** — 이미 배포된 운영 환경에서 실제로 켜려면 배포 후 `PUT /api/admin/batch-schedules/order-sync {"enabled": true}`를 별도로 호출해야 함(신규/로컬 환경은 시드 값 그대로 활성화됨).

## 갱신 (2026-09-04): 라이브 API 실연동 검증 완료

로컬에서 백엔드를 `--spring.profiles.active=local --server.port=8090`으로 기동하고(포트 8080은 무관한 다른 프로젝트가 점유 중이라 회피), 실제 이메일 OTP 로그인(사용자 본인 계정) 후 `POST /api/admin/batch-schedules/order-sync/run`으로 `order-sync`를 1회 수동 실행해 실제 Godo 운영 API(`https://openhub.godo.co.kr`, 프로덕션 자격증명)로 검증했다.

**결과**: 주문 1,686건, 상품라인 3,297건 정상 동기화(윈도우 2026-08-14~2026-09-03). SQLite에 실제 주문 데이터가 정상 적재됐고(`godo_order`/`godo_order_item`), 이 데이터는 `.gitignore`로 저장소에 커밋되지 않는 로컬 파일임을 재확인.

**대시보드 API 교차 검증**: 실데이터 기준으로 `/api/dashboard/summary`(DAILY/WEEKLY), `/grade-detail`, `/top-items`, `/revenue-trend`(`dateBasis=SHIPPED`)를 호출해 원시 SQL 집계 결과와 대조 — 완전히 일치(예: 2026-09-03 DAILY summary `orderCount=95`, `revenue=8,747,668.17`가 `SELECT COUNT(*)`/`SUM(payment_final_amount)` 직접 쿼리 결과와 정확히 일치). 잠봉 슬라이스 롤업(top-items), 등급별 분류(grade-detail)도 실제 회원등급명으로 정상 동작 확인.

이로써 9.7 계획의 3번 단계("OrderSyncBatchJob 구현 및 수동 실행 검증")가 완전히 마무리됐다. `enabled=1` 전환(자동 스케줄 활성화)은 별도 사용자 결정 사안으로 남겨둠.
