# Iteration 18 계획 — 주문 동기화 수동 실행 UI

> 작성일: 2026-09-07
> 관련: `docs/dashboard-integration/dashboard-prd.md` R4(2년 백필), 2026-09-07 대화에서 발견한 "데이터 최신화" 버튼의 오해(실제로는 DB 재조회일 뿐, Godo 재수집이 아님)

## 배경

사용자가 주간 탭 데이터 차이를 조사하는 과정에서 두 가지가 확인됐다.

1. 대시보드 각 탭의 "데이터 최신화" 버튼은 `GET /api/dashboard/*`만 재호출한다 — **우리 DB를 다시 읽는 것**이지 Godo에서 새로 가져오는 게 아니다.
2. 실제로 Godo에 주문을 다시 요청하는 기능(`OrderSyncBatchJob`, 최근 21일 롤링)은 이미 백엔드에 구현·검증되어 있고, `POST /api/admin/batch-schedules/order-sync/run?targetDate=YYYY-MM-DD`로 **임의 날짜를 기준으로 그 날짜의 21일 롤링 재수집**을 수동 실행할 수 있다(`BatchScheduleAdminController` 확인). 다만 이 기능은 화면(프론트)에 노출된 적이 없다.

즉 **백엔드 신규 개발 없이, 이미 있는 API를 화면에 노출하기만 하면 된다.**

## 범위

1. 탭별 "데이터 최신화" 버튼 → **"🔍 조회"**로 개명(아이콘+라벨). 동작은 변경 없음(기존 `refetch`, DB 재조회) — 이 버튼이 실제 데이터를 바꾸지 않는다는 것을 이름으로 분명히 한다.
2. 대시보드 헤더 우측(`styles.header`, 탭과 무관하게 공통 노출)에 **신규 공통 컴포넌트**를 추가한다.
   - 날짜 입력(기본값: 어제) + "▶ 주문 동기화 실행" 버튼
   - 클릭 시 `POST /api/admin/batch-schedules/order-sync/run?targetDate=<선택 날짜>` 호출 — 실제 Godo 운영 API를 호출해 해당 날짜 기준 21일 롤링 재수집을 수행한다(실제 데이터가 바뀌는 액션)
   - 최근 실행 시각/상태(`GET /api/admin/batch-schedules/order-sync`) 표시 — 기존 `GoodsPage`의 `useBatch`/`SummaryCards`/`formatLastRun` 패턴을 참고하되, `product-sync`에 하드코딩된 기존 `api/batch.ts`/`useBatch.ts`는 건드리지 않고 `order-sync` 전용으로 별도 파일을 신설한다(무관한 goods 화면 리팩토링 방지).
3. 이 화면을 이용해 사용자가 날짜를 21일 간격으로 바꿔가며 여러 번 실행하면 임의 과거 기간을 수동으로 백필할 수 있다 — **자동 청크 반복 실행은 이번 범위에 포함하지 않는다**(사용자가 수동 실행 방법을 요청했고, Godo API의 실제 기간 제약도 아직 검증되지 않았으므로).

## 범위 밖 (Next Steps로 남김)

- 여러 기간을 자동으로 순회하며 반복 호출하는 "일괄 백필" 기능
- Godo `Order_Search.php`의 실제 최대 조회 기간 검증(필요 시 수동 실행으로 직접 확인)

## 구현 방향

| 파일 | 내용 |
|---|---|
| `frontend/src/api/orderSync.ts`(신규) | `getOrderSyncSchedule()`, `runOrderSyncNow(targetDate)` — `types/batch.ts`의 기존 `BatchSchedule`/`BatchRunResult` 타입 재사용 |
| `frontend/src/hooks/useOrderSync.ts`(신규, TDD) | schedule 조회, 실행 상태(running/error) 관리 |
| `frontend/src/components/salesDashboard/OrderSyncControl.tsx`(신규) | 날짜 입력 + 실행 버튼 + 최근 실행 상태 표시 |
| `frontend/src/components/salesDashboard/OrderSyncControl.module.scss`(신규) | 헤더 우측에 배치되는 컴팩트 레이아웃 |
| `frontend/src/pages/DashboardPage.tsx` | 헤더에 `<OrderSyncControl />` 추가, 4개 탭의 "데이터 최신화" → "🔍 조회" 개명 |
| `frontend/src/pages/DashboardPage.module.scss` | 조회 버튼 스타일(보조 버튼 톤으로 조정) |

## 완료 기준

- [ ] `useOrderSync` TDD로 작성(RED→GREEN)
- [ ] 4개 탭 버튼 라벨이 "🔍 조회"로 변경되고 기존 동작(DB 재조회) 유지
- [ ] 헤더 우측 공통 컴포넌트가 4개 탭 전환과 무관하게 항상 노출
- [ ] 날짜 지정 후 실행 시 실제 `order-sync` 배치가 트리거됨(Playwright로 네트워크 요청 확인 — 실제 Godo 운영 API 호출은 사용자 확인 후에만 실행)
- [ ] `npm run build`/`lint`/`test` 통과
