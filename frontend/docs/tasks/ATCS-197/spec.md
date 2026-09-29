# ATCS-197 Spec

## Feature Specification
전체 상품 목록 필터 버튼 그룹(품절/판매중)의 오른쪽에 '확인불가' 버튼 추가.

## Filter Logic
- `FilterKey`: `'unavailable'` 추가
- `applyFilter` case `'unavailable'`: `g.status === 'UNVERIFIED'`인 항목만 반환

## UI Spec
- 위치: 판매중 버튼 오른쪽 (동일한 filterGroup 내)
- 비활성 스타일: 기존 `.fbtn`과 동일 (var(--surface), var(--text2))
- 활성 스타일: 엠버 계열
  - background: amber-bg (투명도 있는 엠버)
  - color: #d97706
  - border-color: rgba(217, 119, 6, 0.3)
  - font-weight: 500

## Acceptance Criteria Mapping
| AC | 구현 |
|----|------|
| 판매중 오른쪽에 확인불가 버튼 추가 | FILTER_BUTTONS slice(0,4) 에 포함 |
| 비활성 시 기존 색과 동일 | `.fbtn` 기본 스타일 그대로 |
| 활성 시 엠버색 #d97706 | `.fu` 클래스 적용 |
| 확인불가 품목만 필터 | status === 'UNVERIFIED' |
