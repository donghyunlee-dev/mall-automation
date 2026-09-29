# Iteration 05 Plan — 상품 관리 모달

> 작성일: 2026-04-01  
> 담당: Sohee Kang  
> 상태: In Progress

---

## 목표

상품 추가/수정/삭제 모달과 엑셀 일괄 업로드 모달을 구현한다.

---

## 범위

| # | 항목 | 파일 |
|---|---|---|
| 1 | 상품 추가 모달 | `src/components/modals/ProductAddModal.tsx` |
| 2 | 상품 수정 모달 | `src/components/modals/ProductEditModal.tsx` |
| 3 | 상품 삭제 확인 모달 | `src/components/modals/ProductDeleteModal.tsx` |
| 4 | 엑셀 업로드 모달 | `src/components/modals/ExcelUploadModal.tsx` |
| 5 | 모달 공통 스타일 | `src/components/modals/Modal.module.scss` |
| 6 | DashboardPage 업데이트 | 모달 버튼/핸들러 연결 |

---

## 완료 기준

- UI가 `docs/design/mockup.html` 모달 기준과 일치
- 추가/수정/삭제 API 연동 정상 동작
- 엑셀 업로드 정상 동작
- `npm run build` + `npm run lint` 오류 없음
