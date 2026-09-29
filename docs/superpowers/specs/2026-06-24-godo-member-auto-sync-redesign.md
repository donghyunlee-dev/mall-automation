# 고도몰 신규 회원 자동 수집 — 재설계 문서 (실 HTML 기반)

작성일: 2026-06-24  
브랜치: `feature/godo-member-auto-sync`  
근거: Playwright로 `gdadmin.sfoodmall.co.kr` 실접속 후 HTML 구조 실측

---

## 배경

초기 설계는 고도몰 URL/HTML 구조를 추정하여 작성하였다. 실접속 결과 구조가 다수 달라  
파서(`SfoodAdminMemberListCollector`, `SfoodAdminMemberDetailCollector`)와  
테스트 코드를 실 HTML 기반으로 전면 재작성한다.

---

## 실측 결과 요약

### Phase 1 — 목록 페이지

**URL 파라미터 (실측)**
```
GET /member/member_list.php
  ?searchFl=y
  &entryDt[]=2026-06-24   ← 시작일
  &entryDt[]=2026-06-24   ← 종료일
  &appFl=n                ← 미승인 회원만 (사업자 회원은 관리자 수동 승인 전)
  &pageNum=100
  &nowPage=1
```

> `group_sno` 파라미터는 URL로 필터하지 않으며, 배치에서도 등급 필터를 적용하지 않는다.  
> 일반회원은 가입 즉시 자동 승인되므로 `appFl=n` 조회 시 실질적으로 결과에 포함되지 않는다.

**실제 HTML 구조**
```html
<table class="table table-rows">        ← 기존 table.list_mem 오류
  <tbody>
    <tr class="center" data-member-no="38037">   ← memNo = tr[data-member-no]
      <td>
        <input type="checkbox" name="chk[]" value="38037"
               data-memid="takobi101"
               data-memnm="김영환"
               data-appfl="y"
               data-cellphone="010-9857-2707"
               data-email="takobi101@naver.com">
      </td>
      <td class="font-num"><span>5</span></td>    ← 순번 (사용 안 함)
      <td><span>takobi101</span></td>             ← 아이디
      <td><span>김영환</span></td>                 ← 이름 (암호화되므로 사용 안 함)
      <td><span>B2B 거래처</span></td>             ← 등급명 텍스트 (숫자 아님)
      ...
      <td><button class="btnModify">수정</button></td>  ← href 없음, JS 이벤트
    </tr>
  </tbody>
</table>
```

**파싱 전략**
- `table.table-rows tbody tr[data-member-no]` → memNo 수집
- groupSno 필터: 적용하지 않음 (등급 무관 전체 미승인 회원 수집)
- 마지막 페이지 판별: `tbody tr` 없거나 `td[colspan]` 하나만 있을 때

---

### Phase 2 — 상세 페이지

**실측 URL**
```
GET /member/member_modify.php?memNo={memNo}   ← member_view.php 오류
```

**폼 구조**: `form#frm`, action=`member_ps.php`

**실측 필드명 매핑**

| CrawledMember 필드 | 실제 HTML 요소 | 비고 |
|---|---|---|
| `memNo` | `input[name=memNo]` hidden | 파라미터로 전달 |
| `memId` | `input[name=memId]` hidden | |
| `memberFl` | `input[name=memberFl]:checked` radio | "personal"/"business" |
| `groupSno` | `select[name=groupSno]` selected value | 1/2/3/5/6/7/8/9 |
| `appFl` | `input[name=appFl]:checked` radio | "y"/"n" |
| `memName` | `input[name=memNm]` text | **HTML명: memNm** |
| `nickName` | `input[name=nickNm]` text | **HTML명: nickNm** |
| `email` | `input#emailAddress` + `@` + `input#emailDomain` | 두 input 조합 |
| `maillingFl` | `input[name=maillingFl]` checkbox | checked → "y", else "n" |
| `cellPhone` | `input[name=cellPhone]` text | |
| `smsFl` | `input[name=smsFl]` checkbox | checked → "y", else "n" |
| `address` | `input[name=address]` + ` ` + `input[name=addressSub]` | 두 필드 조합 |
| `phone` | `input[name=phone]` text | |
| `company` | `input[name=company]` text | |
| `businessNo` | `input[name=fullBusiNo]` hidden | **HTML명: fullBusiNo** |
| `ceoName` | `input[name=ceo]` text | **HTML명: ceo** |
| `service` | `input[name=service]` text | |
| `item` | `input[name=item]` text | |
| `comAddress` | `input[name=comAddress]` + ` ` + `input[name=comAddressSub]` | 두 필드 조합 |
| `fax` | `input[name=fax]` text | |
| `job` | `select[name=job]` selected value | |
| `sexFl` | `input[name=sexFl]:checked` radio | "m"/"w" |
| `birthDt` | `input[name=birthDt]` text | |
| `marriFl` | `input[name=marriFl]:checked` radio | "y"/"n" |
| `marriDate` | `input[name=marriDate]` text | "0000-00-00" → null |
| `recommId` | `input[name=recommId]` text | |
| `interest` | `input[name="interest[]"]:checked` 다중 checkbox | checked 값들 콤마 조인 |
| `expirationFl` | `input[name=expirationFl]:checked` radio | 1/3/5/999 |
| `memo` | `textarea[name=memo]` | input 아님 |
| `ex1` | `input[name=ex1]:checked` radio | checked 값 |
| `ex2` | `input[name=ex2]:checked` radio | checked 값 |
| `ex3` | `input[name=ex3]` text | |

**읽기 전용 텍스트 필드 (form input 아님)**

| CrawledMember 필드 | HTML 위치 | 예시 값 |
|---|---|---|
| `entryDt` | `<th>회원가입일</th>` 다음 `<td>` | "2026-03-18 09:34:22" |
| `lastLoginDt` | `<th>최종로그인일</th>` 다음 `<td>` | "2026-03-19 09:18:42" |
| `entryPath` | `<th>가입경로</th>` 다음 `<td>` | "pc" |
| `loginCount` | `<th>방문횟수</th>` 다음 `<td>` | "3" |
| `saleAmount` | `<th>구매금액</th>` 다음 `<td>` | "173,635원" → 173635.0 |
| `mileage` | `<th>마일리지</th>` 다음 `<td>` | "7,707점" → 7707.0 |
| `deposit` | 마일리지 행 두 번째 `<td>` | "0원" → 0.0 |

**appDt**: 폼에 없음 → null 허용 (`GodoMemberEntity.appDt` nullable)

---

## 변경 범위

| 파일 | 변경 내용 |
|---|---|
| `application.yml` | `member-detail-path`: `member_view.php` → `member_modify.php` |
| `CrawledMember.java` | 필드명 유지 (엔티티와 동일). 변경 없음 |
| `SfoodAdminMemberListCollector.java` | 전면 재작성 — 실 HTML 구조 반영 |
| `SfoodAdminMemberDetailCollector.java` | 전면 재작성 — 실 HTML 구조 반영 |
| `SfoodAdminMemberClient.java` | `buildListUrl()` 파라미터 수정, 상세 URL 경로 수정 |
| `SfoodAdminMemberListCollectorTest.java` | 실 HTML 픽스처 기반으로 전면 재작성 |
| `SfoodAdminMemberDetailCollectorTest.java` | 실 HTML 픽스처 기반으로 전면 재작성 |
| `src/test/resources/fixtures/` | 실 HTML 픽스처 파일 추가 |
| `FetchNewMembersUseCase.java` | 변경 없음 (CrawledMember 필드명 유지) |

---

## 테스트 픽스처 전략

- `src/test/resources/fixtures/member_list_real.html` — 실 목록 테이블 HTML (5행, groupSno=9)
- `src/test/resources/fixtures/member_modify_real.html` — 실 상세 폼 HTML (memNo=38037)
- 테스트는 이 파일을 `ClassPathResource`로 읽어 사용
- 픽스처 내 실제 개인정보(이름, 전화, 이메일)는 테스트용 값으로 마스킹 처리

---

## 파싱 규칙 상세

### 읽기 전용 텍스트 파싱
```java
// th 텍스트로 다음 td 찾기
private String tdAfterTh(Document doc, String thText) {
    return doc.select("th:containsOwn(\"" + thText + "\") ~ td").first()...
}
```

### 숫자 정제
```java
// "173,635원" → 173635.0
private Double parseAmount(String text) {
    if (text == null || text.isBlank()) return null;
    String digits = text.replaceAll("[^0-9.]", "");
    return digits.isBlank() ? null : Double.parseDouble(digits);
}
```

### checkbox → "y"/"n"
```java
private String checkboxVal(Document doc, String name) {
    Element el = doc.selectFirst("input[name=" + name + "]");
    return el != null && el.hasAttr("checked") ? "y" : "n";
}
```

### radio checked 값
```java
private String radioVal(Document doc, String name) {
    Element el = doc.selectFirst("input[name=" + name + "]:checked");
    // Jsoup: checked attribute 존재 여부로 판별
    return el != null ? el.val() : null;
}
```

### email 조합
```java
String addr = val(doc, "emailAddress");  // id=emailAddress
String domain = val(doc, "#emailDomain");
email = (addr != null && domain != null) ? addr + "@" + domain : null;
```

---

## 등급(groupSno) 필터 정책

등급 필터를 적용하지 않는다. `appFl=n`으로 조회되는 회원이 곧 수집 대상이다.

**배경:** 일반회원(groupSno=1)은 가입 즉시 자동 승인되므로 미승인 상태로 조회되지 않는다.  
사업자 회원은 관리자가 수동 승인하므로 가입 후 일정 시간 동안 미승인 상태로 존재한다.

```java
// MemberSyncBatchJob — 등급 필터 없음
List<CrawledMember> members = memNos.stream()
    .map(memberPort::fetchMemberDetail)
    .toList();
```
