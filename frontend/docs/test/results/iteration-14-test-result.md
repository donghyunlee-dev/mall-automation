# Iteration 14 Test Result — 대시보드 화면 (월간 탭 Phase B)

> 실행일: 2026-09-04
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

결과: ✅ 33 파일 / 164 테스트 전체 통과 (Iter 14 신규: `twoYearRange.test.ts` 3개, `monthlyTrendRange.test.ts`에 `monthEndDate` 케이스 1개 추가 — TDD로 작성)

---

## 브라우저 수동 확인

mock 서버(PORT=8099)에 `grade-pivot`/`yoy-comparison` 엔드포인트를 추가한 뒤, Playwright로 이메일 OTP 로그인 → `/dashboard` → "월간 (출고)" 탭에서 Phase B 섹션 전부를 확인했다.

| 항목 | 결과 |
|------|------|
| 일별 실적 상세 피벗 테이블(매출액/주문 건수/주문자 수 전환) | ✅ 확인 — 버튼 클릭 시 테이블 값 전환 |
| 일별 실적 상세 합계 행 | ✅ 확인 |
| 전년 동월 비교 콤보 차트 2종(매출·주문건수 / 구매자수·AOV) | ✅ 확인 |
| 최근 2개년 추이 콤보 차트(21개월, 2025-01~2026-09) | ✅ 확인 |
| 월별 실적 상세 피벗 테이블(7개 항목 전환) + 등급별 라인 차트 | ✅ 확인 — "AOV" 클릭 시 테이블+라인 차트 갱신, 합계 행 `-` 표시(평균 지표) |
| 콘솔 에러 없음 | ✅ 확인(대시보드 관련 에러 0건 — `/api/auth/me` 404는 기존 mock 서버의 무관한 엔드포인트) |

### 확인 중 발견·수정한 이슈

최초 구현한 mock 서버의 `grade-pivot` MONTH 버킷 날짜 생성 로직이 `Date.setMonth()`로 월을 거슬러 올라가면서 일부 월에서 중복된 날짜(예: 두 오프셋 모두 "2025-03"으로 귀결)를 만들어 React `key` 중복 경고가 발생했다. 연/월 정수 계산으로 교체해 해결 — **실제 백엔드(`YearMonth.minusMonths`)에는 이 문제가 없음, mock 서버 전용 버그**.

### 참고

- Phase A(일간/주간/월간 대시보드 기존 API)와 마찬가지로 mock 서버는 검증 전용이며 실제 백엔드는 이미 PR #209로 `dateBasis` 처리가 TDD 검증 완료된 상태.
- 분기 탭은 다음 이터레이션.
