# Iteration 07 Plan — Teams 웹훅 알림

> 작성일: 2026-04-02  
> 담당: Sohee Kang  
> 상태: In Progress

---

## 목표

배치 실행 완료를 폴링으로 감지하여 판매 상태 변경 이력이 있으면 Teams Adaptive Card 알림을 발송한다.

---

## 범위

| # | 항목 | 파일 |
|---|---|---|
| 1 | Teams 타입 정의 | `src/types/teams.ts` |
| 2 | Teams 웹훅 API 함수 | `src/api/teams.ts` |
| 3 | 폴링 + 알림 훅 | `src/hooks/useTeamsNotification.ts` |
| 4 | DashboardPage 통합 | `useTeamsNotification` 호출 추가 |
| 5 | 환경변수 추가 | `.env`, `.env.production` |

---

## 완료 기준

- 배치 실행 후 변경 이력 있을 때 Teams 카드 발송 (폴링 감지)
- 배치 오류 시 오류 알림 카드 발송
- 변경 이력 없으면 미발송
- `npm run build` + `npm run lint` 오류 없음
