# 🧪 Testing Strategy

## 🎯 목적

이 문서는 React Frontend 프로젝트에서 테스트 전략을 정의한다.  
목표는 다음과 같다.

- UI 동작과 비즈니스 로직의 정확성을 보장한다.
- 변경 시 회귀(regression)를 방지한다.
- Claude Code가 구현 시 검증 기준을 일관되게 적용하도록 한다.

---

## 📌 테스트 원칙

- 빌드와 린트 통과를 기본 검증으로 삼는다.
- 기능 구현 후 반드시 `npm run build`와 `npm run lint`를 실행한다.
- 테스트는 독립적으로 실행 가능해야 한다.
- 테스트는 빠르고 반복 가능해야 한다.

---

## 🧱 검증 계층 구조

### Build Validation (필수)

- 대상: 전체 프로젝트
- 명령: `npm run build`
- 목적:
  - TypeScript 타입 오류 감지
  - import 오류 감지
  - 빌드 실패 감지

---

### Lint Validation (필수)

- 대상: 전체 프로젝트
- 명령: `npm run lint`
- 목적:
  - 코드 스타일 규칙 준수 확인
  - 사용하지 않는 import/변수 감지
  - `any` 타입 사용 감지

---

### Manual UI Verification (구현 후 확인)

- 대상: 변경된 컴포넌트 / 페이지
- 방법: 브라우저 또는 목업(`docs/design/mockup.html`, `docs/design/mockup-login.html`) 대조 확인
- 목적:
  - UI가 목업과 일치하는지 확인
  - 사용자 인터랙션 동작 확인
  - API 연동 시 요청/응답 확인

---

### Component / Hook Test (선택 — 복잡한 로직 대상)

- 대상: 비즈니스 로직이 포함된 커스텀 훅, 유틸 함수
- 도구: Vitest + React Testing Library (프로젝트에 설정된 경우)
- 목적:
  - 훅의 상태 전이 검증
  - 유틸 함수의 계산 결과 검증

---

## 🧪 검증 작성 규칙

### Given / When / Then 구조

```text
Given: 초기 상태 또는 Props
When: 특정 인터랙션 또는 API 호출
Then: 기대 UI 상태 또는 반환값
```

---

### 기본 규칙

- 한 케이스는 하나의 행위를 검증한다.
- 테스트 데이터는 명확하게 드러나야 한다.
- 외부 API는 mock으로 대체한다.

---

## 🔧 검증 도구

- TypeScript (strict mode) — 타입 안전성
- ESLint — 린트 규칙
- Vite build — 빌드 검증
- Vitest (설정된 경우) — 단위 테스트
- React Testing Library (설정된 경우) — 컴포넌트 테스트

---

## 🚫 금지 사항

- 빌드·린트 확인 없이 구현 완료로 선언
- TypeScript 오류를 `@ts-ignore`로 무시
- `any` 타입으로 타입 오류 우회
- 브라우저 직접 확인 없이 UI 구현 완료 선언

---

## ✅ 검증 기준

다음 조건을 모두 만족해야 검증 완료로 본다.

- `npm run build` 오류 없음
- `npm run lint` 오류 없음
- TypeScript 오류 없음
- 변경된 UI가 목업과 일치함 (해당하는 경우)

---

## 🧾 실행 기준

기본 검증 명령:

```bash
npm run build
npm run lint
```

---

## 📄 결과 기록

각 iteration마다 다음을 기록한다.

- 실행 명령
- 빌드·린트 결과
- 실패 케이스 및 수정 내용
- 수동 UI 확인 결과

경로:

```text
docs/test/results/iteration-XX-test-result.md
```

---

## 🧭 Claude Code 적용 규칙

Claude Code는 반드시 다음 순서를 따른다.

```text
구현
→ npm run build 실행
→ npm run lint 실행
→ 오류 수정 후 재검증
→ 결과 기록
```

빌드·린트 통과 없이 구현을 완료로 선언하면 안 된다.
