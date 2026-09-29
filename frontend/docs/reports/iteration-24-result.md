# Iteration 24 결과 보고서 — 인기 품목/잠봉 차트 타이틀·축 제목 보정 (R21)

> 완료일: 2026-09-09
> 관련: 사용자가 원본 서비스에 직접 접속(제공된 비밀번호)해 실측 비교한 결과 반영

## 완료 범위

- `TopItemsChart` 사용처(4개 탭 × 인기 품목/잠봉 슬라이스 = 8곳) 타이틀에 "(주문금액 & 주문수량)" 접미사 추가
- `TopItemsChart`에 X축 제목 "주문금액 (원)" 추가
- 도넛:트렌드 차트 비율(`.grid2`)은 최초 요청으로 2:1→3:2로 넓혔으나, 원본을 Playwright로 직접 실측(`boxes:true`)한 결과 원본도 정확히 2:1(1127px:547px)이어서 **2:1로 원복** — 최초 비율이 이미 맞았음이 확인됨

## 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `frontend/src/components/salesDashboard/TopItemsChart.tsx` | X축 제목 "주문금액 (원)" 추가 |
| `frontend/src/pages/DashboardPage.tsx` | `TopItemsChart` 8곳의 `title` prop에 "(주문금액 & 주문수량)" 접미사 추가 |

`DashboardPage.module.scss`의 `.grid2`는 실측 후 최초 값(`2fr 1fr`)으로 원복해 실제 diff 없음.

## 신규 발견(범위 제외, 기록만)

원본은 일간/주간 탭 핵심 지표 첫 카드에도 "일간 총 매출 (VAT-)"/"주간 총 매출 (VAT-)" 접두어를 쓴다. R12(Iteration 22)는 월간/분기만 반영해 완료 처리했으나, 이 발견에 따르면 일간/주간도 원본과 다르다. 사용자 확인 후 별도 착수 필요.

## 검증

- `npm run build` — ✅ TypeScript 오류 없음
- `npm run lint` — ✅ 오류·경고 0
- `npm run test` — ✅ 38 파일 / 191 테스트 전체 통과
- 원본 서비스 직접 접속 실측 + 우리 서비스 브라우저 확인 — ✅. 상세: `docs/test/results/iteration-24-test-result.md`
