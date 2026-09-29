# Iteration 35 Plan — 회원 도메인 기반: 암호화 모듈 + DB 스키마 + Entity

> 작성일: 2026-06-11
> 의존: Iteration 34 (기존 공통 인프라 완료)
> 이후: Iteration 36 (업로드 파이프라인), Iteration 37 (조회 API)
> 참조: `backend/docs/analysis/godomall-member-table.md`, `backend/docs/analysis/godomall-member-encryption-design.md`

---

## 목표

회원 도메인 구현의 기반이 되는 세 가지 요소를 준비한다.

1. AES-256-GCM 암호화 모듈 (`common/security/`)
2. DB 스키마 — SQLite `schema.sql` 테이블 3종 추가
3. JPA Entity + Repository (도메인 Port 포함)

이 Iteration이 완료되면 암호화 저장과 복호화 조회가 단위 테스트로 검증된 상태가 된다.

---

## 선행 조건 (착수 전 확인)

| 항목 | 확인 방법 |
|------|----------|
| `ENC_KEY` 환경 변수 생성 | `openssl rand -base64 32` 실행 후 `application-local.yml` 또는 OS 환경변수에 `ENC_KEY` 설정 |
| `application.yml`에 `app.encryption.key: ${ENC_KEY}` 바인딩 추가 | 기동 시 `EncryptionProperties` 빈 로드 확인 |

`ENC_KEY` 없이 기동하면 `EncryptionProperties` 검증에서 `IllegalStateException`을 던져 앱이 시작되지 않는다(fail-fast).

---

## 범위

### 신규 파일

```
backend/src/main/java/com/sfood/automation/
├── common/security/
│   ├── EncryptionProperties.java
│   ├── AesGcmEncryptor.java
│   ├── EncryptedFieldConverter.java
│   └── HashUtil.java
└── member/
    └── domain/
        ├── enums/
        │   ├── MemberGroup.java
        │   └── MemberUploadType.java
        └── port/
            └── MemberRepository.java
    └── infrastructure/
        └── persistence/
            ├── GodoMemberEntity.java
            ├── GodoMemberBusinessEntity.java
            ├── MemberPartnerEntity.java
            ├── GodoMemberJpaRepository.java
            ├── GodoMemberBusinessJpaRepository.java
            └── MemberPartnerJpaRepository.java

backend/src/main/resources/schema.sql  ← 테이블 3종 추가 (기존 파일 수정)

backend/src/test/java/com/sfood/automation/
├── common/security/
│   ├── AesGcmEncryptorTest.java
│   └── HashUtilTest.java
└── member/infrastructure/persistence/
    └── MemberEntityRoundTripTest.java
```

### 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `src/main/resources/schema.sql` | `godo_member`, `godo_member_business`, `member_partner` 테이블 + 인덱스 추가 |
| `src/main/resources/application.yml` | `app.encryption.key: ${ENC_KEY}` 바인딩 추가 |
| `src/test/resources/application.yml` | 테스트 전용 고정 ENC_KEY 설정 (`test.enc.key` 프로파일) |

---

## 구현 명세

### EncryptionProperties.java

```java
@ConfigurationProperties("app.encryption")
@Validated
public class EncryptionProperties {
    @NotBlank
    private String key;   // Base64 32바이트 (AES-256)
    // 검증: Base64 디코딩 후 32바이트 아니면 IllegalStateException
}
```

### AesGcmEncryptor.java

- 알고리즘: `AES/GCM/NoPadding`
- IV: 12바이트 랜덤 (`SecureRandom`)
- AuthTag: 128비트
- 저장 형식: `Base64URL( IV(12) || Ciphertext || Tag(16) )`
- `encrypt(String plaintext) → String`: null 입력 → null 반환
- `decrypt(String ciphertext) → String`: null 입력 → null 반환, 변조 시 `AEADBadTagException` → `DecryptionException`(RuntimeException) 발생

### EncryptedFieldConverter.java

```java
@Converter
@Component
public class EncryptedFieldConverter implements AttributeConverter<String, String> {
    // Spring 빈 주입: @Autowired AesGcmEncryptor
    // convertToDatabaseColumn: encrypt()
    // convertToEntityAttribute: decrypt()
}
```

Spring Boot 3.x Hibernate 6 에서는 `SpringBeanContainer` 등록 필요:
```yaml
spring.jpa.properties.hibernate.resource_locator: ...SpringBeanContainerBeanInstanceProducer
```
또는 `HibernatePropertiesCustomizer`로 등록. 기동 후 암호화 컬럼이 실제로 암호문으로 저장되는지 직접 SELECT로 확인한다.

### HashUtil.java

- `hash(String value) → String`: SHA-256, 고정 salt(`app.encryption.key` 앞 16바이트), HEX 소문자 64자리
- null/blank 입력 → null 반환
- 휴대폰 전처리는 호출 측에서 수행 (하이픈 제거 후 전달)

### MemberGroup.java

```java
public enum MemberGroup {
    GENERAL(1, "일반회원", false),
    BUSINESS_WHITE(2, "사업자회원_화이트", true),
    BUSINESS_ROSE(3, "사업자회원_로제", true),
    BUSINESS_RED(5, "사업자회원_레드", true),
    BUSINESS_VIP(6, "사업자회원_VIP", true),
    ADMIN(7, "관리자 등급", false),
    SKILLED(8, "기능장회원", false),
    B2B(9, "B2B 거래처", true);

    private final int sno;
    private final String label;
    private final boolean hasBusiness;

    public static MemberGroup of(int sno) { ... }  // 매핑 실패 시 GENERAL 반환
}
```

### MemberUploadType.java

```java
public enum MemberUploadType {
    LIST,     // 고도몰 list 엑셀 (43컬럼, 반복 동기화) — member_partner 보존
    TEMPLATE  // 분석 template 엑셀 (51컬럼, 초기 적재·거래처 일괄 갱신)
}
```

### GodoMemberEntity.java

- `@Entity @Table(name = "godo_member")`
- PK: `@Id @GeneratedValue(strategy = IDENTITY) Long id`
- `@Column(unique = true) Long memNo`
- 암호화 대상 컬럼: `@Convert(converter = EncryptedFieldConverter.class)` 적용
  - `memName`, `email`, `cellPhone`, `phone`, `birthDt`, `address`
- 검색용 해시 컬럼: `memNameHash`, `emailHash`, `cellPhoneHash` (일반 TEXT, 인덱스)
- `deletedFl` (default "n"), `syncedAt`, `createdAt`, `updatedAt`
- `@ToString.Exclude` 를 암호화 필드에 반드시 적용 (로그 노출 방지)

### GodoMemberBusinessEntity.java

- `@Entity @Table(name = "godo_member_business")`
- FK: `@Column(unique = true) Long memNo`
- 암호화: `ceoName`, `comAddress`
- `businessNo` — 평문 (암호화 없음)

### MemberPartnerEntity.java

- `@Entity @Table(name = "member_partner")`
- FK: `@Column(unique = true) Long memNo`
- 일반 컬럼: `partnerRegisteredFl`, `partnerCode`, `partnerName`, `taxInvoiceFl`, `duplicateCheck`

### MemberRepository.java (Port)

```java
public interface MemberRepository {
    Optional<GodoMemberEntity> findByMemNo(Long memNo);
    void save(GodoMemberEntity member);
    void saveAll(List<GodoMemberEntity> members);
    // 이후 iteration에서 조회 메서드 추가
}
```

---

## 테스트 명세

### AesGcmEncryptorTest.java

| 시나리오 | 검증 내용 |
|----------|----------|
| 암복호화 라운드트립 | encrypt("홍길동") → decrypt → "홍길동" |
| null 입력 | encrypt(null) == null, decrypt(null) == null |
| 동일 평문 다른 암호문 | encrypt("A") != encrypt("A") (IV 무작위) |
| 변조 암호문 | 암호문 1바이트 수정 후 decrypt → DecryptionException |
| 빈 문자열 | encrypt("") → decrypt → "" |

### HashUtilTest.java

| 시나리오 | 검증 내용 |
|----------|----------|
| 동일 입력 동일 해시 | hash("01012345678") == hash("01012345678") |
| 다른 입력 다른 해시 | hash("A") != hash("B") |
| HEX 64자리 | `^[0-9a-f]{64}$` 정규식 일치 |
| null 입력 | null 반환 |
| salt 적용 | hash("A") != SHA256("A") (unsalted) |

### MemberEntityRoundTripTest.java

- `@DataJpaTest` + 테스트 전용 ENC_KEY
- GodoMemberEntity 저장 후 JDBC로 직접 SELECT → `mem_name` 컬럼 값이 암호문(Base64)임을 확인
- 저장 후 JPA 재조회 → 복호화된 평문과 저장 전 값 동일
- GodoMemberBusinessEntity, MemberPartnerEntity 기본 CRUD 확인

---

## 완료 기준

- `./gradlew test` — AesGcmEncryptorTest, HashUtilTest, MemberEntityRoundTripTest 전부 GREEN
- `./gradlew build` — 오류 없음
- 로컬 기동 시 `godo_member`, `godo_member_business`, `member_partner` 테이블 자동 생성 확인
- 암호화 컬럼이 DB에 평문으로 저장되지 않음을 직접 SELECT로 확인
- 기존 전체 테스트 영향 없음

---

## 주의 사항

- `ENC_KEY`는 소스 코드에 포함하지 않는다. 테스트 키는 `src/test/resources`에서만 사용.
- `EncryptedFieldConverter` 빈 주입이 실패할 경우 정적 홀더(static initializer) 방식으로 폴백 (`암호화 설계 문서 7-3` 참조).
- 기존 테이블(`goods`, `batch_history` 등)에는 변경이 없다.
