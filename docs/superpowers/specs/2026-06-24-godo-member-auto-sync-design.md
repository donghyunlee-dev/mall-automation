# 고도몰 신규 회원 자동 수집 배치 — 설계 문서

작성일: 2026-06-24  
브랜치: `feature/godo-member-auto-sync`  
승인: 사용자 승인 완료

---

## Overview

고도몰 관리자 페이지에 로그인하여 당일 가입 승인된 신규 회원을 자동으로 수집하고 DB에 저장하는 배치 기능을 구현한다. 기존 엑셀 수동 업로드 방식을 보완하며, 일반회원(`group_sno=1`)은 수집 대상에서 제외한다.

---

## Background

현재 회원 데이터는 고도몰 관리자 페이지에서 엑셀을 내려받아 `UploadMembersUseCase`로 수동 업로드하는 방식이다. 매일 신규 가입 회원이 발생할 때마다 수동 처리가 필요하여 운영 부담이 크다. 동일한 고도몰 관리자 페이지 크롤링 패턴이 상품 품절→정상 전환 배치(`ProductSyncBatchJob`)에 이미 구현되어 있으므로 이를 참고하여 자동화한다.

---

## Requirements

- [ ] 배치 실행 날짜 기준으로 당일 가입 승인된 회원을 고도몰 관리자 페이지에서 자동 수집한다
- [ ] 일반회원(`group_sno=1`)은 수집하지 않는다
- [ ] 목록 페이지(Phase 1)에서 `mem_no`와 등급을 수집하고, 상세 팝업(Phase 2)에서 전체 필드를 수집한다
- [ ] 기존 DB에 있는 회원은 UPDATE, 없는 회원은 INSERT한다 (mem_no 기준 upsert)
- [ ] 개인정보(이름·이메일·휴대폰·전화·주소)는 AES-256-GCM으로 암호화하여 저장한다
- [ ] 검색용 해시(mem_name_hash, email_hash, cell_phone_hash)를 생성하여 저장한다
- [ ] `hasBusiness()=true`인 등급(group_sno 2,3,5,6,9)은 `godo_member_business`도 저장한다
- [ ] 기존 `DynamicSchedulerManager` + `BatchExecutionService` 체계로 스케줄 관리한다
- [ ] 배치 실패 시 Slack 알림을 발송한다
- [ ] `SfoodAdminSessionManager`를 공통 인터페이스(`SfoodAdminHttpClient`)로 추상화하여 goods 동작에 영향 없이 member에서 재사용한다

---

## Acceptance Criteria

**Happy Path**

Given 고도몰에 당일 신규 가입 승인된 사업자회원 3명이 있을 때,  
When 배치가 실행되면,  
Then `godo_member` 3건 INSERT, `godo_member_business` 3건 INSERT, 결과 메시지 "신규 3건, 업데이트 0건"이 반환된다.

**일반회원 제외**

Given 당일 가입 회원 중 일반회원(group_sno=1)이 포함되어 있을 때,  
When 배치가 실행되면,  
Then 일반회원은 저장되지 않고 나머지 등급만 저장된다.

**개별 회원 파싱 실패**

Given Phase 2 상세 팝업 파싱 중 특정 회원에서 예외가 발생할 때,  
When 배치가 실행되면,  
Then 해당 회원은 Slack 알림 후 스킵하고, 나머지 회원은 정상 저장한다.

**세션 만료**

Given 크롤링 도중 세션이 만료될 때,  
When 페이지 조회 시 세션 만료 마커가 감지되면,  
Then 자동 재로그인 후 1회 재시도한다.

---

## Architecture

### 공통 인터페이스 추출 (Option B)

`SfoodAdminSessionManager`를 goods 패키지 외부에서도 사용할 수 있도록 공통 인터페이스를 추출한다. goods 기존 코드의 동작은 변경되지 않는다.

```
common/admin/
  SfoodAdminHttpClient (interface)
    + login() : boolean
    + fetchPage(String url) : String

goods/infrastructure/admin/
  SfoodAdminSessionManager
    implements SfoodAdminHttpClient   ← 추가 (기존 동작 불변)
    + fetchPage(String url)           ← 추가 (httpSender.get 래핑)
```

### 신규 컴포넌트

```
member/
  domain/port/
    SfoodAdminMemberPort.java
      + fetchMemberNos(LocalDate date) : List<Long>
      + fetchMemberDetail(Long memNo) : CrawledMember

  application/
    dto/
      CrawledMember.java              (record)
    usecase/
      FetchNewMembersUseCase.java

  infrastructure/admin/
    SfoodAdminMemberListCollector.java   Phase 1 파싱
    SfoodAdminMemberDetailCollector.java Phase 2 파싱
    SfoodAdminMemberClient.java          포트 구현체

batch/infrastructure/job/
  MemberSyncBatchJob.java
```

### 전체 데이터 흐름

```
MemberSyncBatchJob.run(today)
  │
  ├─ Phase 1: SfoodAdminMemberClient.fetchMemberNos(today)
  │     └─ GET /member/member_list.php?entryDt[]=today&appFl=y&nowPage=N (페이지네이션)
  │     └─ SfoodAdminMemberListCollector: HTML 파싱 → memNo + groupSno 수집
  │     └─ groupSno=1 필터링 제외
  │
  ├─ Phase 2: SfoodAdminMemberClient.fetchMemberDetail(memNo) (순차)
  │     └─ GET /member/member_view.php?memNo={memNo}  (실제 URL 브라우저 확인)
  │     └─ SfoodAdminMemberDetailCollector: 탭별 파싱 → CrawledMember 생성
  │
  └─ FetchNewMembersUseCase.execute(crawledMembers)
        ├─ findExistingMemNos() → INSERT/UPDATE 분리
        ├─ AesGcmEncryptor: 개인정보 암호화
        ├─ HashUtil: 검색 해시 생성
        ├─ MemberRepositoryImpl.batchInsert / batchUpdate
        └─ (hasBusiness=true) batchInsertBusiness / batchUpdateBusiness
```

---

## Component Design

### SfoodAdminHttpClient (interface)

```java
// common/admin/SfoodAdminHttpClient.java
package com.sfood.automation.common.admin;

public interface SfoodAdminHttpClient {
    boolean login();
    String fetchPage(String url);
}
```

### SfoodAdminMemberPort (interface)

```java
// member/domain/port/SfoodAdminMemberPort.java
public interface SfoodAdminMemberPort {
    List<Long> fetchMemberNos(LocalDate date);
    CrawledMember fetchMemberDetail(Long memNo);
}
```

### CrawledMember (record)

```java
// member/application/dto/CrawledMember.java
public record CrawledMember(
    Long memNo,
    String memId,
    int groupSno,
    String memberFl,
    String appFl,
    String appDt,
    String entryDt,
    String entryPath,
    String lastLoginDt,
    String memName,
    String nickName,
    String email,
    String cellPhone,
    String phone,
    String address,
    String sexFl,
    String birthDt,
    String marriFl,
    String marriDate,
    String maillingFl,
    String smsFl,
    String job,
    String interest,
    String recommId,
    double saleAmount,
    double mileage,
    double deposit,
    int loginCount,
    String fax,
    int expirationFl,
    String memo,
    String ex1, String ex2, String ex3,
    // 사업자 정보 (nullable)
    String company,
    String businessNo,
    String ceoName,
    String service,
    String item,
    String comAddress
) {}
```

### MemberSyncBatchJob

```java
// batch/infrastructure/job/MemberSyncBatchJob.java
@Component
public class MemberSyncBatchJob implements BatchJob {

    private final SfoodAdminMemberPort memberPort;
    private final FetchNewMembersUseCase fetchNewMembersUseCase;

    @Override
    public String jobName() { return "member-sync"; }

    @Override
    public String run() {
        LocalDate today = LocalDate.now();
        List<Long> memNos = memberPort.fetchMemberNos(today);  // Phase 1
        List<CrawledMember> members = memNos.stream()
            .map(memberPort::fetchMemberDetail)                 // Phase 2 (순차)
            .toList();
        return fetchNewMembersUseCase.execute(members);         // 저장
    }
}
```

---

## 데이터 필드 매핑

### Phase 1 목록 페이지 수집 필드

| 필드 | DB 컬럼 | 비고 |
|---|---|---|
| 회원번호 | `mem_no` | Phase 2 조회 키 |
| 아이디 | `mem_id` | |
| 등급 | `group_sno` | group_sno=1 제외 판별 |
| 가입일 | `entry_dt` | |
| 승인일 | `app_dt` | |
| 승인여부 | `app_fl` | |

### Phase 2 상세 팝업 수집 필드

| 탭 | 수집 필드 | 암호화 여부 |
|---|---|---|
| 기본정보 | mem_name, email, cell_phone, phone, address, nick_name, sex_fl, birth_dt, marri_fl, marri_date, mailling_fl, sms_fl, entry_path, recomm_id, expiration_fl, memo, job, interest | mem_name·email·cell_phone·phone·address |
| 사업자정보 | company, business_no, ceo_name, service, item, com_address | ceo_name·com_address |
| 추가정보 | ex1(업종), ex2(세금계산서), ex3(담당자) | — |

> 탭별 실제 CSS selector는 브라우저 개발자 도구로 확인 후 구현 시 반영

### 암호화 및 해시

| 대상 | 처리 |
|---|---|
| mem_name | AES-256-GCM 암호화 + SHA-256 해시(mem_name_hash) |
| email | AES-256-GCM 암호화 + SHA-256 해시(email_hash, 소문자) |
| cell_phone | AES-256-GCM 암호화 + SHA-256 해시(cell_phone_hash, 숫자만 추출) |
| phone, address, birth_dt | AES-256-GCM 암호화 |
| ceo_name, com_address | AES-256-GCM 암호화 |

---

## 에러 처리

| 상황 | 처리 방식 |
|---|---|
| 로그인 실패 | `SfoodAdminException` throw → 배치 FAIL + Slack 알림 |
| 세션 만료 감지 | 자동 재로그인 1회 재시도, 재시도 실패 시 `SfoodAdminException` |
| 개별 회원 상세 파싱 실패 | 해당 회원 스킵 + Slack 알림, 다음 회원 계속 처리 |
| 배치 전체 예외 | `BatchExecutionService`가 catch → FAIL 로그 + Slack 알림 |

Phase 2는 **순차 처리** (Rate limit 방어, 세션 안정성 우선)

---

## 배치 스케줄 설정

```sql
-- data.sql 및 data-postgresql.sql에 추가
INSERT INTO batch_schedule_config
  (job_name, enabled, schedule_type, fixed_delay_seconds, initial_delay_seconds, description)
VALUES
  ('member-sync', 1, 'FIXED_DELAY', 86400, 60,
   '고도몰 신규 회원 자동 수집 배치 (당일 가입 승인 회원, 일반회원 제외)');
```

| 항목 | 값 |
|---|---|
| 실행 주기 | 24시간 (86400초) |
| 초기 지연 | 60초 |
| 수동 실행 | `POST /api/admin/batch-schedules/member-sync/run` |
| 중복 방지 | `BatchExecutionService` AtomicBoolean 자동 적용 |

---

## 설정 추가

### application.yml

```yaml
app:
  sfood-admin:
    member-list-path: ${SFOOD_ADMIN_MEMBER_LIST_PATH:/member/member_list.php}
    member-detail-path: ${SFOOD_ADMIN_MEMBER_DETAIL_PATH:/member/member_view.php}
```

### SfoodAdminProperties 필드 추가

```java
private String memberListPath;
private String memberDetailPath;
```

---

## 직접 확인이 필요한 항목

구현 시작 전 브라우저로 고도몰 관리자 페이지에서 확인:

| 항목 | 확인 방법 |
|---|---|
| 목록 테이블 CSS selector | 개발자 도구 → 회원 목록 `<table>` 구조 |
| memNo 위치 | 각 행의 링크 href `?memNo=` 파라미터 또는 `<td>` |
| 페이지네이션 파라미터명 | `nowPage` vs `page` 등 실제 파라미터 |
| 마지막 페이지 판별 방법 | 다음 버튼 비활성화 또는 빈 목록 |
| 상세 팝업 URL | 목록 행 "수정" 또는 이름 링크 href |
| 팝업 탭별 HTML 구조 | 각 탭 selector, 입력 필드 name/id |
| pageNum 최대값 | 한 페이지 최대 노출 건수 |

---

## Done Checklist

- [ ] Requirements 구현 완료
- [ ] Acceptance Criteria 검증 완료
- [ ] `SfoodAdminHttpClient` 인터페이스 추출 및 `SfoodAdminSessionManager` implements 추가
- [ ] goods 기존 테스트 전체 통과 확인
- [ ] Phase 1 목록 파싱 통합 테스트
- [ ] Phase 2 상세 팝업 파싱 통합 테스트
- [ ] `FetchNewMembersUseCase` 단위 테스트 (upsert, 암호화, 해시)
- [ ] `MemberSyncBatchJob` 통합 테스트
- [ ] `batch_schedule_config` 초기 데이터 추가
- [ ] BUILD SUCCESSFUL 확인
