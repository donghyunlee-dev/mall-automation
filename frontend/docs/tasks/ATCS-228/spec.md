---
task: ATCS-228
title: FE - 자동 실행 설정 영역 헤더로 위치 이동
---

## Feature Specification

### 제거

- Header의 `statusPill` (자동 실행 중 · N분 간격 뱃지) 제거
- AvatarDropdown의 "자동 실행 설정" ddSection 전체 제거
- AvatarDropdown Props에서 `intervalMinutes`, `onApplyInterval` 제거

### 추가

- Header hdrRight 영역에서 상태 동기화 버튼 왼쪽에 자동 실행 시간 인라인 컨트롤 추가
  - 레이블: "자동 실행 시간"
  - number input (min=1, max=60)
  - 단위: "분"
  - "적용" 버튼
- schedule이 있을 때만 표시 (기존 statusPill 조건과 동일)

## Text Changes

| Before | After |
|--------|-------|
| "자동 실행 설정" (AvatarDropdown 레이블) | 제거 |
| "실행 시간" (AvatarDropdown interval 레이블) | "자동 실행 시간" (Header 인라인) |

## Acceptance Criteria Mapping

| AC | 구현 |
|----|------|
| 헤더의 자동실행 중: N분 간격 뱃지 제거 | statusPill 블록 제거 |
| 자동 실행 설정 영역을 상태 동기화 버튼 왼쪽으로 이동 | Header hdrRight에 runBtn 앞에 컨트롤 추가 |
| "자동 실행 설정" 문구 제거, "실행 시간" → "자동 실행 시간" | 텍스트 변경 |
