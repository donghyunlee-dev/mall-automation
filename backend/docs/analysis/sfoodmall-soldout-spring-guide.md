# 에쓰푸드몰 상품 품절/정상 변경 Spring Backend 개발 문서

## 목적

Spring 기반 백엔드에서 에쓰푸드몰 고도몰 관리자에 로그인한 뒤 상품 수정 화면의 전체 FormData를 사용해 품절 상태만 변경한다.

이 문서의 목표는 다음이다.

- 관리자 로그인 세션을 생성하고 재사용한다.
- 상품 정보 조회 페이지를 열어 현재 상품 수정 FormData를 가져온다.
- `soldOutFl`과 `modDtUse`만 변경하고 나머지 필드는 그대로 전송한다.
- 저장 후 조회 검증으로 `soldOutFl` 외 주요 상품 정보가 바뀌지 않았는지 확인한다.

## 핵심 결론

품절 상태 저장은 `goods_ps.php`에 전체 상품 수정 폼을 제출하는 방식으로 구현한다.

축약 payload는 사용하지 않는다. 테스트 상품 `1000001371` 기준으로 `mode`, `goodsNo`, `soldOutFl` 중심의 축약 payload를 보냈을 때 저장 실패 또는 상품 정보 손상이 발생했다.

확인된 위험은 다음과 같다.

- 필수값 부족 시 응답은 200이어도 `상품명은 필수 항목 입니다.` 메시지로 저장 실패한다.
- 일부 필수값만 포함하면 저장은 되지만 누락 필드가 빈 값 또는 기본값으로 저장될 수 있다.
- 카테고리 연결, 상세 설명, 이미지, 네이버페이 값 등이 변경될 수 있다.
- 옵션 미사용 상품도 관리자 폼 안에는 기존 옵션 식별 hidden 값이 있을 수 있다.

따라서 안정성 우선 원칙은 다음이다.

```text
상품 수정 페이지에서 현재 렌더링된 전체 FormData 수집
-> soldOutFl 값만 y/n으로 교체
-> modDtUse=y 설정
-> multipart/form-data로 goods_ps.php에 POST
-> 저장 후 상품 조회로 검증
```

## 환경 변수

Spring 프로젝트에서는 운영 설정에 아래 값을 둔다.

```properties
sfood.admin.base-url=https://gdadmin.sfoodmall.co.kr
sfood.admin.login-path=/base/login.php
sfood.admin.login-action=/base/login_ps.php
sfood.admin.goods-form-path=/goods/goods_register.php
sfood.admin.goods-save-path=/goods/goods_ps.php
sfood.admin.id=...
sfood.admin.password=...
```

계정 정보는 로그에 출력하지 않는다. 요청/응답 로그를 남길 때도 쿠키, 비밀번호, 세션 값은 마스킹한다.

## 로그인 기능

### 로그인 흐름

1. 상품 수정 URL 또는 `/base/login.php`에 GET 요청한다.
2. 로그인 페이지가 나오면 `#frmLogin` 폼을 파싱한다.
3. hidden input 값을 유지한다.
4. `managerId`, `managerPw`, `mode=login`, `returnUrl`을 채운다.
5. `/base/login_ps.php`로 POST한다.
6. 응답 HTML에 로그인 성공 메시지가 있는지 확인한다.
7. 쿠키 저장소에 `GD5SESSID` 등 세션 쿠키를 유지한다.

관리자 로그인 성공 응답은 HTTP status만으로 판단하지 않는다. 실패도 200으로 내려올 수 있다.

성공 판단 예:

```text
관리자 접속이 승인되었습니다.
```

실패 판단 예:

```text
쇼핑몰 관리자 정보가 존재하지 않거나 잘못된 정보 입니다.
```

### Spring 구현 방향

JDK `HttpClient` 또는 Apache HttpClient를 사용할 수 있다. 쿠키 재사용이 중요하므로 `CookieStore`를 지원하는 Apache HttpClient 5 사용을 권장한다.

```java
CloseableHttpClient httpClient = HttpClients.custom()
    .setDefaultCookieStore(cookieStore)
    .build();
```

로그인 요청은 `application/x-www-form-urlencoded`로 전송한다.

```java
List<NameValuePair> params = List.of(
    new BasicNameValuePair("mode", "login"),
    new BasicNameValuePair("returnUrl", returnUrl),
    new BasicNameValuePair("managerId", adminId),
    new BasicNameValuePair("managerPw", adminPassword)
);

HttpPost post = new HttpPost(baseUrl + "/base/login_ps.php");
post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
```

## 상품 정보 조회 페이지

관리자 상품 수정 페이지 URL은 다음 형식이다.

```text
https://gdadmin.sfoodmall.co.kr/goods/goods_register.php?popupMode=yes&goodsNo={goodsNo}
```

요청 후 로그인 페이지로 이동하면 로그인 후 다시 같은 URL을 조회한다.

상품 페이지 진입 성공 조건은 다음을 모두 만족해야 한다.

- 최종 URL이 `/goods/goods_register.php`이다.
- 페이지 title 또는 body에 상품 수정 화면이 존재한다.
- `form#frmGoods`가 존재한다.
- `input[name=goodsNo]` 값이 요청한 상품번호와 같다.
- `input[name=mode]` 값이 `modify`이다.
- `input[name=soldOutFl]` radio가 존재한다.

## 전체 FormData 수집 규칙

HTML 폼의 “successful controls” 규칙에 맞춰 값을 수집한다.

포함한다.

- hidden input
- text input
- checked radio
- checked checkbox
- textarea
- select의 selected option
- 파일 input은 파일을 새로 업로드하지 않는 한 제외한다.

제외한다.

- disabled control
- unchecked radio/checkbox
- submit/button/reset
- 비어 있는 file input

주의: 단순히 모든 input을 긁으면 unchecked radio까지 중복 전송될 수 있다. 반드시 브라우저 FormData와 같은 규칙으로 수집한다.

Jsoup 기준 예시:

```java
Elements controls = form.select("input, select, textarea");

for (Element el : controls) {
    if (el.hasAttr("disabled")) continue;

    String tag = el.tagName();
    String type = el.attr("type").toLowerCase(Locale.ROOT);
    String name = el.attr("name");
    if (name.isBlank()) continue;

    if ("input".equals(tag)) {
        if (List.of("submit", "button", "reset", "file").contains(type)) continue;
        if ((type.equals("radio") || type.equals("checkbox")) && !el.hasAttr("checked")) continue;
        formData.add(name, el.attr("value"));
    }

    if ("textarea".equals(tag)) {
        formData.add(name, el.text());
    }

    if ("select".equals(tag)) {
        Elements selected = el.select("option[selected]");
        if (selected.isEmpty()) selected = el.select("option").isEmpty() ? new Elements() : new Elements(el.select("option").first());
        for (Element option : selected) {
            formData.add(name, option.hasAttr("value") ? option.attr("value") : option.text());
        }
    }
}
```

## 품절 상태 변경 방법

품절 여부 필드는 `soldOutFl` radio이다.

```text
soldOutFl=n : 정상 판매
soldOutFl=y : 품절
```

저장 직전에 전체 FormData에서 아래 값만 덮어쓴다.

```text
soldOutFl = y 또는 n
modDtUse = y
```

중복 전송을 피하기 위해 기존 `soldOutFl`, `modDtUse` 항목은 제거한 뒤 새 값을 1개만 넣는다.

```java
MultiValueMap<String, Object> formData = collectFullFormData(document);

formData.remove("soldOutFl");
formData.add("soldOutFl", targetSoldOut ? "y" : "n");

formData.remove("modDtUse");
formData.add("modDtUse", "y");
```

저장 요청은 상품 수정 폼의 action을 사용한다.

```text
POST https://gdadmin.sfoodmall.co.kr/goods/goods_ps.php
Content-Type: multipart/form-data
```

Spring WebClient 예시:

```java
MultipartBodyBuilder builder = new MultipartBodyBuilder();
formData.forEach((name, values) -> {
    for (Object value : values) {
        builder.part(name, String.valueOf(value));
    }
});

String html = webClient.post()
    .uri("/goods/goods_ps.php")
    .contentType(MediaType.MULTIPART_FORM_DATA)
    .body(BodyInserters.fromMultipartData(builder.build()))
    .retrieve()
    .bodyToMono(String.class)
    .block();
```

저장 성공 응답은 status 200만으로 판단하지 않는다. 응답 HTML에 아래 메시지가 있어야 한다.

```text
저장이 완료되었습니다.
```

## 저장 후 검증

저장 후 반드시 상품 조회 API 또는 관리자 페이지 재조회로 검증한다.

검증 항목:

- `goodsNo`가 요청한 상품번호와 동일하다.
- `soldOutFl`만 목표값으로 바뀌었다.
- `optionFl`이 기존값과 같다.
- `optionData` 개수와 `sno`가 바뀌지 않았다.
- 상품명, 가격, 카테고리, 배송비, 브랜드, 설명, 이미지가 의도치 않게 바뀌지 않았다.

검증 실패 시 즉시 알림을 남기고 자동 추가 저장을 중단한다.

## 세션 재사용

매 요청마다 로그인하면 느리다. 로그인 세션은 서비스 단위로 재사용한다.

권장 정책:

- CookieStore를 싱글톤 또는 세션 매니저에 보관한다.
- 저장 전 상품 페이지 접근 시 로그인 페이지가 나오면 재로그인한다.
- 재로그인 후 같은 상품 페이지를 다시 조회한다.
- 세션 쿠키 값은 로그에 남기지 않는다.

## 처리 순서 요약

1. 요청 상품번호를 검증한다.
2. 로그인 세션을 준비한다.
3. 상품 수정 페이지를 GET 한다.
4. `#frmGoods`와 `goodsNo`를 검증한다.
5. 전체 FormData를 수집한다.
6. `soldOutFl`, `modDtUse`만 덮어쓴다.
7. `goods_ps.php`로 multipart POST 한다.
8. 응답 메시지 `저장이 완료되었습니다.`를 확인한다.
9. 상품 조회로 저장 결과를 검증한다.

## 절대 하지 말아야 할 것

- 일부 필드만 추려서 저장하지 않는다.
- `soldOutFl` 단독 payload를 보내지 않는다.
- `optionFl=n`이라고 옵션 관련 hidden 값을 임의로 제거하지 않는다.
- 저장 응답 HTTP 200만 보고 성공 처리하지 않는다.
- 상품번호 검증 없이 관리자 저장을 호출하지 않는다.
- 쿠키나 비밀번호를 로그에 남기지 않는다.

## 테스트에서 확인한 사실

테스트 상품 `1000001371`로 확인했다.

- 전체 FormData 저장은 품절 상태 변경에 성공했다.
- 축약 payload는 저장 실패 또는 다른 필드 변경을 일으켰다.
- 옵션 미사용 상품도 관리자 폼에 `optionN[sno][0]`, `optionN[optionNo][0]` 같은 hidden 값이 존재할 수 있다.
- 안정성을 우선하면 전체 FormData를 유지하는 방식이 맞다.

## 권장 서비스 인터페이스

```java
public interface SfoodMallAdminGoodsService {
    GoodsAdminSnapshot getGoodsForm(String goodsNo);
    SoldOutChangeResult changeSoldOut(String goodsNo, boolean soldOut);
}
```

`changeSoldOut`는 내부에서 로그인, 상품 페이지 조회, 전체 FormData 저장, 저장 후 검증까지 수행한다.

## 오류 처리

대표 오류:

- 로그인 실패: 관리자 계정 정보 불일치 또는 추가 인증 필요
- 상품 폼 없음: 권한 없음, 잘못된 상품번호, 세션 만료
- 저장 실패: 필수값 누락, 서버 validation 실패
- 검증 실패: 저장 후 다른 필드 변경 감지

검증 실패는 일반 실패보다 높은 우선순위로 알림 처리한다.

## 운영 주의

이 기능은 실제 상품 정보를 저장한다. 운영 적용 전에는 테스트 상품으로 충분히 검증하고, 처음에는 변경 전/후 스냅샷을 남긴다.

최소한 다음 값은 저장 전후로 비교한다.

```text
goodsNm
goodsDisplayFl
goodsSellFl
goodsPrice
fixedPrice
costPrice
cateCd
allCateCd
brandCd
deliverySno
optionFl
optionData.sno
detailImageData
listImageData
magnifyImageData
mainImageData
```
