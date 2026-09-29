# Iteration 36 Plan — 회원 업로드 파이프라인

> 작성일: 2026-06-11
> 의존: Iteration 35 (암호화 모듈 + Entity 완료)
> 이후: Iteration 37 (조회 API)
> 참조: `backend/docs/analysis/godomall-member-field-mapping.md`, `backend/docs/plan/member-management-ui-plan.md` 7-4

---

## 목표

고도몰 회원 엑셀 파일을 청크 단위로 업로드하여 DB에 upsert하는 파이프라인을 구현한다.
프론트엔드에서 SheetJS로 파싱한 행 데이터를 받아 정규화 → 암호화 → 저장까지 처리한다.

---

## 범위

### 신규 파일

```
backend/src/main/java/com/sfood/automation/member/
├── application/
│   ├── dto/
│   │   ├── MemberUploadStartRequest.java
│   │   ├── MemberUploadStartResponse.java
│   │   ├── MemberUploadRowsRequest.java
│   │   ├── MemberUploadChunkResponse.java
│   │   ├── MemberUploadCompleteRequest.java
│   │   └── MemberUploadCompleteResponse.java
│   ├── upload/
│   │   ├── MemberUploadSessionManager.java
│   │   └── MemberRowNormalizer.java
│   └── usecase/
│       └── UploadMembersUseCase.java
└── presentation/
    └── MemberUploadController.java

backend/src/test/java/com/sfood/automation/member/
├── application/upload/
│   └── MemberRowNormalizerTest.java
└── presentation/
    └── MemberUploadControllerTest.java
```

### 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `member/domain/port/MemberRepository.java` | `upsert`, `findAllMemNos`, `softDeleteNotIn` 메서드 추가 |
| `member/infrastructure/persistence/MemberRepositoryImpl.java` | 위 포트 구현체 추가 (신규이면 신규 생성) |

---

## 구현 명세

### MemberUploadStartRequest.java

```java
public record MemberUploadStartRequest(
    @NotNull MemberUploadType type,
    @Positive int totalRows,
    @NotBlank String fileName
) {}
```

### MemberUploadStartResponse.java

```java
public record MemberUploadStartResponse(String uploadId) {}
```

### MemberUploadRowsRequest.java

```java
public record MemberUploadRowsRequest(
    @NotEmpty List<List<Object>> rows  // 엑셀 원본 셀 값 그대로 (String/Double/null)
) {}
```

### MemberUploadChunkResponse.java

```java
public record MemberUploadChunkResponse(
    int processed,
    int inserted,
    int updated,
    List<FailedRow> failedRows
) {
    public record FailedRow(int rowIndex, String reason) {}
}
```

### MemberUploadCompleteRequest.java

```java
public record MemberUploadCompleteRequest(boolean applyDeletion) {}
```

### MemberUploadCompleteResponse.java

```java
public record MemberUploadCompleteResponse(
    int totalInserted,
    int totalUpdated,
    int totalDeleted,
    int totalFailed,
    List<MemberUploadChunkResponse.FailedRow> failedRows,
    boolean deletionSkipped,
    String deletionSkippedReason,  // null if not skipped
    String syncedAt                // ISO-8601
) {}
```

---

### MemberUploadSessionManager.java

인메모리 세션 관리 (`@Component`, 싱글턴):

```java
public class MemberUploadSessionManager {
    // 세션 구조
    record Session(
        String uploadId,
        MemberUploadType type,
        int totalRows,
        Set<Long> receivedMemNos,  // upsert 완료된 memNo 누적
        int inserted, int updated, int failed,
        List<FailedRow> failedRows,
        LocalDateTime createdAt
    ) {}

    // 주요 메서드
    String createSession(MemberUploadType type, int totalRows, String fileName);
    Session getSession(String uploadId);          // 없으면 UploadSessionNotFoundException
    void recordUpsert(String uploadId, long memNo, boolean isInsert);
    void recordFailure(String uploadId, int rowIndex, String reason);
    void expireSession(String uploadId);
    void evictExpired();   // 30분 경과 세션 자동 폐기 (@Scheduled)
}
```

---

### MemberRowNormalizer.java

필드 매핑 문서 2~4장 기준으로 엑셀 원본 셀 값을 DB 저장 가능한 형태로 변환한다.

**날짜 변환 (타입 혼재 분기 — 실측 확인):**

```java
// 같은 컬럼에 숫자(시리얼)와 문자열이 혼재할 수 있다
String normalizeDate(Object cellValue) {
    if (cellValue == null) return null;
    if (cellValue instanceof Double serial) {
        // Excel 시리얼: 1899-12-30 기준 일수
        LocalDate date = LocalDate.of(1899, 12, 30).plusDays((long) serial.doubleValue());
        return date.toString();  // YYYY-MM-DD
    }
    if (cellValue instanceof String s && !s.isBlank()) {
        // "YYYY-MM-DD HH:MM:SS" → 날짜 부분만 추출
        return s.trim().substring(0, 10);
    }
    return null;
}
```

**단위 제거 ("0점", "1500점", "0원"):**

```java
BigDecimal normalizeAmount(Object cellValue) {
    if (cellValue == null) return BigDecimal.ZERO;
    String s = cellValue.toString().replaceAll("[^0-9.]", "");
    return s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s);
}
```

**플래그 변환:**

```java
String normalizeFlag(Object cellValue) {
    if (cellValue == null) return "n";
    return "y".equalsIgnoreCase(cellValue.toString().trim()) ? "y" : "n";
}

String normalizePartnerRegistered(Object cellValue) {
    // "O" → "y", 공란 → "n"
    if (cellValue == null) return "n";
    return "O".equals(cellValue.toString().trim()) ? "y" : "n";
}
```

**숫자 셀 정수 변환:**

```java
Long normalizeLong(Object cellValue) {
    if (cellValue == null) return null;
    if (cellValue instanceof Double d) return d.longValue();  // "990954.0" → 990954
    if (cellValue instanceof String s && !s.isBlank()) return Long.parseLong(s.trim());
    return null;
}
```

**LIST 파일 (43컬럼) 행 매핑 순서 (필드 매핑 문서 5장 — 인덱스는 0-base):**

| 인덱스 | 엑셀 컬럼명 | 매핑 대상 |
|--------|------------|----------|
| 0 | 회원번호 | mem_no |
| 1 | 등록일 | entry_dt |
| 2 | 가입승인일 | app_dt |
| 3 | 가입승인여부 | app_fl |
| 4 | 가입경로 | entry_path |
| 5 | 최종로그인일 | last_login_dt |
| 6 | 회원구분 | member_fl |
| 7 | 등급 | group_sno |
| 8 | 아이디 | mem_id |
| 9 | 닉네임 | nick_name |
| 10 | 이름 | mem_name (암호화), mem_name_hash |
| 11 | 이메일 | email (암호화), email_hash |
| 12 | 휴대폰번호 | cell_phone (암호화), cell_phone_hash (하이픈 제거 후 해시) |
| 13 | 전화번호 | phone (암호화) |
| 14 | 주소 | address (암호화) |
| 15 | 메일수신여부 | mailling_fl |
| 16 | SMS수신여부 | sms_fl |
| 17 | 주문금액 | sale_amount |
| 18 | 마일리지 | mileage |
| 19 | 예치금 | deposit |
| 20 | 방문횟수 | login_count |
| 21 | 상호 | company |
| 22 | 사업자번호 | business_no (평문) |
| 23 | 대표자명 | ceo_name (암호화) |
| 24 | 업태 | service |
| 25 | 종목 | item |
| 26 | 사업자 주소 | com_address (암호화) |
| 27 | 팩스 번호 | fax |
| 28 | 추천인아이디 | recomm_id |
| 29 | 성별 | sex_fl |
| 30 | 생일 | birth_dt (암호화, 날짜 변환) |
| 31 | 결혼여부 | marri_fl |
| 32 | 결혼기념일 | marri_date |
| 33 | 직업 | job |
| 34 | 관심분야 | interest |
| 35 | 개인정보유효기간 | expiration_fl |
| 36 | 남기는말씀 | memo |
| 37 | 추가정보1 | ex1 |
| 38 | 추가정보2 | ex2 |
| 39 | 추가정보3 | ex3 |
| 40 | 추가정보4 | ex4 |
| 41 | 추가정보5 | ex5 |
| 42 | 추가정보6 | ex6 |

**TEMPLATE 파일 (51컬럼):**
인덱스 0~7: 에쓰푸드몰 전용 (거래처등록·거래처코드·거래처명·거래처명중복·세금계산서여부·중복여부·파일명 7개)
인덱스 8~50: LIST와 동일 순서

**해시 생성 규칙:**
- `mem_name_hash`: `HashUtil.hash(memName)`
- `email_hash`: `HashUtil.hash(email)`
- `cell_phone_hash`: `HashUtil.hash(cellPhone.replaceAll("[^0-9]", ""))` — 하이픈 제거 후 해시

---

### UploadMembersUseCase.java

**startUpload(request) → uploadId:**
- 세션 생성 후 uploadId 반환

**processChunk(uploadId, rows) → MemberUploadChunkResponse:**

```
for each row:
  1. mem_no 추출 → null이면 failedRows에 추가 (건너뜀)
  2. MemberRowNormalizer.normalize(row, type)
  3. GodoMemberEntity upsert (mem_no 기준 INSERT OR REPLACE / JPA merge)
  4. 사업자 등급이면 GodoMemberBusinessEntity upsert
  5. TEMPLATE 업로드이면 MemberPartnerEntity upsert (LIST는 건너뜀)
  6. 세션에 memNo 기록
트랜잭션: 청크 단위 (@Transactional)
```

**completeUpload(uploadId, applyDeletion) → MemberUploadCompleteResponse:**

```
if applyDeletion:
  미수신 memNo 집합 = DB 전체 memNo - 세션 receivedMemNos
  if 미수신 건수 > 전체의 10%:
    deletionSkipped = true, reason = "삭제 대상이 전체의 10%를 초과합니다 ..."
    return 결과 (삭제 미적용)
  else:
    deleted_fl = 'y' 일괄 업데이트 (미수신 memNo 대상)
세션 폐기
syncedAt 기록
```

---

### MemberUploadController.java

```
POST /api/members/uploads              → startUpload
POST /api/members/uploads/{id}/rows    → processChunk
POST /api/members/uploads/{id}/complete → completeUpload
```

세션 `id`가 없으면 `404 Not Found`.

---

## 테스트 명세

### MemberRowNormalizerTest.java

| 시나리오 | 입력 | 기댓값 |
|----------|------|--------|
| 날짜 시리얼 숫자 | `46169.0` | `"2026-05-22"` |
| 날짜 문자열 | `"2025-01-15 10:30:00"` | `"2025-01-15"` |
| 날짜 null | null | null |
| 마일리지 "1500점" | `"1500점"` | `1500` |
| 마일리지 "0점" | `"0점"` | `0` |
| 예치금 "0원" | `"0원"` | `0` |
| 주문금액 숫자 셀 | `12500.0` | `12500` |
| 플래그 승인 | `"y"` | `"y"` |
| 플래그 미승인 | `"n"` | `"n"` |
| 거래처등록 "O" | `"O"` | `"y"` |
| 거래처등록 공란 | `""` | `"n"` |
| 거래처코드 Double | `990954.0` | `990954L` |
| 휴대폰 해시 | `"010-1234-5678"` | `HashUtil.hash("01012345678")` |
| 이름 암호화 후 해시 | `"홍길동"` | 해시값 != plaintext |
| 빈 셀 이름 | null | mem_name = null, mem_name_hash = null |

### MemberUploadControllerTest.java (`@WebMvcTest`)

| 시나리오 | 검증 |
|----------|------|
| 유효한 startUpload 요청 | 201, uploadId 반환 |
| 유효한 processChunk 요청 (100행) | 200, processed=100 |
| 없는 uploadId로 processChunk | 404 |
| applyDeletion=false로 complete | 200, deletionSkipped=false, totalDeleted=0 |
| 10% 초과 삭제 시도 | 200, deletionSkipped=true |

### 업로드 통합 테스트 (`@SpringBootTest`)

| 시나리오 | 검증 |
|----------|------|
| LIST 파일 43컬럼 행 upsert | DB에 1행 저장, 재업로드 시 updated=1 (멱등) |
| mem_no 없는 행 | failedRows에 포함, 나머지 행은 정상 처리 |
| 사업자 등급 회원 | godo_member_business 행도 생성 |
| LIST 업로드 후 member_partner 수동 수정 | complete 후 partner 데이터 보존 확인 |
| applyDeletion=true, 10% 이하 | deleted_fl='y' 적용 확인 |

---

## 완료 기준

- `./gradlew test` — 위 모든 테스트 GREEN
- `./gradlew build` — 오류 없음
- 실제 `godomall-member-list.xlsx` 일부 행(100행)을 직접 업로드하여 DB 저장 확인
- 기존 전체 테스트 영향 없음
