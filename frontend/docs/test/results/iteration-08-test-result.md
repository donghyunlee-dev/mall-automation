# Iteration 08 Test Result — 네비게이션 재구성

> 실행일: 2026-06-22
> 담당: Sohee Kang

---

## 빌드 검증

```
> npm run build
> tsc -b && vite build

✓ 142 modules transformed.
dist/index.html                    0.50 kB │ gzip:   0.35 kB
dist/assets/favicon-DguG_j6D.svg  1.42 kB │ gzip:   0.76 kB
dist/assets/index-DAidUhAl.css   42.46 kB │ gzip:   8.09 kB
dist/assets/index-B0W4IzYP.js   780.62 kB │ gzip: 257.02 kB
✓ built in 5.45s
```

결과: **통과** — TypeScript 컴파일 오류 없음

---

## 린트 검증

```
> npm run lint
> eslint . --ext ts,tsx --report-unused-disable-directives --max-warnings 0
```

결과: **통과** — 오류 및 경고 0건

---

## 테스트 결과

```
> npm run test
> vitest --run

 RUN  v4.1.2 C:/Users/USER/SFOOD-MALL-AUTOMATION/frontend

 Test Files  12 passed (12)
      Tests  37 passed (37)
   Start at  17:10:53
   Duration  7.10s
```

결과: **통과** — 37/37 전체 통과

---

## 테스트 파일별 상세

| 테스트 파일 | 통과 |
|------------|------|
| `Header.test.tsx` | ✅ 3건 |
| `AvatarDropdown.test.tsx` | ✅ |
| `ProductTable.test.tsx` | ✅ |
| `ProductAddModal.test.tsx` | ✅ |
| `ProductEditModal.test.tsx` | ✅ |
| `useGoods.test.tsx` | ✅ |
| `useUserNotifications.test.tsx` | ✅ |
| `useCommonStatusConfig.test.tsx` | ✅ |
| 기타 4개 파일 | ✅ |

---

## Header.test.tsx 변경 내역

### MemoryRouter 래퍼 추가

Header에 NavLink가 포함되면서 Router context가 필요해졌다.
3개 테스트 케이스의 render 호출을 모두 `<MemoryRouter>`로 감쌌다.

### 삭제된 props 반영

기존 3개 테스트 케이스에서 제거된 props를 모두 삭제했다.

제거 목록: `schedule`, `running`, `lastRefreshedAt`, `onRunNow`, `onApplyInterval`

### 테스트 2번 교체

| 이전 | 이후 |
|------|------|
| `shows status sync button text and hides refresh timestamp` | `renders program name and excludes batch controls` |
| `schedule` prop으로 배치 버튼 존재 확인 | 로고 텍스트 `운영 관리` 표시 확인. 배치 제어 버튼 미존재 확인 |

배치 동기화 버튼이 GoodsPage로 이동했으므로 기존 테스트가 더 이상 유효하지 않아 교체했다.

### 공통 defaultProps 도입

3개 테스트 케이스에서 반복되는 props를 `defaultProps` 객체로 추출하여 중복을 제거했다.

---

## 회귀 없음 확인

Iteration 01~07에서 구현한 기존 테스트 37건 전체 통과. 신규 파일(AuthedLayout, GoodsPage, MembersPage)에 대한 별도 테스트는 Iteration 08 범위 외이다.
