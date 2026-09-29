# Iteration 05 Result — 상품 관리 모달

> 날짜: 2026-04-01  
> 담당: Sohee Kang  
> 상태: ✅ 완료

---

## 요약

상품 추가/수정/삭제 모달과 엑셀 일괄 업로드 모달을 구현하고 DashboardPage에 통합했다. 공통 토스트 알림 컴포넌트도 추가했다.

---

## 변경 파일

| 파일 | 내용 |
|---|---|
| `src/components/modals/ProductAddModal.tsx` | 상품 추가 모달 — POST /api/goods |
| `src/components/modals/ProductEditModal.tsx` | 상품 수정 모달 — PUT /api/goods/{id} |
| `src/components/modals/ProductDeleteModal.tsx` | 상품 삭제 확인 모달 — DELETE /api/goods/{id} |
| `src/components/modals/ExcelUploadModal.tsx` | 엑셀 일괄 업로드 모달 — xlsx 파싱 + POST /api/goods |
| `src/components/modals/ExcelUploadModal.module.scss` | 업로드 모달 전용 스타일 |
| `src/components/common/Toast.tsx` | 토스트 알림 컴포넌트 |
| `src/components/common/Toast.module.scss` | 토스트 스타일 |
| `src/pages/DashboardPage.tsx` | 모달 연결, 엑셀/추가 버튼, 토스트 상태 관리 |
| `src/pages/DashboardPage.module.scss` | btnSmall, btnSmallOutline 스타일 추가 |
| `package.json` | xlsx 의존성 추가 |

---

## 실행 명령

```bash
npm run build   # ✅ 통과
npm run lint    # ✅ 통과
```

---

## 핵심 결정 사항

- **xlsx (SheetJS)** 라이브러리 추가 — ExcelUploadModal의 브라우저 기반 xlsx 파싱에 필수
- **Toast 자동 닫기**: DashboardPage의 `useEffect` 에서 `setTimeout(() => setToast(null), duration)` 패턴 적용 — async callback이므로 `react-hooks/set-state-in-effect` 규칙 미적용
- **모달 API 호출**: 모두 이벤트 핸들러(`handleSave`, `handleDelete`, `handleUpload`) 내 `.then()/.catch()` 체인으로 구현 — `useEffect` 내 setState 없음
- **엑셀 파싱**: 1행을 헤더로 간주하고 2행부터 데이터 읽음. D열 값 `전용` → `onlyMallYn: 'Y'`, 그 외 → `'N'`

---

## Next Steps (현재 Iteration 범위 외)

- Iteration 06: HistoryTable, 오늘 변경 이력 테이블, IntervalConfirmModal
- 번들 크기 최적화: xlsx 동적 import 고려 (Iteration 07 이후)
