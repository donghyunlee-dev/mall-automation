# Iteration 19 Test Result — 회원 관리 수동 수집 버튼

> 실행일: 2026-09-07
> 담당: Sohee Kang

---

## 빌드 검증

```
npm run build
```

결과: ✅ TypeScript 오류 없음. 빌드 성공.

---

## 린트 검증

```
npm run lint
```

결과: ✅ 오류·경고 0 (`--max-warnings 0`)

---

## 테스트 실행

```
npm run test
```

결과: ✅ 37 파일 / 176 테스트 전체 통과 (Iter 19 신규: `useMemberSync.test.ts` 3개 — TDD로 작성: RED(모듈 없음) → GREEN)

---

## 브라우저 수동 확인 (실제 백엔드 + 실제 회원 수집 API)

로컬 백엔드(포트 8090, 실 데이터) + Vite dev 서버로 Playwright에서 확인했다.

| 항목 | 결과 |
|------|------|
| 회원 관리 카드 헤더에 "회원 수집" 버튼 + 최근 실행 상태 노출 | ✅ 확인 — 기존 엑셀 업로드/다운로드/거래처 일괄 등록 버튼과 나란히 배치, `.btnPrimary`/`.cardMeta` 스타일 재사용으로 신규 CSS 없이 통일감 유지 |
| "회원 수집" 클릭 → 실제 `POST /api/admin/batch-schedules/member-sync/run` 호출 | ✅ 확인 |
| 실제 회원 크롤링 API 동기화 완료까지 확인 | ✅ 확인 — DB `batch_execution_log`에 `SUCCESS`, "신규 3건, 업데이트 0건" 기록(6초 소요), 프론트에도 "방금 전 · 정상 완료"로 정확히 반영, 타임아웃 이슈 없음 |

### 참고

- Iteration 18(주문 동기화 수동 실행 UI)과 동일한 패턴 — 백엔드 신규 개발 없이 기존 `member-sync` 배치 엔드포인트를 화면에 노출
- 날짜 파라미터는 요청 범위에 없어 추가하지 않음(`MemberSyncBatchJob.run()` 기본값 = 오늘)
