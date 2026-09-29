# Iteration 07 Result — Teams 웹훅 알림

> 날짜: 2026-04-02  
> 담당: Sohee Kang  
> 상태: ✅ 완료

---

## 요약

배치 실행 완료를 폴링으로 감지하여 판매 상태 변경 이력이 있으면 Teams Adaptive Card 알림을 발송하는 훅을 구현했다. Power Automate HTTP 트리거 URL로 전송한다.

---

## 변경 파일

| 파일 | 내용 |
|---|---|
| `src/types/teams.ts` | Adaptive Card v1.4 타입 정의 |
| `src/api/teams.ts` | 카드 빌더 함수, `sendTeamsNotification` (fetch 기반) |
| `src/hooks/useTeamsNotification.ts` | 30초 폴링, 배치 완료 감지, 알림 발송 |
| `src/pages/DashboardPage.tsx` | `useTeamsNotification()` 호출 추가 |
| `.env` | `VITE_TEAMS_WEBHOOK_URL`, `VITE_DASHBOARD_URL` 추가 |
| `.env.production` | `VITE_DASHBOARD_URL` 추가 |

---

## 실행 명령

```bash
npm run build   # ✅ 통과
npm run lint    # ✅ 통과
```

---

## 핵심 결정 사항

- **폴링 간격**: 30초 (`POLL_INTERVAL_MS = 30_000`)
- **최초 폴링**: `prevEndedAt.current === undefined` 로 초기화 여부 판별 — 최초에는 기준값만 설정하고 알림 미발송 (페이지 로드 시 spurious 알림 방지)
- **Teams 웹훅 전송**: `fetch` 사용 — 외부 URL에 `axios` withCredentials 적용 불필요
- **웹훅 URL 미설정 시**: `sendTeamsNotification`에서 `Promise.resolve()` 로 무해하게 처리
- **Adaptive Card v1.4**: FactSet + ColumnSet 조합으로 테이블 구성. `Action.OpenUrl`로 대시보드 링크 삽입
- **VITE_DASHBOARD_URL**: 프로덕션 배포 시 실제 EC2 호스트로 교체 필요

---

## 운영 설정 가이드

`.env.production` 에서 반드시 채워야 할 항목:

```
VITE_TEAMS_WEBHOOK_URL=<Power Automate HTTP 트리거 URL>
VITE_DASHBOARD_URL=<배포된 대시보드 URL>
```

---

## Next Steps (전체 PRD 완료)

7개 Iteration 모두 완료. 추가 고려 사항:
- xlsx 동적 import 적용으로 번들 크기 최적화
- Teams 알림 발송 실패 시 재시도 로직
- 폴링 간격 환경변수화 (`VITE_POLL_INTERVAL_MS`)
