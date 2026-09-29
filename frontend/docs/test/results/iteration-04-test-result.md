# Iteration 04 Test Result — 상품 목록 테이블

> 날짜: 2026-04-01  
> 담당: Sohee Kang

---

## 실행 명령 및 결과

### Build Validation

```bash
npm run build
```

**결과**: ✅ 통과

```
✓ 114 modules transformed.
dist/index.html                0.49 kB
dist/assets/index-47qTADE-.css 19.73 kB
dist/assets/index-B0Reqj4M.js 299.53 kB
✓ built in 1.81s
```

---

### Lint Validation

```bash
npm run lint
```

**결과**: ✅ 통과

---

## 실패 케이스 및 수정 내용

| 오류 | 원인 | 수정 |
|---|---|---|
| `react-hooks/set-state-in-effect` | `useGoods.ts`의 `load()` useCallback 내 `setLoading(true)` 동기 호출 후 effect에서 `load()` 실행 | `useGoods` 내부에도 `refreshKey` 패턴 적용 — `reload()`는 `setRefreshKey` 증가만 수행 |
