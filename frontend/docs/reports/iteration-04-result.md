# Iteration 04 Result — 상품 목록 테이블

> 날짜: 2026-04-01  
> 담당: Sohee Kang  
> 상태: ✅ 완료

---

## 요약

전체 상품 목록 테이블, 검색바, 페이지네이션 컴포넌트를 구현하고 DashboardPage에 통합했다. 프론트엔드 기반 검색/필터/정렬/페이지네이션이 정상 동작한다.

---

## 변경 파일

| 파일 | 내용 |
|---|---|
| `src/hooks/useGoods.ts` | 상품 목록 상태 관리, 필터/정렬/페이지네이션, refreshKey 패턴 |
| `src/components/dashboard/ProductSearchBar.tsx` | 검색 입력, 필터 버튼 그룹 |
| `src/components/dashboard/ProductSearchBar.module.scss` | 검색바 스타일 |
| `src/components/dashboard/ProductTable.tsx` | 테이블 9컬럼, 정렬 아이콘, 뱃지, 행 액션 |
| `src/components/dashboard/ProductTable.module.scss` | 테이블 스타일 |
| `src/components/dashboard/Pagination.tsx` | 페이지 버튼, 이전/다음 |
| `src/components/dashboard/Pagination.module.scss` | 페이지네이션 스타일 |
| `src/pages/DashboardPage.tsx` | ProductTable 통합, 상품목록 섹션 완성 |
| `src/pages/DashboardPage.module.scss` | pill, hdRight, devNote 스타일 추가 |

---

## 실행 명령

```bash
npm run build   # ✅ 통과
npm run lint    # ✅ 통과
```

---

## 핵심 결정 사항

- 필터/검색/정렬은 API 재호출 없이 프론트엔드에서 처리
- `refreshKey` 패턴 일관 적용 — 모든 데이터 재조회는 key 증가로 트리거
- 수정/삭제 모달은 Iteration 05 구현 예정 — 임시 dev note로 표시

---

## Next Steps (현재 Iteration 범위 외)

- Iteration 05: ProductAddModal, ProductEditModal, ProductDeleteModal, ExcelUploadModal
