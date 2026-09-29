---
task: ATCS-228
title: FE - 자동 실행 설정 영역 헤더로 위치 이동
---

## Requirement Summary

자동 실행 설정 UI를 AvatarDropdown 모달에서 헤더 영역으로 이동한다.

## Business Context

AvatarDropdown은 로그인 계정에 종속된 개인화 설정 영역이다. 자동 실행 설정은 계정 개인 설정이 아닌 시스템 동작 설정이므로 헤더 공용 영역으로 이동이 적절하다.

## Scope

- 헤더의 자동 실행 중 N분 간격 뱃지(statusPill) 제거
- AvatarDropdown의 "자동 실행 설정" 섹션 → 헤더 상태 동기화 버튼 왼쪽으로 이동
- "자동 실행 설정" 문구 제거, "실행 시간" → "자동 실행 시간"으로 변경

## Out of Scope

- 자동 실행 설정 기능 자체의 동작 변경 없음
- 알림 설정 등 다른 AvatarDropdown 항목 변경 없음

## Impacted Modules

- `src/components/layout/Header.tsx`
- `src/components/layout/Header.module.scss`
- `src/components/layout/AvatarDropdown.tsx`
- `src/components/layout/AvatarDropdown.module.scss`
- `src/components/layout/Header.test.tsx`
- `src/components/layout/AvatarDropdown.test.tsx`

## Risk Analysis

- AvatarDropdown에서 intervalMinutes, onApplyInterval props 제거 시 DashboardPage 연동 확인 필요 (Header에서 직접 처리하므로 DashboardPage 변경 불필요)
