# Iteration 09 결과 보고서 — 회원 목록 페이지

> 완료일: 2026-06-22
> 담당: Sohee Kang
> 브랜치: feature/iteration-09

---

## 완료 범위

회원 목록 페이지 (`/members`) 구현을 완료했습니다.

---

## 주요 결정 사항

| 결정 | 이유 |
|------|------|
| `cellPhone` 컬럼 제거 | PR #133에서 API 응답에서 제거됨 |
| `partnerName` 필드 추가 | PR #135에서 API 응답에 추가됨 |
| 날짜 필터(가입일) 구현 | `mockup-members.html` 기준 두 행 필터 UI 반영. 기본값 오늘, `오늘` 프리셋 활성 |
| 엑셀 다운로드 버튼 비활성화 | API 엔드포인트 미존재 — Iteration 10에서 검토 |
| URL 쿼리 파라미터 동기화 | `useSearchParams` 기반 — 상세 이동 후 뒤로가기 시 필터 복원 |
| 회원 상세 클릭 핸들러 placeholder | Iteration 10에서 모달/라우팅 구현 예정 |
| 뷰포트 꽉찬 레이아웃 | mockup-members.html의 flex stretch 구조 반영. body → page → main → section → ucard → tableWrap 전 계층 flex:1 적용 |

---

## 신규 파일

| 파일 | 역할 |
|------|------|
| `src/types/member.ts` | 회원 도메인 타입 |
| `src/api/members.ts` | `getMembers`, `uploadMembers` API |
| `src/hooks/useMembers.ts` | URL 동기화 목록 상태 훅 |
| `src/hooks/useMemberUpload.ts` | SheetJS 파싱 + 업로드 훅 |
| `src/utils/memberGroup.ts` | group_sno → 배지 매핑 |
| `src/utils/parseMemberExcel.ts` | 파일 종류 자동 판별 + SheetJS 파싱 |
| `src/components/members/MemberSearchBar.tsx` | 등급·검색타입·키워드·페이지크기 검색바 |
| `src/components/members/MemberTable.tsx` | 정렬·페이징 회원 테이블 |
| `src/components/modals/MemberUploadModal.tsx` | 5단계 업로드 모달 |
| `src/pages/MembersPage.tsx` | 목록 페이지 (플레이스홀더 대체) |
| `src/pages/MembersPage.module.scss` | 페이지 스타일 |

---

## 수정 사항 (구현 후 보완)

| 항목 | 내용 |
|------|------|
| 빈 상태 업로드 버튼 제거 | 카드 헤더 우측 상단에 이미 있으므로 텍스트 안내로 대체 |
| 결과 카운트 바 제거 | mockup에 없는 항목 — "전체 N명 중 1–10" 제거 |
| 뷰포트 꽉찬 레이아웃 적용 | `_reset.scss` body에 flex column 추가, 각 계층 `flex: 1 + min-height: 0` |

---

## 검증

- `npm run build` — ✅ TypeScript 오류 없음
- `npm run test -- --run` — ✅ 73 테스트 전체 통과 (신규 28개 포함)

---

## 다음 단계 (Iteration 10)

- 회원 상세 모달 구현 (`onRowClick` 핸들러 완성)
- 엑셀 다운로드 API 연동
- `/api/members/summary` 요약 카드 연동
- 가입일 날짜 필터 추가
