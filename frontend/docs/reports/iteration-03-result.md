# Iteration 03 Result — 대시보드 레이아웃

> 날짜: 2026-04-01  
> 담당: Sohee Kang  
> 상태: ✅ 완료

---

## 요약

Header(로고, 상태 pill, 동기화 버튼, 아바타 드롭다운), 현황 요약 카드 3개, DashboardPage 레이아웃을 구현했다. 세션 확인, 배치 상태 연동, 상품/이력 집계를 포함한다.

---

## 변경 파일

| 파일 | 내용 |
|---|---|
| `src/types/batch.ts` | BatchSchedule, BatchRunResult, BatchScheduleUpdateBody |
| `src/types/goods.ts` | GoodsItem, GoodsListResponse, GoodsStatusLog 등 전체 상품 타입 |
| `src/api/batch.ts` | getBatchSchedule, runBatchNow, updateBatchSchedule |
| `src/api/goods.ts` | getGoods, getGoodsById, createGoods, updateGoods, deleteGoods, getGoodsStatusLogs |
| `src/hooks/useBatch.ts` | 배치 상태 관리, 수동 실행, 간격 변경 |
| `src/components/layout/Header.tsx` | 헤더 컴포넌트 |
| `src/components/layout/Header.module.scss` | 헤더 스타일 |
| `src/components/layout/AvatarDropdown.tsx` | 아바타 드롭다운 컴포넌트 |
| `src/components/layout/AvatarDropdown.module.scss` | 드롭다운 스타일 |
| `src/components/dashboard/SummaryCards.tsx` | 현황 요약 카드 3개 |
| `src/components/dashboard/SummaryCards.module.scss` | 요약 카드 스타일 |
| `src/pages/DashboardPage.tsx` | 대시보드 페이지 레이아웃 |
| `src/pages/DashboardPage.module.scss` | 대시보드 스타일 |

---

## 실행 명령

```bash
npm run build   # ✅ 통과
npm run lint    # ✅ 통과
```

---

## 핵심 결정 사항

- `refreshKey` state 패턴 사용: 배치 실행 완료 후 상품/이력 재조회 트리거
- 세션 확인은 DashboardPage 마운트 시 수행 — 실패 시 `/login` 리다이렉트
- 로그아웃은 UI(확인 모달)만 구현 — 백엔드 API 미구현
- 전체 상품 목록 / 변경 이력 섹션은 placeholder — Iteration 04/06에서 구현

---

## Next Steps (현재 Iteration 범위 외)

- Iteration 04: 상품 목록 테이블 (검색/필터/정렬/페이지네이션)
- Iteration 05: 상품 추가/수정/삭제/엑셀업로드 모달
