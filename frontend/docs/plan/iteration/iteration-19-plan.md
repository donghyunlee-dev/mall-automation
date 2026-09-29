# Iteration 19 계획 — 회원 관리 수동 수집 버튼

> 작성일: 2026-09-07
> 관련: Iteration 18(주문 동기화 수동 실행 UI)과 동일한 패턴을 회원 관리 화면에 적용

## 배경

Iteration 18에서 대시보드에 "주문 동기화 수동 실행" UI를 추가한 것과 동일하게, 회원 관리(`/members`) 화면에도 기존 `member-sync` 배치(`MemberSyncBatchJob`)를 수동으로 트리거하는 버튼이 필요하다. 백엔드는 이미 `POST /api/admin/batch-schedules/member-sync/run`을 지원한다(`BatchScheduleAdminController` 공용 엔드포인트) — **백엔드 신규 개발 없음**.

## 범위

- `MembersPage` 카드 헤더(`.hdRight`, 엑셀 업로드/다운로드/거래처 등록 버튼이 있는 영역)에 "회원 수집" 버튼 추가
- 날짜 지정 없이 기존 API를 그대로 호출(`MemberSyncBatchJob.run()` 기본값 = 오늘) — 사용자가 날짜 파라미터를 요청하지 않았으므로 범위에 포함하지 않는다
- 최근 실행 상태(`GET /api/admin/batch-schedules/member-sync`) 표시
- Iteration 18과 마찬가지로 실제 배치가 오래 걸릴 수 있으므로 해당 API 호출에만 타임아웃을 넉넉히 설정

## 구현 방향

| 파일 | 내용 |
|---|---|
| `frontend/src/api/memberSync.ts`(신규) | `getMemberSyncSchedule()`, `runMemberSyncNow()` |
| `frontend/src/hooks/useMemberSync.ts`(신규, TDD) | 동기화 상태/실행 관리 훅 (order-sync와 동일 패턴, 날짜 파라미터 없음) |
| `frontend/src/pages/MembersPage.tsx` | 카드 헤더에 수집 버튼 + 최근 실행 상태 추가(기존 `.btnPrimary`/`.cardMeta` 스타일 재사용, 신규 CSS 없음) |

## 완료 기준

- [ ] `useMemberSync` TDD로 작성(RED→GREEN)
- [ ] `npm run build`/`lint`/`test` 통과
- [ ] 실제 `member-sync` API 호출까지 Playwright로 검증(사용자 확인 후)
