# 🛍️ 고도몰 Open API Spec — AI Agent용

> **Base URL (Real):** `https://openhub.godo.co.kr`
> **Base URL (Sandbox):** `http://sbopenhub.godo.co.kr`
> **통신 방식:** HTTP POST
> **인코딩:** UTF-8
> **응답 형식:** XML

---

## 🔐 공통 인증 파라미터

모든 API 요청에 필수로 포함되어야 합니다.

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `partner_key` | STRING | ✅ | 제휴사 인증키 |
| `key` | STRING | ✅ | 쇼핑몰 인증키 |

---

## ⚠️ Rate Limit 정책

- **방식:** Token Bucket
- **제한:** 1초에 100회 이상 호출 시 `429 Too Many Requests` 응답
- **헤더 확인:** 응답 헤더의 `ratelimit-available-level` 값이 `EXHAUSTED`이면 즉시 호출 중단 필요

---

## 📦 공통 Response 구조

```xml
<data>
  <header>
    <code>000</code>           <!-- RETURN CODE -->
    <msg>성공</msg>
    <total>100</total>         <!-- 검색결과 총 개수 (조회 API만) -->
    <max_page>10</max_page>    <!-- 총 페이지 수 (조회 API만) -->
    <now_page>1</now_page>     <!-- 현재 페이지 (조회 API만) -->
  </header>
  <return>
    <!-- response data -->
  </return>
</data>
```

### 주요 Return 코드

| 코드 | 구분 | 메시지 |
|---|---|---|
| `000` | ✅ 성공 | 성공 |
| `999` | ❌ 실패 | 인증키가 유효하지 않습니다 |
| `898` | ❌ 실패 | 값은 필수 값입니다 |
| `119` | ❌ 실패 | 존재하지 않는 상품번호입니다 |
| `429` | ❌ 실패 | Too Many Requests |

---

---

## 🔍 상품조회 API

### 개요

| 항목 | 내용 |
|---|---|
| **목적** | 쇼핑몰에 등록된 상품 정보 조회 |
| **Endpoint (Real)** | `POST https://openhub.godo.co.kr/godomall5/goods/Goods_Search.php` |
| **Endpoint (Sandbox)** | `POST http://sbopenhub.godo.co.kr/godomall5/goods/Goods_Search.php` |

---

### Request 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `goodsNm` | STRING | | 상품명 |
| `goodsNo` | INTEGER | | 상품코드 |
| `goodsCd` | STRING | | 자체 상품코드 |
| `makerNm` | STRING | | 제조사 |
| `originNm` | STRING | | 원산지 |
| `goodsSearchWord` | STRING | | 검색 키워드 |
| `goodsModelNo` | STRING | | 모델번호 |
| `searchDateType` | STRING | | 기간검색 타입 (`regDt`=등록일, `modDt`=수정일) |
| `startDate` | DATE | 조건부 | 기간검색 시작일 — `searchDateType` 입력 시 필수 (예: `2016-06-03`) |
| `endDate` | DATE | 조건부 | 기간검색 종료일 — `searchDateType` 입력 시 필수 (예: `2016-06-03`) |
| `cateCd` | INTEGER | | 카테고리 코드 |
| `page` | INTEGER | | 페이지 번호 |
| `size` | INTEGER | | 페이지당 상품건수 (최대 100건) |

> ⚠️ `companyNm` (공급사명)은 공급사 인증키에서 사용 불가

---

### Response 필드

#### 📌 상품 기본 정보

| 필드 | 설명 |
|---|---|
| `goodsNo` | 상품번호 |
| `goodsNmFl` | 상품명 타입 (`d`=기본, `e`=확장) |
| `goodsNm` | 기본 상품명 (최대 250자, HTML Tag 미지원) |
| `goodsNmMain` | 메인 상품명 (HTML Tag 지원, 메인 노출 시만 표시) |
| `goodsNmList` | 리스트 상품명 (HTML Tag 지원) |
| `goodsNmDetail` | 상세 상품명 (HTML Tag 지원) |
| `goodsNmPartner` | 제휴 상품명 — N쇼핑, 다음 쇼핑하우 등 외부 노출용 |
| `purchaseGoodsNm` | 매입처 상품명 — 고객 미노출, 운영자 관리용 |
| `goodsCd` | 자체 상품코드 (영문·숫자·언더바, 최대 30자) |
| `cateCd` | 대표 카테고리 코드 |
| `allCateCd` | 전체 연결 카테고리 (구분자: `\|`) |
| `brandCd` | 브랜드 코드 |
| `makerNm` | 제조사 (최대 30자) |
| `originNm` | 원산지 (최대 30자) |
| `goodsModelNo` | 모델번호 (최대 30자, 영문·숫자·언더바) |
| `goodsSearchWord` | 검색 키워드 (최대 250자) |
| `goodsState` | 상품상태 (`n`=새상품, `u`=중고상품, `r`=반품/재고상품) |
| `goodsColor` | 대표 색상 코드 (구분자: `^|^`) |
| `scmNo` | 공급사번호 (`1`=본사) |
| `applyFl` | 공급사 승인여부 (`a`=승인요청, `y`=승인완료, `n`=승인거부, `r`=승인반려) |
| `applyType` | 승인구분 (`r`=상품등록, `m`=상품수정, `d`=상품삭제) |
| `applyMsg` | 승인 반려 사유 |
| `regDt` | 상품 등록일 |
| `modDt` | 상품 수정일 |

#### 📌 판매 / 노출 상태

| 필드 | 설명 |
|---|---|
| `goodsDisplayFl` | PC 쇼핑몰 노출 (`y`/`n`) |
| `goodsDisplayMobileFl` | 모바일 쇼핑몰 노출 (`y`/`n`) |
| `goodsSellFl` | PC 쇼핑몰 판매 (`y`/`n`) |
| `goodsSellMobileFl` | 모바일 쇼핑몰 판매 (`y`/`n`) |
| `goodsOpenDt` | 상품 노출 시작 일시 (예: `2016-02-19 05:30:00`) |
| `salesStartYmd` | 판매 시작일 |
| `salesEndYmd` | 판매 종료일 |

#### 📌 가격 / 재고

| 필드 | 설명 |
|---|---|
| `goodsPrice` | 판매가 |
| `fixedPrice` | 정가 |
| `costPrice` | 매입가 |
| `goodsPriceString` | 가격 대체 문구 (최대 30자) |
| `taxFreeFl` | 과세/면세 (`t`=과세, `f`=면세) |
| `taxPercent` | 과세율 |
| `totalStock` | 상품 재고 |
| `stockFl` | 판매재고 기준 (`n`=무한정판매, `y`=재고량에 따름) |
| `soldOutFl` | 품절상태 (`n`=정상, `y`=품절(수동)) |
| `minOrderCnt` | 최소 구매수량 |
| `maxOrderCnt` | 최대 구매수량 |
| `salesUnit` | 묶음주문 단위 |

#### 📌 옵션 정보 (`optionData[]`)

| 필드 | 설명 |
|---|---|
| `sno` | 옵션 고유번호 |
| `optionNo` | 옵션 순서 |
| `goodsNo` | 상품번호 |
| `optionImage` | 옵션 이미지 |
| `optionValue1` ~ `optionValue5` | 1~5차 옵션명 |
| `optionPrice` | 옵션 추가금액 |
| `optionViewFl` | 옵션 노출 여부 |
| `optionSellFl` | 옵션 판매 여부 |
| `optionCode` | 옵션 자체코드 |
| `stockCnt` | 옵션 재고량 |
| `regDt` | 옵션 등록일 |
| `modDt` | 옵션 수정일 |

#### 📌 입력옵션 정보 (`textOptionData[]`)

| 필드 | 설명 |
|---|---|
| `sno` | 입력옵션 고유번호 |
| `goodsNo` | 상품번호 |
| `optionName` | 옵션 타이틀 |
| `mustFl` | 필수 여부 |
| `addPrice` | 추가금액 |
| `inputLimit` | 입력 제한 글자 수 |

#### 📌 이미지 정보

| 필드 | 설명 |
|---|---|
| `magnifyImageData` | 확대 이미지 |
| `detailImageData` | 상세 이미지 |
| `listImageData` | 썸네일 이미지 (최대 1개) |
| `mainImageData` | 리스트 이미지 (최대 1개) |

#### 📌 SEO / 마케팅

| 필드 | 설명 |
|---|---|
| `seoTagFl` | 개별 SEO 태그 사용 여부 (`y`/`n`) |
| `seoTagTitle` | SEO 태그 타이틀 (한글 10자 권고) |
| `seoTagAuthor` | 메타태그 작성자 |
| `seoTagDescription` | 메타태그 설명 (최대 80자) |
| `seoTagKeyword` | 메타태그 키워드 (쉼표 구분, 최대 10개) |
| `daumFl` | 다음 쇼핑하우 노출 여부 (`y`/`n`) |
| `naverFl` | 네이버쇼핑 노출 여부 (`y`/`n`) |
| `eventDescription` | 이벤트 문구 (최대 100자) |

---

### Request 예시

```
POST https://openhub.godo.co.kr/godomall5/goods/Goods_Search.php

partner_key=YOUR_PARTNER_KEY
key=YOUR_KEY
goodsNo=12345
```

### Response 예시 (성공)

```xml
<data>
  <header>
    <code>000</code>
    <msg>성공</msg>
    <total>1</total>
    <max_page>1</max_page>
    <now_page>1</now_page>
  </header>
  <return>
    <goodsNo>12345</goodsNo>
    <goodsNm>테스트 상품</goodsNm>
    <goodsPrice>10000</goodsPrice>
    <soldOutFl>n</soldOutFl>
    <stockFl>y</stockFl>
    <totalStock>100</totalStock>
    ...
  </return>
</data>
```

---

---

## 🚫 상품품절상태 변경 API

### 개요

| 항목 | 내용 |
|---|---|
| **목적** | 상품의 품절 여부만 단독으로 변경 |
| **Endpoint (Real)** | `POST https://openhub.godo.co.kr/godomall5/goods/Goods_Soldout_Status.php` |
| **Endpoint (Sandbox)** | `POST http://sbopenhub.godo.co.kr/godomall5/goods/Goods_Soldout_Status.php` |

---

### Request 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `goodsNo` | STRING | ✅ | 상품코드 |
| `soldoutFl` | STRING | ✅ | 품절 여부 (`y`=품절, `n`=정상) |
| `data_url` | URL | | 상품 XML URL — **사용 시 `goodsNo` 무시됨** |

> ℹ️ `data_url` 사용 시 XML 내 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `goodsNo` | STRING | ✅ | 상품코드 |
| `soldOutFl` | STRING | ✅ | 품절 여부 (`y`=품절, `n`=정상) |

---

### Response 필드

| 필드 | 설명 |
|---|---|
| `code` | 성공/실패 코드 |
| `msg` | 성공 또는 실패 사유 |
| `goods_data.goodsNo` | 처리된 상품 고유번호 |

---

### Request 예시 — 단건 품절 처리

```
POST https://openhub.godo.co.kr/godomall5/goods/Goods_Soldout_Status.php

partner_key=YOUR_PARTNER_KEY
key=YOUR_KEY
goodsNo=12345
soldoutFl=y
```

### Request 예시 — 품절 해제

```
POST https://openhub.godo.co.kr/godomall5/goods/Goods_Soldout_Status.php

partner_key=YOUR_PARTNER_KEY
key=YOUR_KEY
goodsNo=12345
soldoutFl=n
```

### Response 예시 (성공)

```xml
<data>
  <header>
    <code>000</code>
    <msg>성공</msg>
  </header>
  <return>
    <goods_data>
      <goodsNo>12345</goodsNo>
    </goods_data>
  </return>
</data>
```

### Response 예시 (실패 — 존재하지 않는 상품)

```xml
<data>
  <header>
    <code>119</code>
    <msg>존재하지 않는 상품번호입니다</msg>
  </header>
</data>
```

---

## 🤖 AI Agent 활용 가이드

### 상품 품절 처리 플로우

```
1. 상품조회 API로 goodsNo 및 현재 soldOutFl 확인
2. soldOutFl == 'n' → 품절 처리 가능 상태 확인
3. 상품품절상태 변경 API 호출 (soldoutFl=y)
4. Response code == '000' → 성공, 그 외 → 에러 처리
```

### 에러 핸들링 원칙

- `429` 응답 수신 시 → 호출 즉시 중단, 백오프(back-off) 후 재시도
- `ratelimit-available-level: EXHAUSTED` 헤더 감지 시 → 선제적 호출 중단
- `119` (존재하지 않는 상품) → 상품번호 재확인 후 재시도 금지
- `898` (필수값 누락) → 요청 파라미터 검증 로직 점검
