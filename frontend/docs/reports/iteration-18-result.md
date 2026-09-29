# Iteration 18 결과 보고서 — 주문 동기화 수동 실행 UI

> 완료일: 2026-09-07
> 담당: Sohee Kang
> 브랜치: (dev-branch-pr 플로우로 생성)

---

## 배경

주간 탭 데이터가 원본과 차이 난다는 문의를 조사하는 과정에서, 사용자가 대시보드의 "데이터 최신화" 버튼을 실제 Godo 재수집 기능으로 오해하고 있다는 것이 드러났다. 확인 결과 그 버튼은 `GET /api/dashboard/*`(우리 DB 재조회)만 호출했고, 실제로 Godo에 주문을 다시 요청하는 기능(`OrderSyncBatchJob`, 21일 롤링)은 이미 백엔드에 구현돼 있었지만 화면에 노출된 적이 없었다(`POST /api/admin/batch-schedules/order-sync/run?targetDate=`로 이미 임의 날짜 지정을 지원).

## 완료 범위

**백엔드 신규 개발 없이**, 이미 있는 API를 화면에 노출했다.

1. 탭별 "데이터 최신화" 버튼 → **"🔍 조회"**로 개명(4개 탭). 동작 변경 없음(기존 DB 재조회) — 버튼이 데이터를 바꾸지 않는다는 것을 이름으로 분명히 함. 스타일도 보조 버튼 톤으로 조정.
2. 대시보드 헤더 우측(탭 전환과 무관하게 공통 노출)에 **신규 공통 컴포넌트(`OrderSyncControl`)** 추가.
   - 날짜 입력(기본값: 어제) + "▶ 주문 동기화 실행" 버튼(실제 데이터를 바꾸는 액션임을 나타내는 진한 색 버튼)
   - 클릭 시 실제 Godo 운영 API를 호출해 해당 날짜 기준 21일 롤링 재수집 수행
   - 최근 실행 시각/상태 표시(`GET /api/admin/batch-schedules/order-sync`)
3. 사용자가 날짜를 21일 간격으로 바꿔가며 이 버튼을 반복 실행하면, 별도의 자동 백필 로직 없이도 임의 과거 기간을 수동으로 채울 수 있다.

## 주요 결정 사항

| 결정 | 이유 |
|------|------|
| 기존 `api/batch.ts`/`useBatch.ts`(goods `product-sync` 전용)를 건드리지 않고 `order-sync` 전용 파일 신설 | 무관한 상품 관리 화면 리팩토링 방지, 관련 없는 코드 변경 최소화 |
| 자동 청크 백필(여러 기간을 자동 순회)은 이번 범위에서 제외 | 사용자가 "수동 실행 방법"을 요청했고, Godo API의 실제 최대 조회 기간 제약도 아직 검증되지 않아 우선 수동 방식으로 안전하게 노출 |
| `runOrderSyncNow` API 호출에만 타임아웃 2분 지정 | 실제 배치가 30~50초 걸리는 동기 처리라 전역 10초 타임아웃에 걸림 — 전역 설정은 다른 API에 영향을 주므로 이 호출에만 개별 적용 |

## 신규 파일

| 파일 | 역할 |
|------|------|
| `frontend/src/api/orderSync.ts` | `getOrderSyncSchedule()`, `runOrderSyncNow(targetDate)` |
| `frontend/src/hooks/useOrderSync.ts`(+test, TDD) | 동기화 상태/실행 관리 훅 |
| `frontend/src/components/salesDashboard/OrderSyncControl.tsx`(+scss) | 헤더 우측 공통 컴포넌트 |
| `frontend/docs/plan/iteration/iteration-18-plan.md` | 이터레이션 계획 |

## 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `frontend/src/pages/DashboardPage.tsx` | 헤더에 `<OrderSyncControl />` 추가, 4개 탭 "데이터 최신화" → "🔍 조회" 개명 |
| `frontend/src/pages/DashboardPage.module.scss` | `.refreshBtn` → `.searchBtn`(보조 버튼 스타일로 변경) |

---

## 검증

- `npm run build` — ✅ TypeScript 오류 없음
- `npm run lint` — ✅ 오류·경고 0
- `npm run test` — ✅ 36 파일 / 173 테스트 전체 통과
- 브라우저 수동 확인 — ✅ 완료(Playwright, 실제 Godo 운영 API 호출까지 엔드투엔드 검증, 발견된 클라이언트 타임아웃 이슈 수정·재검증 포함). 상세: `docs/test/results/iteration-18-test-result.md`

---

## Open Gaps / 다음 단계

- 자동 청크 백필(여러 기간 자동 순회 실행) — 이번 범위 밖. 필요 시 별도 Iteration으로 진행
- Godo `Order_Search.php`의 실제 최대 조회 기간 제약 — 아직 미확인(월 단위 이상 넓은 범위로 수동 실행해보며 확인 가능)
- `docs/dashboard-integration/dashboard-prd.md` R4(2년 백필) — 이번 기능으로 "수동 반복 실행"은 가능해졌으나, 자동화된 일괄 백필 자체는 여전히 미구현 상태로 남겨둠(로드맵 갱신 필요)
