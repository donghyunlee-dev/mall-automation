# Iteration 37 Plan — 회원 조회 API (목록·요약·상세)

> 작성일: 2026-06-11
> 의존: Iteration 35 (Entity), Iteration 36 (업로드 데이터 존재)
> 이후: Iteration 38 (파트너 수정 API)
> 참조: `backend/docs/plan/member-management-ui-plan.md` 7-4·7-5, `backend/docs/product/api-spec-member.md`

---

## 목표

목록 페이징·검색·필터, 요약 카드 카운트, 상세 조회(복호화 평문) API를 구현한다.
이 Iteration 완료 후 프론트엔드 Iteration 09(회원 목록 페이지)를 착수할 수 있다.

---

## 범위

### 신규 파일

```
backend/src/main/java/com/sfood/automation/member/
├── application/
│   ├── dto/
│   │   ├── MemberSearchCondition.java
│   │   ├── MemberListItemResponse.java
│   │   ├── MemberPageResponse.java
│   │   ├── MemberSummaryResponse.java
│   │   ├── MemberDetailResponse.java
│   │   └── MemberBusinessDetailResponse.java
│   ├── MemberMasker.java
│   └── usecase/
│       ├── GetMemberListUseCase.java
│       ├── GetMemberSummaryUseCase.java
│       └── GetMemberDetailUseCase.java
└── presentation/
    └── MemberQueryController.java

backend/src/test/java/com/sfood/automation/member/
├── application/
│   └── MemberMaskerTest.java
└── presentation/
    └── MemberQueryControllerTest.java
```

### 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `member/domain/port/MemberRepository.java` | 목록·요약·상세 조회 메서드 추가 |
| `member/infrastructure/persistence/MemberRepositoryImpl.java` | QueryDSL 기반 구현 추가 |

---

## 구현 명세

### MemberSearchCondition.java

```java
public record MemberSearchCondition(
    SearchType searchType,   // MEM_ID | NAME | EMAIL | CELL_PHONE
    String keyword,
    GroupFilter group,       // ALL | GENERAL | BUSINESS | B2B
    String appFl,            // null | "y" | "n"
    LocalDate entryDtFrom,
    LocalDate entryDtTo,
    String sortKey,          // mem_no | mem_id | group_sno | app_fl | entry_dt | last_login_dt | sale_amount
    String sortDir,          // asc | desc
    int page,
    int size
) {
    public enum SearchType { MEM_ID, NAME, EMAIL, CELL_PHONE }
    public enum GroupFilter { ALL, GENERAL, BUSINESS, B2B }

    // 기본값
    public static MemberSearchCondition defaults() {
        return new MemberSearchCondition(SearchType.MEM_ID, null, GroupFilter.ALL,
            null, null, null, "mem_no", "desc", 0, 20);
    }
}
```

**검색 타입별 DB 쿼리 전략:**

| 검색 타입 | DB 조건 | 비고 |
|----------|--------|------|
| MEM_ID | `mem_id LIKE '%keyword%'` | 부분 검색 |
| NAME | `mem_name_hash = HashUtil.hash(keyword)` | 정확 일치 |
| EMAIL | `email_hash = HashUtil.hash(keyword)` | 정확 일치 |
| CELL_PHONE | `cell_phone_hash = HashUtil.hash(keyword.replaceAll("[^0-9]", ""))` | 하이픈 제거 후 해시 |

### MemberListItemResponse.java

```java
public record MemberListItemResponse(
    long memNo,
    String memId,
    String memName,        // 마스킹: "홍*"
    int groupSno,
    String groupName,      // MemberGroup.label
    String cellPhone,      // 마스킹: "010-****-5678"
    String email,          // 마스킹: "ho***@naver.com"
    String company,        // 상호 (null 허용)
    String partnerCode,    // ERP 거래처 코드 (null 허용)
    String entryDt,
    String appFl
) {}
```

### MemberPageResponse.java

```java
public record MemberPageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
```

### MemberSummaryResponse.java

```java
public record MemberSummaryResponse(
    long totalCount,
    long businessCount,       // group_sno IN (2,3,5,6,9)
    long b2bCount,            // group_sno = 9
    long pendingApprovalCount, // app_fl = 'n'
    String lastSyncedAt       // 가장 최근 synced_at (null 허용)
) {}
```

### MemberDetailResponse.java

```java
public record MemberDetailResponse(
    long memNo,
    String memId,
    String memName,        // 복호화 평문
    int groupSno,
    String groupName,
    String appFl,
    String appDt,
    String email,          // 복호화 평문
    String cellPhone,      // 복호화 평문
    String phone,          // 복호화 평문
    String address,        // 복호화 평문
    String entryDt,
    String lastLoginDt,
    String entryPath,
    String maillingFl,
    String smsFl,
    String nickName,
    String sexFl,
    String birthDt,        // 복호화 평문
    String marriFl,
    String marriDate,
    String job,
    String interest,
    long loginCount,
    double saleAmount,
    double mileage,
    double deposit,
    String recommId,
    String ex1, String ex2, String ex3, String ex4, String ex5, String ex6,
    String memo,
    MemberBusinessDetailResponse business,  // null if not business grade
    MemberPartnerDetailResponse partner     // null if not registered
) {}
```

### MemberBusinessDetailResponse.java

```java
public record MemberBusinessDetailResponse(
    String company,
    String businessNo,     // 평문
    String ceoName,        // 복호화 평문
    String service,
    String item,
    String comAddress,     // 복호화 평문
    String bizLicenseFile
) {}
```

---

### MemberMasker.java

마스킹 규칙 (`member-management-ui-plan.md` 5-3):

```java
@Component
public class MemberMasker {
    // 이름: 첫 글자 + * (2자: "홍*", 3자 이상: "홍*동" 형식)
    public String maskName(String name) { ... }

    // 이메일: 로컬 부분 앞 2자 + "***@" + 도메인
    // "hong@naver.com" → "ho***@naver.com"
    // 1자 이하: "*@도메인"
    public String maskEmail(String email) { ... }

    // 휴대폰: 가운데 4자리 마스킹
    // "010-1234-5678" → "010-****-5678"
    // 하이픈 없는 형식도 처리
    public String maskCellPhone(String cellPhone) { ... }

    // 주소: 시/도 + 구 + "…" (앞 두 토큰만)
    public String maskAddress(String address) { ... }
}
```

---

### GetMemberListUseCase.java

1. `MemberSearchCondition` 수신
2. `MemberRepositoryImpl.findAll(condition)` — QueryDSL 동적 쿼리
3. 결과 각 행에 `MemberMasker` 적용 (복호화 → 마스킹 순)
4. `MemberPageResponse<MemberListItemResponse>` 반환

**정렬 허용 컬럼 화이트리스트 검증:**
허용 목록 외 `sortKey` 전달 시 `mem_no DESC`로 대체.

### GetMemberSummaryUseCase.java

COUNT 쿼리 4건 + MAX(`synced_at`) 를 한 번에 조회하거나 개별 쿼리 4건으로 처리.
응답: `MemberSummaryResponse`

### GetMemberDetailUseCase.java

1. `memNo`로 `godo_member` 조회 — 없으면 `MemberNotFoundException` (404)
2. `godo_member_business` LEFT JOIN 조회
3. `member_partner` LEFT JOIN 조회
4. 복호화 후 `MemberDetailResponse` 구성 (마스킹 없음)
5. `business` 섹션: `MemberGroup.of(groupSno).hasBusiness()` 가 true이고 레코드 존재 시 포함
6. `partner` 섹션: `member_partner` 레코드 존재 시 포함

---

### MemberRepositoryImpl.java (QueryDSL)

```java
Page<GodoMemberEntity> findAll(MemberSearchCondition condition);
// QueryDSL BooleanBuilder로 동적 조건 구성
// LEFT JOIN godo_member_business, member_partner
// WHERE deleted_fl = 'n' 고정 (삭제 회원 제외)
// COUNT 쿼리 분리 (페이징 totalElements용)
```

**인덱스 활용:**
- `*_hash` 컬럼 검색 → 해시 인덱스 직접 사용
- `group_sno`, `app_fl`, `entry_dt` → 인덱스 활용
- `mem_id LIKE '%keyword%'` → 풀스캔 (23,000건 기준 허용 수준)

---

### MemberQueryController.java

```
GET  /api/members           → getMemberList(@ModelAttribute MemberSearchCondition)
GET  /api/members/summary   → getMemberSummary()
GET  /api/members/{memNo}   → getMemberDetail(@PathVariable Long memNo)
```

---

## 테스트 명세

### MemberMaskerTest.java

| 시나리오 | 입력 | 기댓값 |
|----------|------|--------|
| 이름 1자 | "홍" | "홍" |
| 이름 2자 | "홍길" | "홍*" |
| 이름 3자 | "홍길동" | "홍*동" |
| 이름 4자 | "홍길동이" | "홍**이" |
| 이름 null | null | null |
| 이메일 정상 | "hong@naver.com" | "ho***@naver.com" |
| 이메일 로컬 1자 | "h@test.com" | "*@test.com" |
| 이메일 null | null | null |
| 휴대폰 하이픈 | "010-1234-5678" | "010-****-5678" |
| 휴대폰 숫자만 | "01012345678" | "010****5678" |
| 휴대폰 null | null | null |

### MemberQueryControllerTest.java (`@WebMvcTest`)

| 시나리오 | 검증 |
|----------|------|
| GET /api/members 기본 | 200, content/page/size/totalElements 포함 |
| GET /api/members?searchType=NAME&keyword=홍길동 | NAME 검색 파라미터 전달 확인 |
| GET /api/members?group=B2B | group 필터 전달 확인 |
| GET /api/members/summary | 200, 5개 필드 반환 |
| GET /api/members/99999 (없음) | 404 |
| GET /api/members/{memNo} (존재) | 200, memName 평문 포함 |

### 통합 테스트 (`@SpringBootTest`)

| 시나리오 | 검증 |
|----------|------|
| 업로드 후 목록 조회 | totalElements 업로드 건수 일치 |
| mem_id 부분검색 | 부분 일치 행 반환 |
| 이름 해시 검색 | 정확 일치 행만 반환 |
| group=B2B 필터 | group_sno=9 행만 반환 |
| appFl=n 필터 | 미승인 행만 반환 |
| 페이징 totalElements | 전체 건수와 일치 |
| 상세 조회 복호화 | memName 평문 저장값과 일치 |
| 삭제 회원 제외 | deleted_fl='y' 행 목록에 미포함 |

---

## 완료 기준

- `./gradlew test` — 위 모든 테스트 GREEN
- `./gradlew build` — 오류 없음
- 실제 업로드 데이터로 목록 조회 API curl 테스트 (page=0, size=20)
- 상세 조회 API — DB 암호문 컬럼이 응답에 평문으로 반환됨 확인
- 기존 전체 테스트 영향 없음

---

## 프론트엔드 연결 지점

이 Iteration 완료 후 다음 정보를 프론트엔드 팀에 전달한다.

- API 응답 타입 확정 (api-spec-member.md 참조)
- 검색 타입별 동작 설명 (특히 이름·이메일·휴대폰은 정확 일치만 가능)
- 정렬 허용 컬럼 목록
- `lastSyncedAt` null 케이스 (업로드 전 초기 상태)
