# STYLE_GUIDE — 디자인 토큰 및 스타일 규칙

`docs/design/mockup.html` 기준. 모든 스타일 구현은 이 문서와 목업을 함께 참고한다.

---

## CSS 변수 (Design Tokens)

`src/styles/_variables.scss`에 아래 변수를 정의하고 `:root`에 등록한다.

```scss
:root {
  // 배경 / 서피스
  --bg: #f4f4f4;
  --surface: #fff;
  --surface2: #f8f8f8;
  --surface3: #efefef;

  // 테두리
  --border: rgba(0, 0, 0, 0.08);
  --border2: rgba(0, 0, 0, 0.14);

  // 텍스트
  --text: #1a1917;
  --text2: #6b6960;
  --text3: #9c9a8e;

  // 브랜드 (S-Food 레드)
  --teal: #c0000a;
  --teal-bg: #fdecea;
  --teal-mid: #e8000d;

  // 긍정 (초록 — 판매중, 재개)
  --pos: #0f6e56;
  --pos-bg: #e1f5ee;
  --pos-mid: #1d9e75;

  // 경고 (주황 — 품절)
  --coral: #993c1d;
  --coral-bg: #faece7;
  --coral-mid: #d85a30;

  // 전용 구분 (보라)
  --purple-bg: #eeedfe;
  --purple: #534ab7;

  // Border Radius
  --r-sm: 6px;
  --r-md: 8px;
  --r-lg: 12px;
  --r-xl: 16px;
}
```

---

## 폰트

```scss
font-family: 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;
```

| 용도 | 크기 | 굵기 | 색상 |
|---|---|---|---|
| 기본 본문 | 15px | 400 | `--text` |
| 카드 타이틀 | 13px | 500 | `--text` |
| 테이블 헤더 (th) | 12px | 600 | #555 |
| 테이블 셀 (td) | 13px | 400 | `--text` |
| 보조 텍스트 | 11px | 400 | `--text3` |
| 뱃지 | 11px | 500 | — |

---

## 레이아웃

- **최대 너비**: `1400px`, 중앙 정렬
- **헤더**: 높이 `52px`, sticky, `z-index: 100`, 흰 배경
- **메인 패딩**: `1rem 1.5rem 2rem`
- **섹션 간격**: `margin-bottom: 0.75rem`

---

## 카드 (`.ucard`)

```scss
background: var(--surface);
border: 0.5px solid var(--border);
border-radius: var(--r-lg);
overflow: hidden;
width: 100%;
```

카드 헤더:
```scss
display: flex;
align-items: center;
gap: 7px;
padding: 10px 14px;
border-bottom: 0.5px solid var(--border);
```

---

## 뱃지 (Badge)

공통 기본:
```scss
display: inline-flex;
align-items: center;
padding: 2px 8px;
border-radius: 10px;
font-size: 11px;
font-weight: 500;
white-space: nowrap;
line-height: 1.6;
```

| 클래스 | 용도 | 스타일 |
|---|---|---|
| `b-res-s` | 판매중 | `background: var(--pos-mid)`, 흰 글씨 |
| `b-out-s` | 품절 | `background: var(--coral-mid)`, 흰 글씨 |
| `b-g` | 범용 | `background: var(--surface2)`, `border: 0.5px solid var(--border)`, `color: var(--text2)` |
| `b-e` | 전용 | `background: var(--purple-bg)`, `color: var(--purple)` |

---

## 버튼

| 종류 | 스타일 | 사용처 |
|---|---|---|
| Primary (`.add-btn`, `.run-btn`) | `background: #c8000b`, 흰 글씨, `border-radius: var(--r-md)` | 상품 추가, 동기화 실행 |
| Secondary (`.upload-btn`) | 테두리 버튼, `color: var(--text2)`, `border: 0.5px solid var(--border2)` | 엑셀 업로드 |
| 저장 (`.btn-save`) | `background: #c8000b`, 높이 34px | 모달 저장 |
| 취소 (`.btn-cancel`) | 테두리, `color: var(--text)` | 모달 취소 |
| 삭제 확인 (`.btn-del-cf`) | `background: var(--coral-mid)` | 삭제 모달 확인 |

---

## 필터 버튼 (`.fbtn`)

기본: 높이 `26px`, `border: 0.5px solid var(--border2)`, `font-size: 11px`

| 클래스 | 활성 상태 | 용도 |
|---|---|---|
| `.fa` | `color: var(--text)`, 진한 테두리 | 전체 |
| `.fg` | `background: var(--surface2)` | 범용 |
| `.fe` | `background: var(--purple-bg)`, `color: var(--purple)` | 전용 |
| `.fo` | `background: var(--coral-bg)`, `color: var(--coral)` | 품절 |
| `.fn` | `background: var(--pos-bg)`, `color: var(--pos)` | 판매중 |

---

## 테이블

```scss
// 헤더
th {
  padding: 7px 11px;
  font-size: 12px;
  font-weight: 600;
  color: #555;
  background: #ececef;
  border-bottom: 1px solid rgba(0,0,0,0.1);
  text-transform: uppercase;
  white-space: nowrap;
}

// 셀
td {
  padding: 7px 11px;
  border-bottom: 0.5px solid var(--border);
  font-size: 13px;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

tr:hover td {
  background: var(--surface2);
}
```

전체 상품 목록 테이블 컬럼 비율:

| 컬럼 | 비율 |
|---|---|
| 고도몰 상품코드 | 11% |
| ERP 품목코드 | 12% |
| 상품명 | 24% |
| 구분 | 6% |
| 판매 상태 | 9% |
| 동기화 일자 | 10% |
| 등록·수정일 | 11% |
| 등록/수정자 | 10% |
| 액션 버튼 | 7% |

---

## 현황 요약 카드

3열 그리드 (`grid-template-columns: repeat(3, 1fr)`, gap `12px`).

| 카드 | 특이사항 |
|---|---|
| 마지막 실행 | 기본 스타일 |
| 현재 품절 | `border-left: 4px solid #c8000b`, 품절 수 빨간 굵은 글씨 (`font-size: 28px`, `font-weight: 700`, `color: var(--coral-mid)`), `/전체수` 는 연한 작은 글씨 |
| 오늘 상태 변경 | `sum-value`에 `color: var(--pos)` |

---

## 모달

```scss
.modal-bg {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.32);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.modal {
  background: var(--surface);
  border: 0.5px solid var(--border);
  border-radius: var(--r-xl);
  padding: 1.25rem;
  width: 100%;
  max-width: 420px;
  max-height: 90vh;
  overflow-y: auto;
}
```

---

## 토스트 알림

- 위치: 우측 하단 고정
- 배경: `#1a1917` (일반) / `var(--coral-mid)` (오류)
- 텍스트: 흰색, `font-size: 12px`
- 자동 사라짐: 정상 2.5초, 오류 6초

---

## 반응형

640px 이하:
- 요약 그리드 1열
- 헤더 중앙 상태 pill 숨김
- 실행 버튼 아이콘만 표시

---

## 목업 참조

디자인 구현 시 `docs/design/mockup.html`을 브라우저에서 직접 열어 확인한다.
CSS 변수명, 클래스명, 레이아웃 모두 목업 기준을 따른다.
