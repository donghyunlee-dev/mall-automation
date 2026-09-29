---
task: ATCS-230
title: FE - 전체 상품 목록 고도몰 상품코드 기준으로 정렬 및 코드 기준 업데이트로 수정
status: in-progress
---

## Requirement Summary

전체 상품 목록의 기본 정렬을 고도몰 상품코드 오름차순으로 변경하고, 상품 추가/수정 후에도 정렬과 페이지네이션 상태가 유지되도록 한다.

## Business Context

현재 기본 정렬이 `updatedAt DESC`로 되어 있어 최근 수정된 상품이 상단에 표시되지만, 실무에서는 코드 기준 정렬이 더 직관적이다.

## Scope

- `useGoods.ts` 기본 sortKey: `'updatedAt'` → `'godoGoodsCode'`
- `useGoods.ts` 기본 sortDir: `'desc'` → `'asc'`

## Out of Scope

- 정렬 로직 자체 변경
- 페이지네이션 리셋 로직 변경 (reload()는 이미 page/sort를 초기화하지 않음)

## Impacted Modules

- `src/hooks/useGoods.ts`

## Risk Analysis

초기 정렬 기준만 변경. `reload()` 후 page/sort 보존은 기존 코드에서 이미 동작함.
