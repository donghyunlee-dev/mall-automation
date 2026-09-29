# Iteration 06 Plan — 배치 관리

> 작성일: 2026-04-02  
> 담당: Sohee Kang  
> 상태: In Progress

---

## 목표

오늘 변경 이력 테이블, 배치 간격 변경 확인 모달을 구현하고 DashboardPage에 통합한다.

---

## 범위

| # | 항목 | 파일 |
|---|---|---|
| 1 | 오늘 변경 이력 테이블 | `src/components/dashboard/HistoryTable.tsx` |
| 2 | 테이블 스타일 | `src/components/dashboard/HistoryTable.module.scss` |
| 3 | 배치 간격 변경 확인 모달 | `src/components/modals/IntervalConfirmModal.tsx` |
| 4 | DashboardPage 업데이트 | HistoryTable 통합, IntervalConfirmModal 연결 |

---

## 완료 기준

- 오늘 변경 이력 테이블 정상 표시 (빈 경우 안내 문구)
- 배치 간격 변경 확인 모달 정상 동작 + 토스트 표시
- `npm run build` + `npm run lint` 오류 없음
