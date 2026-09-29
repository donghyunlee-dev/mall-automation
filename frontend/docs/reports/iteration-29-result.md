# Iteration 29 결과 보고서 — 대기 메시지 폭 조정 + StrictMode 버튼 잠김 버그 수정

> 완료일: 2026-09-09

## 완료 범위

1. "서버에서 계속 실행 중이에요..." 메시지의 `max-width: 320px` 제한을 제거하고 `white-space: nowrap`으로 한 줄 유지
2. **버그 수정(시각 확인 중 발견)**: `unmountedRef`가 React 18 StrictMode의 개발 모드 이중 마운트를 거치면 `true`로 고정되어 버튼이 영구히 잠기고 폴링이 전혀 동작하지 않는 문제 — 마운트 시 명시적으로 `false`로 재설정하도록 수정

## 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `frontend/src/components/salesDashboard/OrderSyncControl.module.scss` | `.error`의 `max-width` 제거, `white-space: nowrap` 추가 |
| `frontend/src/hooks/useOrderSync.ts` | `unmountedRef` 초기화 `useEffect`를 마운트 시 `false` 재설정 + 언마운트 시 `true`로 구조 변경 |

## 검증

- `npm run build` — ✅
- `npm run lint` — ✅
- `npm run test` — ✅ 38 파일 / 194 테스트 전체 통과
- Playwright로 504 응답을 강제 발생시켜 확인 — ✅ "서버에서 계속 실행 중이에요..." 메시지가 한 줄로 표시, 버튼이 정상적으로 잠금/해제됨(StrictMode 버그 수정으로 최초 확인 시도에서 재현했던 영구 잠김 현상 해결)

## 참고

StrictMode 이중 마운트는 개발 모드(`npm run dev`) 전용 동작이라 프로덕션 빌드에는 영향이 없었지만, 근본적으로 잘못된 ref 관리 패턴이라 향후 유사한 재확인/디버깅 과정에서 다시 혼란을 줄 수 있어 이번에 함께 고쳤다.
