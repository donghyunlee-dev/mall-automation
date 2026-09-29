# ATCS-216 Tasks

## Development Tasks

- 전체 상품 목록 카드 헤더에 `엑셀 다운로드` 버튼을 추가한다.
- 전체 상품 데이터를 엑셀 행 구조로 변환하는 유틸을 구현한다.
- `xlsx`를 사용해 시트명, 헤더, 컬럼 폭이 적용된 `.xlsx` 파일을 생성한다.
- 빈 데이터 또는 생성 실패 시 토스트로 사용자에게 상태를 알린다.
- 관련 테스트와 문서를 추가한다.

## Test Cases To Implement

- 상품 데이터가 엑셀 다운로드용 행 구조로 올바르게 변환되는지 확인
- 파일명이 날짜/시간 형식으로 생성되는지 확인

## Modules To Modify

- `src/pages/DashboardPage.tsx`
- `src/pages/DashboardPage.module.scss`
- `src/utils/exportGoodsExcel.ts`
- `src/utils/exportGoodsExcel.test.ts`

## New Classes Or Components

- 없음

## Integration Points

- 기존 `useGoods`의 `allItems`
- 기존 공통 토스트 표시 흐름
- 기존 `xlsx` 라이브러리

## Result

### Changed Files

- `docs/tasks/ATCS-216/plan.md`
- `docs/tasks/ATCS-216/spec.md`
- `docs/tasks/ATCS-216/tasks.md`
- `src/pages/DashboardPage.tsx`
- `src/pages/DashboardPage.module.scss`
- `src/utils/exportGoodsExcel.ts`
- `src/utils/exportGoodsExcel.test.ts`

### Summary

- 전체 상품 목록 카드 헤더에 `엑셀 다운로드` 버튼을 추가했다.
- 다운로드는 페이지네이션이나 필터 상태와 무관하게 `allItems` 전체 데이터를 기준으로 `.xlsx` 파일을 생성하도록 구현했다.
- 엑셀 데이터 변환, 파일명 규칙, 시트/컬럼 폭 설정을 유틸로 분리했다.

### Test Results

- `npm run build` 통과
- `npm run lint` 통과
- `npm test -- --run src/utils/exportGoodsExcel.test.ts` 통과
- 브라우저 수동 UI 확인은 이번 턴에서 실행하지 않음

### Review Notes

- 기존 워크트리의 백엔드 변경(`backend/*`)과 `.codex`는 이번 작업 범위에 포함하지 않았다.
- 저장소 규칙에 따라 브랜치 생성, 커밋, 푸시, PR 생성은 수행하지 않았다.
