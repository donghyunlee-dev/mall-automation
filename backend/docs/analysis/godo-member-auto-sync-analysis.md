# 고도몰 신규 회원 자동 수집 배치 — 기술 분석 보고서

작성일: 2026-06-24  
브랜치: `feature/godo-member-auto-sync`

---

## 1. 개요

고도몰 관리자 페이지에서 특정 날짜에 신규 가입한 회원을 자동으로 수집하여 DB에 저장하는 배치 기능을 구현한다.

- 기존 엑셀 수동 업로드(`UploadMembersUseCase`) 방식을 보완
- 배치 실행 날짜 기준으로 당일 가입한 미승인(appFl=n) 신규 회원 자동 수집
- 등급 구분 없이 전체 미승인 회원 수집 대상 (사업자 회원은 관리자 승인 전 미승인 상태)

---

## 2. 기존 크롤링 패턴 요약

현재 상품 품절→정상 전환 배치에서 구현된 크롤링 패턴이 있으며, 이를 그대로 재사용한다.

### 2-1. 4단계 흐름

| 단계 | 클래스 | 설명 |
|---|---|---|
| 1. 로그인 | `SfoodAdminSessionManager` | Form POST `/base/login_ps.php`, 세션 쿠키 자동 유지 |
| 2. 페이지 GET | `SfoodAdminSessionManager` | `HttpClient` 재사용, 세션 만료 시 자동 재로그인 |
| 3. HTML 파싱 | `SfoodAdminFormCollector` | Jsoup CSS selector 기반 |
| 4. DB 저장 | `GodoMemberJpaRepository` | `mem_no` 기준 upsert |

### 2-2. 로그인 메커니즘

```
POST /base/login_ps.php
Body: mode=login&returnUrl=%2F&managerId={id}&managerPw={pw}
성공 판별: 응답 body에 "관리자 접속이 승인되었습니다." 포함 여부
세션 유지: java.net.http.HttpClient + CookieManager(ACCEPT_ALL) 내부 싱글턴
```

---

## 3. 데이터 수집 전략: 2-Phase 크롤링

회원 목록 페이지에는 기본 정보만 존재하며, 회원 클릭 시 나오는 팝업(상세 페이지)에 이름·이메일·휴대폰·주소·사업자정보 등 민감 필드 전체가 존재한다.

### Phase 1 — 목록 페이지: memNo 및 기본 식별자 수집

**URL 패턴:**
```
GET /member/member_list.php
  ?searchFl=y
  &appFl=n                         ← 미승인 회원만 (사업자 회원은 관리자 수동 승인 전)
  &entryDt[]=2026-06-24            ← 가입일 from (배치 실행일)
  &entryDt[]=2026-06-24            ← 가입일 to  (배치 실행일)
  &pageNum=100                     ← 페이지당 최대 건수 (확인 필요)
  &nowPage=1                       ← 페이지 번호 (페이지네이션)
```

**목록 페이지에서 수집 가능한 필드 (추정, 직접 확인 필요):**

| 필드 | DB 컬럼 | 비고 |
|---|---|---|
| 회원번호 (memNo) | `mem_no` | 상세 조회 키 |
| 아이디 | `mem_id` | |
| 등급 | `group_sno` | 상세 페이지에서 수집 |
| 가입일 | `entry_dt` | |
| 승인일 | `app_dt` | |
| 승인여부 | `app_fl` | |

**페이지네이션:** 전체 건수 또는 "다음 페이지" 존재 여부로 마지막 페이지 판별 (직접 확인 필요)

### Phase 2 — 상세 팝업: 전체 회원 정보 수집

**URL 패턴 (추정):**
```
GET /member/member_view.php?memNo={memNo}
또는
GET /member/member_modify.php?memNo={memNo}
```
→ 실제 URL은 목록 페이지에서 "수정" 링크 href를 파싱하여 확인 필요

**팝업 탭 구성 (추정):**

| 탭 | 수집 대상 필드 |
|---|---|
| 기본정보 | 이름, 이메일, 휴대폰, 전화번호, 주소, 성별, 생년월일, 결혼여부 등 |
| 사업자정보 | 상호, 사업자번호, 대표자명, 업태, 종목, 사업장 주소 |
| 추가정보 | ex1(업종), ex2(세금계산서), ex3(담당자) 등 |

> **중요:** 팝업 HTML의 실제 CSS selector, 필드 위치, 탭 구조는 브라우저로 직접 확인 필요

---

## 4. 회원 도메인 현황

### 4-1. 저장 대상 테이블

| 테이블 | 엔티티 | 조건 |
|---|---|---|
| `godo_member` | `GodoMemberEntity` | 전체 수집 대상 |
| `godo_member_business` | `GodoMemberBusinessEntity` | `MemberGroup.hasBusiness()=true` 인 경우만 (`group_sno` 2,3,5,6,9) |

### 4-2. group_sno 수집 대상

| group_sno | 등급명 | hasBusiness | 수집 여부 |
|---|---|---|---|
| 1 | 일반회원 | false | 수집 (자동승인이므로 미승인 상태로 조회되지 않음) |
| 2 | 사업자회원_화이트 | true | 수집 |
| 3 | 사업자회원_로제 | true | 수집 |
| 5 | 사업자회원_레드 | true | 수집 |
| 6 | 사업자회원_VIP | true | 수집 |
| 7 | 관리자 등급 | false | 수집 |
| 8 | 기능장회원 | false | 수집 |
| 9 | B2B 거래처 | true | 수집 |

### 4-3. 개인정보 암호화 필드

이하 필드는 `AesGcmEncryptor`로 AES-256-GCM 암호화 후 저장한다.

| 필드 | 검색 해시 컬럼 |
|---|---|
| `mem_name` | `mem_name_hash` (SHA-256) |
| `email` | `email_hash` (SHA-256, 소문자) |
| `cell_phone` | `cell_phone_hash` (SHA-256, 하이픈 제거 후) |
| `phone` | — |
| `address` | — |
| `birth_dt` | — |
| `ceo_name` (business) | — |
| `com_address` (business) | — |

### 4-4. 기존 저장 방식 (upsert)

`UploadMembersUseCase` 기반 현재 upsert 패턴:

```
1. findExistingMemNos(targetMemNos) — 기존 회원 여부 확인
2. INSERT 대상 / UPDATE 대상 분리
3. JdbcTemplate.batchUpdate() — IN절 500건 청크 분할
```

신규 배치도 동일 패턴 적용 (엑셀 파싱 단계만 크롤링으로 대체).

---

## 4-2. 저장 로직 상세 (기존 패턴 직접 재사용)

`MemberRepositoryImpl`의 JdbcTemplate batchInsert/batchUpdate SQL 컬럼 목록:

**INSERT INTO godo_member** — 44컬럼 (mem_no, mem_id, group_sno, member_fl, app_fl, app_dt, mem_name, mem_name_hash, nick_name, sex_fl, birth_dt, marri_fl, marri_date, job, interest, email, email_hash, cell_phone, cell_phone_hash, phone, fax, mailling_fl, sms_fl, address, entry_dt, entry_path, last_login_dt, login_count, sale_amount, mileage, deposit, recomm_id, ex1~ex6, expiration_fl, memo, deleted_fl, synced_at, created_at, updated_at)

**암호화 대상 필드:**
- `godo_member`: mem_name, birth_dt, email, cell_phone, phone, address
- `godo_member_business`: ceo_name, com_address

**검색 해시 생성 대상:**
- `mem_name_hash` = SHA-256(mem_name)
- `email_hash` = SHA-256(email 소문자)
- `cell_phone_hash` = SHA-256(cell_phone에서 숫자만 추출)

---

## 5. 재사용 가능한 컴포넌트

| 컴포넌트 | 파일 경로 | 재사용 내용 |
|---|---|---|
| `SfoodAdminSessionManager` | `goods/infrastructure/admin/` | 로그인, 세션 유지, GET/POST |
| `SfoodAdminProperties` | `common/config/` | baseUrl, managerId, managerPw, loginPath |
| `BatchExecutionService` | `batch/infrastructure/job/` | 중복 실행 방지, 로그 저장, Slack 알림 |
| `DynamicSchedulerManager` | `batch/infrastructure/scheduler/` | DB 기반 스케줄 등록 |
| `SlackNotifier` | `common/slack/` | 오류 알림 |
| `AesGcmEncryptor` | `common/security/` | 개인정보 암호화 |
| `HashUtil` | `common/security/` | SHA-256 검색 해시 |
| `GodoMemberEntity` | `member/infrastructure/persistence/` | 저장 엔티티 |
| `GodoMemberBusinessEntity` | `member/infrastructure/persistence/` | 사업자 저장 엔티티 |
| `MemberGroup` (enum) | `member/domain/enums/` | group_sno 매핑, hasBusiness() |

---

## 6. 신규 구현 컴포넌트

```
member/
  application/
    dto/
      CrawledMember.java                    ← 크롤링 수집 결과 DTO
    usecase/
      FetchNewMembersUseCase.java            ← 수집→저장 유스케이스
  domain/
    port/
      SfoodAdminMemberPort.java             ← 아웃바운드 포트
  infrastructure/
    admin/
      SfoodAdminMemberListCollector.java    ← 목록 페이지 파싱 (Phase 1)
      SfoodAdminMemberDetailCollector.java  ← 팝업 상세 파싱 (Phase 2)
      SfoodAdminMemberClient.java           ← 포트 구현체 (어댑터)

batch/
  infrastructure/
    job/
      MemberSyncBatchJob.java               ← 배치 잡 구현체
```

### `SfoodAdminProperties` 추가 프로퍼티

```yaml
app:
  sfood-admin:
    member-list-path: /member/member_list.php   # 신규
    member-detail-path: /member/member_view.php  # 신규 (실제 URL 확인 후 조정)
```

### `batch_schedule_config` 초기 데이터

```sql
INSERT INTO batch_schedule_config
  (job_name, enabled, schedule_type, fixed_delay_seconds, initial_delay_seconds, description)
VALUES
  ('member-sync', 1, 'FIXED_DELAY', 86400, 60,
   '고도몰 신규 회원 자동 수집 배치 (당일 가입 미승인 회원)');
```

---

## 7. 배치 실행 흐름

```
MemberSyncBatchJob.run(today)
  ↓
Phase 1: 목록 페이지 순회 (페이지네이션)
  └─ SfoodAdminMemberClient.fetchMemberNos(today)
     ├─ GET /member/member_list.php?entryDt[]=today&nowPage=1 → memNo 목록 수집
     ├─ GET /member/member_list.php?entryDt[]=today&nowPage=2 → (마지막 페이지까지)
     └─ 미승인(appFl=n) 회원 전체 memNo 수집 (등급 필터 없음)
  ↓
Phase 2: 상세 팝업 조회 (memNo별)
  └─ SfoodAdminMemberClient.fetchMemberDetail(memNo)
     └─ GET /member/member_view.php?memNo={memNo} → 전체 필드 파싱
  ↓
저장: FetchNewMembersUseCase
  ├─ findExistingMemNos() → INSERT/UPDATE 분리
  ├─ AesGcmEncryptor: 개인정보 암호화
  ├─ HashUtil: 검색 해시 생성
  ├─ batchInsert/batchUpdate: godo_member
  └─ batchInsert/batchUpdate: godo_member_business (hasBusiness=true 대상)
  ↓
결과 반환: "신규 N건 저장, 업데이트 M건"
```

---

## 8. 에러 처리 전략 (기존 패턴 적용)

| 상황 | 처리 |
|---|---|
| 로그인 실패 | `SfoodAdminException` throw → 배치 FAIL, Slack 알림 |
| 세션 만료 (목록/상세 조회 중) | 자동 재로그인 1회 재시도 |
| 개별 회원 상세 파싱 실패 | 해당 회원 스킵, Slack 알림, 다음 회원 계속 처리 |
| 전체 배치 예외 | `BatchExecutionService`가 catch → FAIL 로그, Slack 알림 |

---

## 9. 직접 확인이 필요한 항목 (브라우저 검증)

| 항목 | 확인 방법 |
|---|---|
| 목록 페이지 테이블 CSS selector | 브라우저 개발자 도구 → `table` / `tr` 구조 |
| 목록에서 memNo 위치 | 각 행의 `<td>` 또는 링크 href `?memNo=` 파라미터 |
| 페이지네이션 파라미터 | `nowPage` vs `page` 등 실제 파라미터명 |
| 마지막 페이지 판별 | 다음 버튼 비활성화 여부 또는 빈 목록 여부 |
| 상세 팝업 URL | 목록에서 "수정" 또는 이름 링크 href 확인 |
| 팝업 탭별 HTML 구조 | 각 탭 CSS selector, 필드명 위치 |
| 사업자 정보 탭 존재 여부 | 등급별로 탭 구성이 다를 수 있음 |
| pageNum 최대값 | 한 페이지 최대 노출 건수 파라미터 |

---

## 10. SfoodAdminSessionManager 공유 전략 (핵심 설계 결정)

현재 `SfoodAdminSessionManager`는 `goods.infrastructure.admin` 패키지에 위치하며 `@Component`로 등록되어 있다. member 도메인에서 직접 DI는 기술적으로 가능하지만 **도메인 간 의존 역전**이 발생한다.

**세 가지 옵션:**

| 옵션 | 방식 | 장점 | 단점 |
|---|---|---|---|
| A | goods 패키지 직접 주입 | 코드 변경 최소 | 도메인 의존성 역전 |
| B | 공통 HTTP 클라이언트 인터페이스 추출 → `common` 패키지 | 깔끔한 추상화, 기존 goods 코드 최소 변경 | 추가 인터페이스 작업 |
| C | `SfoodAdminSessionManager` 전체를 `common`으로 이동 | 단순 | goods 코드 패키지 변경 |

**권장: Option B** — `common.admin` 패키지에 `SfoodAdminHttpClient` 인터페이스 추출. goods/member 양쪽에서 주입. `SfoodAdminProperties`도 `common`으로 이동하여 회원 관련 경로 추가.

---

## 11. 미결 설계 결정 사항

1. **Phase 2 순차 vs 병렬**: 상세 팝업 조회 순차 처리(안전) vs 병렬(속도), Rate limit 고려
2. **당일 외 재처리**: 수동 실행 시 날짜 파라미터 지정 가능 여부
3. **목록 pageNum 최대값**: `pageNum=100` 이상 가능 여부 (페이지 조회 횟수 최소화)
4. **팝업 URL 확인**: 실제 URL은 브라우저 개발자도구로 목록 행의 링크 href 확인 필요
