# Iteration 29 계획 — 대기 메시지 폭 조정 + StrictMode 버튼 잠김 버그 수정

> 작성일: 2026-09-09

## 배경

"서버에서 계속 실행 중이에요..." 메시지가 좁은 폭(`max-width: 320px`) 때문에 2줄로 표시됐다. 이 메시지는 이미 별도 줄(Iteration 28)에 있어 폭을 넓혀도 버튼 위치에 영향이 없다.

시각 확인 과정에서 별개의 실제 버그를 발견했다: `unmountedRef`를 `useEffect(() => () => { unmountedRef.current = true }, [])`로만 관리해서, React 18 StrictMode의 개발 모드 이중 마운트(mount→cleanup→mount)를 거치면 `unmountedRef.current`가 `true`로 남은 채 되돌아오지 않아 버튼이 영구히 "실행 중..."에 잠기고 폴링도 전혀 동작하지 않는 문제였다. (프로덕션 빌드는 StrictMode 이중 호출이 없어 실사용 영향은 없지만, 로컬 개발 확인 시 재현됨 — 근본적으로 잘못된 패턴이라 수정)

## 범위

- `.error`의 `max-width` 제한 제거, `white-space: nowrap`으로 한 줄 유지
- `unmountedRef`를 마운트 시 명시적으로 `false`로 재설정하도록 수정

## 완료 기준

- [x] Playwright로 "서버에서 계속 실행 중이에요..." 메시지가 한 줄로 표시됨을 확인
- [x] 버튼이 정상적으로 잠금 해제됨을 확인(StrictMode 버그 수정 검증)
- [x] `npm run build`/`lint`/`test` 통과
