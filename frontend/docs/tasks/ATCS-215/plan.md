# ATCS-215 Plan

## Requirement Summary

- 로그인 화면과 대시보드 전반의 문구를 운영 용어에 맞게 정리한다.
- 헤더, 계정 드롭다운, 상품 목록, 상품 추가, 엑셀 업로드 UI의 불필요한 요소를 제거하거나 위치를 조정한다.
- 상품 추가 모달에서 범용 선택 상태가 더 명확하게 보이도록 스타일을 보정한다.

## Business Context

- 운영자가 자주 사용하는 로그인과 대시보드의 문구가 길거나 부정확하면 작업 속도와 이해도가 떨어진다.
- 상태 동기화, 상태 변경, 자동 실행 설정처럼 운영 의미가 직접 드러나는 표현으로 통일할 필요가 있다.
- 상품 추가 시 선택 상태가 눈에 잘 띄지 않으면 잘못된 구분으로 저장할 가능성이 있다.

## Scope

- 로그인 페이지 문구 및 버튼 아이콘 제거
- 헤더 상태 pill 위치 변경 및 동기화 버튼 문구 변경
- 계정 드롭다운 문구 변경
- 오늘 상태 변경 문구 변경
- 전체 상품 목록에서 동기화 일자 컬럼 제거
- 엑셀 업로드 관련 버튼/제목 문구 변경
- 상품 추가 모달 범용 선택 스타일 보정
- 후속 리뷰 반영으로 최근 동기화 시간 카드 제목, 상품 추가/수정 구분 안내문, 상품 목록 컬럼 폭을 재조정
- 최소 범위 테스트 보강 및 문서화

## Out Of Scope

- 로그인/대시보드 기능 동작 변경
- 배치 실행 로직 변경
- 상품 수정 모달의 숨김 필드 복구
- API 스펙 및 백엔드 변경

## Impacted Modules

- `src/pages/LoginPage.tsx`
- `src/pages/DashboardPage.tsx`
- `src/components/layout/Header.tsx`
- `src/components/layout/AvatarDropdown.tsx`
- `src/components/dashboard/SummaryCards.tsx`
- `src/components/dashboard/ProductTable.tsx`
- `src/components/modals/ProductAddModal.tsx`
- `src/components/modals/ProductEditModal.tsx`
- `src/components/modals/ExcelUploadModal.tsx`
- 관련 스타일 및 테스트 파일

## Architecture Considerations

- 이번 작업은 UI 문구/배치 수정이라 기존 훅, API, 상태 흐름은 유지한다.
- 컬럼 제거는 테이블 렌더링 구조와 `colSpan`만 맞추고 정렬 로직은 불필요한 확장을 피한다.
- 공통 모달 스타일의 범용 선택 강조는 상품 추가/수정 모두에 일관되게 반영되고, 구분 보조 문구는 제거한다.

## Risk Analysis

- 헤더 배치 수정 시 모바일 구간에서 폭이 부족할 수 있어 기존 반응형 규칙을 함께 조정해야 한다.
- 상품 목록 컬럼 제거 시 `colSpan`, `colgroup`, 빈 행 개수가 어긋나면 레이아웃이 깨질 수 있다.
- 문구 변경은 테스트 보호가 약한 영역이 많아 빌드/린트 외에 수동 UI 확인이 필요하다.
