# Iteration 21 검증 결과 — 차트 스타일 보정 (R7/R8/R11)

> 검증일: 2026-09-09

## 자동 검증

| 명령 | 결과 |
|---|---|
| `npm run build` | ✅ TypeScript 오류 없음 |
| `npm run lint` | ✅ 오류·경고 0 |
| `npm run test` | ✅ 38 파일 / 191 테스트 전체 통과 (신규 `getTrendAxisTitle` 4건 포함) |

## TDD 진행

- `dashboardLabel.test.ts`에 `getTrendAxisTitle` 케이스 추가 → RED 확인(함수 없음) → 구현 → GREEN(15/15 통과)

## 브라우저 수동 확인 (Playwright, mock-server 기준)

| 항목 | 확인 위치 | 결과 |
|---|---|---|
| R7 AOV 헤더 구분 | 일간/주간 탭 등급별 상세 지표 | ✅ "객단가" 유지 |
| R7 AOV 헤더 구분 | 월간/분기 탭 등급별 상세 지표 | ✅ "AOV"로 표시 |
| R8 축 제목 | 시간대별 매출 추이(일간) | ✅ X축 "주문시간", Y축 "매출액" |
| R8 축 제목 | 트렌드 차트(4개 탭 전체) | ✅ X축이 그래뉼래리티별로 "일자"/"주차 (Week)"/"월"/"분기", Y축(좌) "매출액", Y축(우) "주문건수" |
| R8 축 제목 | 전년동월/동분기 비교(YoY) 차트 | ✅ Y축(좌/우)에 각각 barLabel/lineLabel("매출액"/"주문건수", "구매자 수"/"AOV") 표시 |
| R11 도넛 hole | 등급별 매출 비중(4개 탭 전체) | ✅ `cutout: '40%'` 적용 확인 |

## 참고

- 브라우저 확인 중 mock-server.js에 `/api/dashboard/jambong-breakdown` 스텁을 임시로 추가해 확인 후 `git checkout`으로 되돌렸다(Iteration 20에서와 동일한 이유 — 범위 외 변경이라 커밋에 포함하지 않음).
