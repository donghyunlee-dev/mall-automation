# Iteration 06 Result — 배치 관리

> 날짜: 2026-04-02  
> 담당: Sohee Kang  
> 상태: ✅ 완료

---

## 요약

오늘 변경 이력 테이블(HistoryTable), 배치 간격 변경 확인 모달(IntervalConfirmModal)을 구현하고 DashboardPage에 통합했다.

---

## 변경 파일

| 파일 | 내용 |
|---|---|
| `src/components/dashboard/HistoryTable.tsx` | 오늘 변경 이력 테이블 — changedAt 오름차순, 빈 상태 안내 |
| `src/components/dashboard/HistoryTable.module.scss` | 테이블 스타일 — 뱃지(범용/전용, 판매중/품절), AM/PM 시각 |
| `src/components/modals/IntervalConfirmModal.tsx` | 배치 간격 변경 확인 모달 |
| `src/pages/DashboardPage.tsx` | HistoryTable 통합, IntervalConfirmModal 연결, 간격 변경 흐름 변경 |

---

## 실행 명령

```bash
npm run build   # ✅ 통과
npm run lint    # ✅ 통과
```

---

## 핵심 결정 사항

- **HistoryTable**은 `allItems: GoodsItem[]` prop을 받아 `godoGoodsCode` 기준으로 상품명/구분 조회 (추가 API 호출 없음)
- **배치 간격 변경 흐름 변경**: AvatarDropdown "적용" → `handleRequestInterval` (모달 열기) → IntervalConfirmModal 확인 → API 호출 + 토스트
- **IntervalConfirmModal**의 `onConfirm` prop은 `() => Promise<void>` — DashboardPage가 async 함수로 제공

---

## Next Steps (현재 Iteration 범위 외)

- Iteration 07: Teams 웹훅 알림 (폴링 기반 배치 완료 감지, Adaptive Card 발송)
