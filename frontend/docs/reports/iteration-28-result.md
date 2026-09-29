# Iteration 28 결과 보고서 — 주문 동기화 에러 메시지 레이아웃 수정

> 완료일: 2026-09-09

## 완료 범위

`OrderSyncControl`의 상태/날짜입력/버튼을 `.row`로 묶고, 에러 메시지를 그 아래 별도 줄로 분리했다. 에러 메시지가 표시/해제되어도 버튼을 포함한 컨트롤 행의 위치·폭이 변하지 않는다.

## 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `frontend/src/components/salesDashboard/OrderSyncControl.tsx` | 상태/날짜입력/버튼을 `.row`로 감싸고, 에러 메시지를 그 바깥(아래)에 배치 |
| `frontend/src/components/salesDashboard/OrderSyncControl.module.scss` | `.wrap`을 `flex-direction: column`으로, 새 `.row` 클래스 추가, `.error`에 `max-width`/`text-align: right` 추가 |

## 검증

- `npm run build` — ✅
- `npm run lint` — ✅
- `npm run test` — ✅ 38 파일 / 194 테스트 전체 통과
- Playwright로 실패 메시지를 강제 발생시켜 확인 — ✅ 메시지가 버튼 아래 줄에 표시되고 버튼 위치 고정됨
