---
task: ATCS-228
title: FE - 자동 실행 설정 영역 헤더로 위치 이동
---

## Development Tasks

- [x] AvatarDropdown.tsx: intervalMinutes, onApplyInterval props 제거 / ddSection("자동 실행 설정") 제거
- [x] AvatarDropdown.module.scss: ddInterval 관련 스타일 제거
- [x] Header.tsx: statusPill 제거 / 자동 실행 시간 인라인 컨트롤 추가 (runBtn 왼쪽)
- [x] Header.module.scss: statusPill 관련 스타일 제거 / 인라인 컨트롤 스타일 추가
- [x] AvatarDropdown.test.tsx: props 업데이트, "자동 실행 설정" 관련 테스트 수정
- [x] Header.test.tsx: props 유지 (onApplyInterval은 Header에 남음)

## Result

### Changed Files

- `src/components/layout/AvatarDropdown.tsx`
- `src/components/layout/AvatarDropdown.module.scss`
- `src/components/layout/AvatarDropdown.test.tsx`
- `src/components/layout/Header.tsx`
- `src/components/layout/Header.module.scss`

### Summary

자동 실행 설정 UI를 AvatarDropdown에서 Header 인라인 컨트롤로 이동. statusPill 제거. "자동 실행 시간" 레이블 적용.

### Test Results

- 빌드 및 테스트 결과 아래 기록

### Review Notes

- DashboardPage.tsx 변경 불필요: onApplyInterval은 Header에 유지
