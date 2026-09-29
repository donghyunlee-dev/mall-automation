# DESIGN — 디자인 시스템 / 스타일 기준

신규 화면 개발 시 **현재 구현된 디자인·컬러를 그대로 유지**하기 위한 기준 문서입니다.
모든 값은 실제 코드(`src/styles/_variables.scss`, `_reset.scss`, 각 컴포넌트 `*.module.scss`)에서 추출했습니다.

- 토큰 원본(Single Source of Truth): `src/styles/_variables.scss`
- 신규 컴포넌트는 **하드코딩 금지**, 아래 CSS 변수를 재사용합니다. (예외: 브랜드 레드 버튼의 `#c8000b`만 컴포넌트에 직접 사용 중 — 동일 값 유지)

---

## 1. 컬러

### 1-1. 배경 / 서피스

| 토큰 | 값 | 용도 |
|---|---|---|
| `--bg` | `#f4f4f4` | 페이지 전체 배경 |
| `--surface` | `#fff` | 카드·헤더·모달 등 기본 면 |
| `--surface2` | `#f8f8f8` | hover 배경, 입력 필드 배경 |
| `--surface3` | `#efefef` | 보조 면 |

### 1-2. 테두리

| 토큰 | 값 | 용도 |
|---|---|---|
| `--border` | `rgba(0,0,0,0.08)` | 기본 구분선 (헤더·카드 하단 등) |
| `--border2` | `rgba(0,0,0,0.14)` | 버튼·입력 등 또렷한 테두리 |

> 테두리 두께는 관례적으로 `0.5px`를 사용합니다. (`0.5px solid var(--border)`)

### 1-3. 텍스트

| 토큰 | 값 | 용도 |
|---|---|---|
| `--text` | `#1a1917` | 본문·제목 기본 |
| `--text2` | `#6b6960` | 보조 텍스트, 설명 |
| `--text3` | `#9c9a8e` | 비활성·라벨·단위 |

### 1-4. 브랜드 / 상태 컬러 (S-Food 레드 기반)

각 컬러는 `메인 / -mid(중간) / -bg(연한 배경)` 3단으로 구성됩니다.

| 의미 | 메인 | -mid | -bg | 사용처 |
|---|---|---|---|---|
| **브랜드 / 위험** (레드) | `--teal` `#c0000a` | `--teal-mid` `#e8000d` | `--teal-bg` `#fdecea` | 로고 강조, 주요 액션, 포커스 |
| **긍정** (초록) | `--pos` `#0f6e56` | `--pos-mid` `#1d9e75` | `--pos-bg` `#e1f5ee` | 판매중·재개 상태 |
| **경고** (주황) | `--coral` `#993c1d` | `--coral-mid` `#d85a30` | `--coral-bg` `#faece7` | 품절 상태, 에러 박스 |
| **전용 구분** (보라) | `--purple` `#534ab7` | — | `--purple-bg` `#eeedfe` | 전용몰(onlyMall) 등 구분 태그 |

> 브랜드 명칭이 `--teal`(원래 청록 의미)이지만 **실제 값은 S-Food 레드**입니다. 명칭만 레거시이며 값은 그대로 유지합니다.
> 주요 버튼의 실제 배경색은 `#c8000b`(레드)로 컴포넌트에 직접 지정돼 있습니다 — 신규 주요 버튼도 동일 값 사용.

---

## 2. 타이포그래피

| 항목 | 값 |
|---|---|
| 폰트 패밀리 | `'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif` |
| 기본 크기 | `15px` (html·body 기준) |
| 기본 굵기 | `400` |
| 줄간격 | `1.5` |
| 렌더링 | `-webkit-font-smoothing: antialiased` |

### 폰트 크기 스케일 (실사용 기준)

| 용도 | 크기 | 굵기 |
|---|---|---|
| 로고 | `18px` | `700` |
| 카드 제목 / 본문 | `13–14px` | `500` |
| 버튼 | `12–13px` | `500` |
| 배지 / 칩 | `11px` | `500–600` |
| 라벨(대문자) | `11px` | `600`, `letter-spacing: 0.4px`, `text-transform: uppercase` |

---

## 3. 모양 (Radius)

| 토큰 | 값 | 용도 |
|---|---|---|
| `--r-sm` | `6px` | 작은 입력·작은 버튼 |
| `--r-md` | `8px` | 버튼·배지·기본 |
| `--r-lg` | `12px` | 팝오버·카드 |
| `--r-xl` | `16px` | 모달 |

그림자(팝오버·모달): `0 4px 16px rgba(0,0,0,0.1)`

---

## 4. 컴포넌트 패턴

### 4-1. 헤더 (Header)
- 높이 `52px`, 배경 `--surface`, 하단 `0.5px solid var(--border)`
- `position: sticky; top: 0; z-index: 100`
- 좌측 로고(18px/700, 강조부 `em`은 `--teal-mid`), 우측 액션은 `margin-left: auto`로 정렬, 요소 간 `gap: 8px`

### 4-2. 주요 버튼 (Primary)
```
height: 32px; padding: 0 13px;
background: #c8000b; color: #fff; border: none;
border-radius: var(--r-md); font-size: 13px; font-weight: 500;
hover → opacity: 0.88 / active → transform: scale(0.97)
disabled / running → opacity: 0.65
```

### 4-3. 보조 버튼 (Outline)
```
height: 30–32px; padding: 0 10px;
background: none; border: 0.5px solid var(--border2);
border-radius: var(--r-md); font-size: 12px; color: var(--text);
hover → background: var(--surface2)
disabled → opacity: 0.45
```

### 4-4. 입력 필드 (Input)
```
height: 28px; background: var(--surface2);
border: 0.5px solid var(--border2); border-radius: var(--r-sm);
font-size: 12px; outline: none;
focus → border-color: var(--teal-mid)
```

### 4-5. 카드 / 모달
- 카드: 배경 `--surface`, `0.5px solid var(--border)`, radius `--r-lg`
- 모달: radius `--r-xl`, padding `1.25rem`, max-width `340px`(확인 모달 기준)
- 모달 배경 오버레이: `rgba(0,0,0,0.32)`, `z-index: 200`, 중앙 정렬

### 4-6. 팝오버 (Popover)
- 배경 `--surface`, `0.5px solid var(--border2)`, radius `--r-lg`
- `box-shadow: 0 4px 16px rgba(0,0,0,0.1)`, 트리거 기준 `top: calc(100% + 6px)`

### 4-7. 배지 / 상태 칩 (Badge)
```
display: inline-flex; height: 24px; padding: 0 10px;
border-radius: var(--r-md); font-size: 11px; font-weight: 500–600;
```
상태별 색은 §1-4 매핑을 따릅니다. (예: 판매중 → `--pos-mid` 배경 + 흰 글자, 품절 → `--coral` 계열, 전용 → `--purple` 계열)

### 4-8. 에러 박스
```
background: var(--coral-bg); border: 0.5px solid rgba(153,60,29,0.2);
border-radius: var(--r-md); padding: 10px 12px;
font-size: 12px; color: var(--coral);
```

---

## 5. 반응형
- 브레이크포인트: `max-width: 640px`
- 좁은 화면에서 버튼 라벨 텍스트 숨김(`span { display:none }`), 보조 컨트롤 숨김 등으로 아이콘 위주 축소

---

## 6. 신규 화면 작성 규칙

1. 색·radius는 **반드시 위 CSS 변수 사용** (하드코딩 금지). 주요 버튼 레드만 예외적으로 `#c8000b` 직접 사용.
2. 스타일은 컴포넌트 단위 **CSS Modules(`.module.scss`)**, 전역 변수는 `_variables.scss`에만 정의.
3. 인라인 스타일 금지(동적 값은 CSS 변수로 주입).
4. 폰트는 `inherit` 유지(`font-family: inherit`) — 별도 폰트 지정 금지.
5. 새 상태/구분이 필요하면 기존 4색 팔레트(레드/초록/주황/보라) 안에서 의미를 매핑하고, 부득이할 때만 `_variables.scss`에 토큰을 추가합니다.
6. 레이아웃·간격·컴포넌트 형태의 시각적 기준은 `docs/design/mockup.html`을 함께 참고합니다.
