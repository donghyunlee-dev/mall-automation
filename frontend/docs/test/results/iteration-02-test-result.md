# Iteration 02 Test Result — 로그인 페이지

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
✓ 97 modules transformed.
dist/index.html                0.49 kB
dist/assets/index-DAaAVKGD.css 6.37 kB
dist/assets/index-CHyqPg0u.js 279.34 kB
✓ built in 1.56s
```

---

### Lint Validation

```bash
npm run lint
```

**결과**: ✅ 통과 (오류 없음, 경고 없음)

---

## 수동 UI 확인

- 목업 `docs/design/mockup-login.html` 기준 확인 (브라우저 오픈 비교)
- Step 1 → Step 2 전환 흐름, 스텝 인디케이터 변화 확인
- 타이머 카운트다운, 재발송 쿨다운 동작 확인
- 에러 박스 표시 조건 확인

---

## 실패 케이스 및 수정 내용

| 오류 | 원인 | 수정 |
|---|---|---|
| `react-hooks/set-state-in-effect` lint 오류 | useEffect 내에서 `setOtpInput('')` 동기 호출 | effect에서 제거, 핸들러(`handleResend`, `handleGoBack`)에서 명시적으로 처리 |
