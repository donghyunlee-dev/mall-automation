# 회원·거래처 연동 고도화 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 거래처 상태 관리, 거래처 자동 생성, 사업자/거래처 이력 로그, 공통코드 관리, 중복 거래처 검사, 수집 조건 및 사업자 저장 조건 개선을 백엔드에 구현한다.

**Architecture:** 기존 Layered Architecture (Controller → UseCase → Repository → JPA Entity) 패턴을 그대로 따른다. 신규 테이블 3개(godo_member_business_log, member_partner_log, common_code)를 추가하고, 기존 테이블 2개(member_partner, godo_member_business) 컬럼을 변경한다. 로그는 변경 전 전체 스냅샷 방식으로 저장한다.

**Tech Stack:** Java 21, Spring Boot 3, JPA (Hibernate), QueryDSL, JDBC (JdbcTemplate), SQLite(로컬), PostgreSQL(운영)

**Spec:** `docs/superpowers/specs/2026-07-02-member-partner-enhancement-design.md`

---

## 파일 구조

### 신규 생성

```
src/main/java/com/sfood/automation/
  member/
    infrastructure/persistence/
      GodoMemberBusinessLogEntity.java
      GodoMemberBusinessLogJpaRepository.java
      MemberPartnerLogEntity.java
      MemberPartnerLogJpaRepository.java
    application/
      usecase/
        UpdateMemberBusinessUseCase.java
        GetMemberBusinessLogsUseCase.java
        GetMemberPartnerLogsUseCase.java
      dto/
        MemberBusinessUpdateRequest.java
        MemberBusinessLogListResponse.java
        MemberPartnerLogListResponse.java
  common_code/
    infrastructure/persistence/
      CommonCodeEntity.java
      CommonCodeJpaRepository.java
    application/
      usecase/CommonCodeUseCase.java
      dto/
        CommonCodeRequest.java
        CommonCodeListResponse.java
    presentation/
      CommonCodeController.java

src/test/java/com/sfood/automation/
  member/
    application/usecase/
      UpdateMemberBusinessUseCaseIntegrationTest.java
      GetMemberLogsUseCaseIntegrationTest.java
      FetchNewMembersUseCaseIntegrationTest.java   (기존 파일 신규)
    presentation/
      MemberManageControllerTest.java              (기존 수정)
  common_code/
    presentation/
      CommonCodeControllerTest.java
```

### 기존 수정

```
src/main/resources/schema.sql
src/main/resources/postgresql/schema-member-postgresql.sql
src/main/java/com/sfood/automation/
  common/bootstrap/
    SqliteSchemaMigration.java
  member/
    infrastructure/persistence/
      MemberPartnerEntity.java
      GodoMemberBusinessEntity.java
      MemberRepositoryImpl.java             (SQL 상수 + 신규 메서드)
    domain/port/
      MemberRepository.java                 (신규 메서드 선언)
    application/
      usecase/
        UpdateMemberPartnerUseCase.java
        FetchNewMembersUseCase.java
        UploadMembersUseCase.java
      upload/MemberRowNormalizer.java
      dto/
        MemberPartnerUpdateRequest.java
        MemberPartnerUpdateResponse.java
    presentation/
      MemberManageController.java
src/test/java/com/sfood/automation/
  support/SharedSqliteTestDatabase.java
  member/application/usecase/
    UpdateMemberPartnerUseCaseIntegrationTest.java  (기존 수정)
```

---

## Task 1: DB 스키마 변경

**Files:**
- Modify: `src/main/resources/schema.sql`
- Modify: `src/main/resources/postgresql/schema-member-postgresql.sql`
- Modify: `src/main/java/com/sfood/automation/common/bootstrap/SqliteSchemaMigration.java`
- Modify: `src/test/java/com/sfood/automation/support/SharedSqliteTestDatabase.java`

- [ ] **Step 1: schema.sql — member_partner 테이블 수정 (duplicate_check 제거, member_status 추가)**

`schema.sql`의 `member_partner` CREATE TABLE을 아래로 교체한다.

```sql
CREATE TABLE IF NOT EXISTS member_partner (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    mem_no                BIGINT  NOT NULL UNIQUE,
    partner_registered_fl TEXT NOT NULL DEFAULT 'n',
    partner_code          TEXT,
    partner_name          TEXT,
    tax_invoice_fl        TEXT NOT NULL DEFAULT 'n',
    member_status         TEXT NOT NULL DEFAULT 'NORMAL',
    created_at            TEXT NOT NULL,
    updated_at            TEXT NOT NULL,
    FOREIGN KEY (mem_no) REFERENCES godo_member(mem_no)
);
```

- [ ] **Step 2: schema.sql — godo_member_business 테이블 수정 (duplicate_mem_no 추가)**

`schema.sql`의 `godo_member_business` CREATE TABLE에 `duplicate_mem_no TEXT` 컬럼을 추가한다.

```sql
CREATE TABLE IF NOT EXISTS godo_member_business (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    mem_no          BIGINT  NOT NULL UNIQUE,
    company         TEXT,
    business_no     TEXT,
    ceo_name        TEXT,
    service         TEXT,
    item            TEXT,
    com_address     TEXT,
    biz_license_file TEXT,
    duplicate_mem_no TEXT,
    created_at      TEXT NOT NULL,
    updated_at      TEXT NOT NULL,
    FOREIGN KEY (mem_no) REFERENCES godo_member(mem_no)
);
```

- [ ] **Step 3: schema.sql — 신규 테이블 3개 추가**

`schema.sql` 맨 아래에 추가한다.

```sql
CREATE TABLE IF NOT EXISTS godo_member_business_log (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    mem_no           BIGINT  NOT NULL,
    company          TEXT,
    business_no      TEXT,
    ceo_name         TEXT,
    service          TEXT,
    item             TEXT,
    com_address      TEXT,
    biz_license_file TEXT,
    logged_at        TEXT NOT NULL,
    logged_by        TEXT,
    created_at       TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_godo_member_business_log_mem_no
    ON godo_member_business_log(mem_no);
CREATE INDEX IF NOT EXISTS idx_godo_member_business_log_logged_at
    ON godo_member_business_log(logged_at);

CREATE TABLE IF NOT EXISTS member_partner_log (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    mem_no                BIGINT  NOT NULL,
    partner_registered_fl TEXT,
    partner_code          TEXT,
    partner_name          TEXT,
    tax_invoice_fl        TEXT,
    member_status         TEXT,
    logged_at             TEXT NOT NULL,
    logged_by             TEXT,
    created_at            TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_member_partner_log_mem_no
    ON member_partner_log(mem_no);
CREATE INDEX IF NOT EXISTS idx_member_partner_log_logged_at
    ON member_partner_log(logged_at);

CREATE TABLE IF NOT EXISTS common_code (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    group_code  TEXT NOT NULL,
    group_name  TEXT NOT NULL,
    code        TEXT NOT NULL,
    code_name   TEXT NOT NULL,
    note        TEXT,
    use_yn      TEXT NOT NULL DEFAULT 'Y',
    created_at  TEXT NOT NULL,
    updated_at  TEXT NOT NULL,
    UNIQUE (group_code, code)
);
CREATE INDEX IF NOT EXISTS idx_common_code_group_code
    ON common_code(group_code);
```

- [ ] **Step 4: SqliteSchemaMigration — 기존 DB 마이그레이션 메서드 추가**

`SqliteSchemaMigration.afterPropertiesSet()`에 신규 메서드 호출을 추가하고, 메서드를 구현한다.

```java
@Override
public void afterPropertiesSet() {
    // 기존 메서드 호출들 유지 ...
    ensureGoodsSyncBlockedColumn();
    ensureGoodsRemainValidDaysColumn();
    ensureCommonStatusConfigRemainValidDaysColumn();
    synchronizeGoodsRemainValidDaysFromConfig();
    ensureGoodsStatusLogRemainValidDaysColumn();
    ensureGoodsStatusLogNullableValidPercent();
    // 신규 추가
    ensureMemberPartnerMemberStatusColumn();
    ensureGodoMemberBusinessDuplicateMemNoColumn();
    ensureGodoMemberBusinessLogTable();
    ensureMemberPartnerLogTable();
    ensureCommonCodeTable();
}

private void ensureMemberPartnerMemberStatusColumn() {
    ensureColumn("member_partner", "member_status",
        "ALTER TABLE member_partner ADD COLUMN member_status TEXT NOT NULL DEFAULT 'NORMAL'");
}

private void ensureGodoMemberBusinessDuplicateMemNoColumn() {
    ensureColumn("godo_member_business", "duplicate_mem_no",
        "ALTER TABLE godo_member_business ADD COLUMN duplicate_mem_no TEXT");
}

private void ensureGodoMemberBusinessLogTable() {
    List<String> tables = jdbcTemplate.query(
        "SELECT name FROM sqlite_master WHERE type='table' AND name='godo_member_business_log'",
        (rs, i) -> rs.getString("name"));
    if (!tables.isEmpty()) return;
    jdbcTemplate.execute(
        "CREATE TABLE godo_member_business_log (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
        "mem_no BIGINT NOT NULL," +
        "company TEXT, business_no TEXT, ceo_name TEXT, service TEXT, item TEXT," +
        "com_address TEXT, biz_license_file TEXT," +
        "logged_at TEXT NOT NULL, logged_by TEXT, created_at TEXT NOT NULL)");
    jdbcTemplate.execute(
        "CREATE INDEX idx_godo_member_business_log_mem_no ON godo_member_business_log(mem_no)");
    jdbcTemplate.execute(
        "CREATE INDEX idx_godo_member_business_log_logged_at ON godo_member_business_log(logged_at)");
}

private void ensureMemberPartnerLogTable() {
    List<String> tables = jdbcTemplate.query(
        "SELECT name FROM sqlite_master WHERE type='table' AND name='member_partner_log'",
        (rs, i) -> rs.getString("name"));
    if (!tables.isEmpty()) return;
    jdbcTemplate.execute(
        "CREATE TABLE member_partner_log (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
        "mem_no BIGINT NOT NULL," +
        "partner_registered_fl TEXT, partner_code TEXT, partner_name TEXT," +
        "tax_invoice_fl TEXT, member_status TEXT," +
        "logged_at TEXT NOT NULL, logged_by TEXT, created_at TEXT NOT NULL)");
    jdbcTemplate.execute(
        "CREATE INDEX idx_member_partner_log_mem_no ON member_partner_log(mem_no)");
    jdbcTemplate.execute(
        "CREATE INDEX idx_member_partner_log_logged_at ON member_partner_log(logged_at)");
}

private void ensureCommonCodeTable() {
    List<String> tables = jdbcTemplate.query(
        "SELECT name FROM sqlite_master WHERE type='table' AND name='common_code'",
        (rs, i) -> rs.getString("name"));
    if (!tables.isEmpty()) return;
    jdbcTemplate.execute(
        "CREATE TABLE common_code (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
        "group_code TEXT NOT NULL, group_name TEXT NOT NULL," +
        "code TEXT NOT NULL, code_name TEXT NOT NULL," +
        "note TEXT, use_yn TEXT NOT NULL DEFAULT 'Y'," +
        "created_at TEXT NOT NULL, updated_at TEXT NOT NULL," +
        "UNIQUE (group_code, code))");
    jdbcTemplate.execute(
        "CREATE INDEX idx_common_code_group_code ON common_code(group_code)");
}
```

- [ ] **Step 5: SharedSqliteTestDatabase — 테스트 DB 마이그레이션 동기화**

`SharedSqliteTestDatabase.ensureSchemaCompatibility()` 내 `try` 블록 마지막에 추가한다.

```java
ensureColumn(statement, "member_partner", "member_status",
    "ALTER TABLE member_partner ADD COLUMN member_status TEXT NOT NULL DEFAULT 'NORMAL'");
ensureColumn(statement, "godo_member_business", "duplicate_mem_no",
    "ALTER TABLE godo_member_business ADD COLUMN duplicate_mem_no TEXT");
ensureTable(statement, "godo_member_business_log",
    "CREATE TABLE godo_member_business_log (" +
    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
    "mem_no BIGINT NOT NULL," +
    "company TEXT, business_no TEXT, ceo_name TEXT, service TEXT, item TEXT," +
    "com_address TEXT, biz_license_file TEXT," +
    "logged_at TEXT NOT NULL, logged_by TEXT, created_at TEXT NOT NULL)");
ensureTable(statement, "member_partner_log",
    "CREATE TABLE member_partner_log (" +
    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
    "mem_no BIGINT NOT NULL," +
    "partner_registered_fl TEXT, partner_code TEXT, partner_name TEXT," +
    "tax_invoice_fl TEXT, member_status TEXT," +
    "logged_at TEXT NOT NULL, logged_by TEXT, created_at TEXT NOT NULL)");
ensureTable(statement, "common_code",
    "CREATE TABLE common_code (" +
    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
    "group_code TEXT NOT NULL, group_name TEXT NOT NULL," +
    "code TEXT NOT NULL, code_name TEXT NOT NULL," +
    "note TEXT, use_yn TEXT NOT NULL DEFAULT 'Y'," +
    "created_at TEXT NOT NULL, updated_at TEXT NOT NULL," +
    "UNIQUE (group_code, code))");
```

`SharedSqliteTestDatabase`에 `ensureTable` 헬퍼 메서드를 추가한다.

```java
private static void ensureTable(Statement statement, String tableName, String createSql) throws Exception {
    if (tableExists(statement, tableName)) return;
    statement.execute(createSql);
}
```

- [ ] **Step 6: schema-member-postgresql.sql — PostgreSQL 마이그레이션**

파일 맨 아래에 추가한다.

```sql
-- member_partner 변경
ALTER TABLE IF EXISTS member_partner ADD COLUMN IF NOT EXISTS member_status TEXT NOT NULL DEFAULT 'NORMAL';
ALTER TABLE IF EXISTS member_partner DROP COLUMN IF EXISTS duplicate_check;

-- godo_member_business 변경
ALTER TABLE IF EXISTS godo_member_business ADD COLUMN IF NOT EXISTS duplicate_mem_no TEXT;

-- 신규 테이블
CREATE TABLE IF NOT EXISTS godo_member_business_log (
    id               INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    mem_no           BIGINT  NOT NULL,
    company          TEXT,
    business_no      TEXT,
    ceo_name         TEXT,
    service          TEXT,
    item             TEXT,
    com_address      TEXT,
    biz_license_file TEXT,
    logged_at        TEXT NOT NULL,
    logged_by        TEXT,
    created_at       TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_godo_member_business_log_mem_no
    ON godo_member_business_log(mem_no);
CREATE INDEX IF NOT EXISTS idx_godo_member_business_log_logged_at
    ON godo_member_business_log(logged_at);

CREATE TABLE IF NOT EXISTS member_partner_log (
    id                    INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    mem_no                BIGINT  NOT NULL,
    partner_registered_fl TEXT,
    partner_code          TEXT,
    partner_name          TEXT,
    tax_invoice_fl        TEXT,
    member_status         TEXT,
    logged_at             TEXT NOT NULL,
    logged_by             TEXT,
    created_at            TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_member_partner_log_mem_no
    ON member_partner_log(mem_no);
CREATE INDEX IF NOT EXISTS idx_member_partner_log_logged_at
    ON member_partner_log(logged_at);

CREATE TABLE IF NOT EXISTS common_code (
    id          INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    group_code  TEXT NOT NULL,
    group_name  TEXT NOT NULL,
    code        TEXT NOT NULL,
    code_name   TEXT NOT NULL,
    note        TEXT,
    use_yn      TEXT NOT NULL DEFAULT 'Y',
    created_at  TEXT NOT NULL,
    updated_at  TEXT NOT NULL,
    UNIQUE (group_code, code)
);
CREATE INDEX IF NOT EXISTS idx_common_code_group_code
    ON common_code(group_code);
```

- [ ] **Step 7: 빌드 확인**

```bash
cd /home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: 커밋**

```bash
git add src/main/resources/schema.sql \
        src/main/resources/postgresql/schema-member-postgresql.sql \
        src/main/java/com/sfood/automation/common/bootstrap/SqliteSchemaMigration.java \
        src/test/java/com/sfood/automation/support/SharedSqliteTestDatabase.java
git commit -m "feat: DB 스키마 변경 — member_status 추가, duplicate_mem_no 추가, 로그/공통코드 테이블 신규"
```

---

## Task 2: 엔티티 계층 변경 및 신규 생성

**Files:**
- Modify: `src/main/java/com/sfood/automation/member/infrastructure/persistence/MemberPartnerEntity.java`
- Modify: `src/main/java/com/sfood/automation/member/infrastructure/persistence/GodoMemberBusinessEntity.java`
- Create: `src/main/java/com/sfood/automation/member/infrastructure/persistence/GodoMemberBusinessLogEntity.java`
- Create: `src/main/java/com/sfood/automation/member/infrastructure/persistence/GodoMemberBusinessLogJpaRepository.java`
- Create: `src/main/java/com/sfood/automation/member/infrastructure/persistence/MemberPartnerLogEntity.java`
- Create: `src/main/java/com/sfood/automation/member/infrastructure/persistence/MemberPartnerLogJpaRepository.java`
- Create: `src/main/java/com/sfood/automation/common_code/infrastructure/persistence/CommonCodeEntity.java`
- Create: `src/main/java/com/sfood/automation/common_code/infrastructure/persistence/CommonCodeJpaRepository.java`

- [ ] **Step 1: MemberPartnerEntity — duplicateCheck 제거, memberStatus 추가**

`duplicateCheck` 필드 및 getter/setter를 제거하고 `memberStatus`를 추가한다.

```java
// 제거할 필드/메서드:
@Column(name = "duplicate_check")
private String duplicateCheck;
public String getDuplicateCheck() { return duplicateCheck; }
public void setDuplicateCheck(String duplicateCheck) { this.duplicateCheck = duplicateCheck; }

// 추가할 필드/메서드:
@Column(name = "member_status", nullable = false)
private String memberStatus = "NORMAL";
public String getMemberStatus() { return memberStatus; }
public void setMemberStatus(String memberStatus) { this.memberStatus = memberStatus; }
```

- [ ] **Step 2: GodoMemberBusinessEntity — duplicateMemNo 추가**

```java
// 기존 updatedAt 필드 아래에 추가
@Column(name = "duplicate_mem_no")
private String duplicateMemNo;
public String getDuplicateMemNo() { return duplicateMemNo; }
public void setDuplicateMemNo(String duplicateMemNo) { this.duplicateMemNo = duplicateMemNo; }
```

- [ ] **Step 3: GodoMemberBusinessLogEntity 생성**

```java
package com.sfood.automation.member.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "godo_member_business_log")
public class GodoMemberBusinessLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "mem_no", nullable = false)
    private Long memNo;

    private String company;

    @Column(name = "business_no")
    private String businessNo;

    @Column(name = "ceo_name")
    private String ceoName;

    private String service;
    private String item;

    @Column(name = "com_address")
    private String comAddress;

    @Column(name = "biz_license_file")
    private String bizLicenseFile;

    @Column(name = "logged_at", nullable = false)
    private String loggedAt;

    @Column(name = "logged_by")
    private String loggedBy;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    // getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Long getMemNo() { return memNo; }
    public void setMemNo(Long memNo) { this.memNo = memNo; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getBusinessNo() { return businessNo; }
    public void setBusinessNo(String businessNo) { this.businessNo = businessNo; }
    public String getCeoName() { return ceoName; }
    public void setCeoName(String ceoName) { this.ceoName = ceoName; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
    public String getComAddress() { return comAddress; }
    public void setComAddress(String comAddress) { this.comAddress = comAddress; }
    public String getBizLicenseFile() { return bizLicenseFile; }
    public void setBizLicenseFile(String bizLicenseFile) { this.bizLicenseFile = bizLicenseFile; }
    public String getLoggedAt() { return loggedAt; }
    public void setLoggedAt(String loggedAt) { this.loggedAt = loggedAt; }
    public String getLoggedBy() { return loggedBy; }
    public void setLoggedBy(String loggedBy) { this.loggedBy = loggedBy; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 4: GodoMemberBusinessLogJpaRepository 생성**

```java
package com.sfood.automation.member.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GodoMemberBusinessLogJpaRepository
    extends JpaRepository<GodoMemberBusinessLogEntity, Integer> {

    @Query("SELECT l FROM GodoMemberBusinessLogEntity l WHERE l.memNo = :memNo ORDER BY l.loggedAt DESC")
    List<GodoMemberBusinessLogEntity> findByMemNoOrderByLoggedAtDesc(@Param("memNo") Long memNo);
}
```

- [ ] **Step 5: MemberPartnerLogEntity 생성**

```java
package com.sfood.automation.member.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "member_partner_log")
public class MemberPartnerLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "mem_no", nullable = false)
    private Long memNo;

    @Column(name = "partner_registered_fl")
    private String partnerRegisteredFl;

    @Column(name = "partner_code")
    private String partnerCode;

    @Column(name = "partner_name")
    private String partnerName;

    @Column(name = "tax_invoice_fl")
    private String taxInvoiceFl;

    @Column(name = "member_status")
    private String memberStatus;

    @Column(name = "logged_at", nullable = false)
    private String loggedAt;

    @Column(name = "logged_by")
    private String loggedBy;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    // getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Long getMemNo() { return memNo; }
    public void setMemNo(Long memNo) { this.memNo = memNo; }
    public String getPartnerRegisteredFl() { return partnerRegisteredFl; }
    public void setPartnerRegisteredFl(String partnerRegisteredFl) { this.partnerRegisteredFl = partnerRegisteredFl; }
    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }
    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }
    public String getTaxInvoiceFl() { return taxInvoiceFl; }
    public void setTaxInvoiceFl(String taxInvoiceFl) { this.taxInvoiceFl = taxInvoiceFl; }
    public String getMemberStatus() { return memberStatus; }
    public void setMemberStatus(String memberStatus) { this.memberStatus = memberStatus; }
    public String getLoggedAt() { return loggedAt; }
    public void setLoggedAt(String loggedAt) { this.loggedAt = loggedAt; }
    public String getLoggedBy() { return loggedBy; }
    public void setLoggedBy(String loggedBy) { this.loggedBy = loggedBy; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 6: MemberPartnerLogJpaRepository 생성**

```java
package com.sfood.automation.member.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberPartnerLogJpaRepository
    extends JpaRepository<MemberPartnerLogEntity, Integer> {

    @Query("SELECT l FROM MemberPartnerLogEntity l WHERE l.memNo = :memNo ORDER BY l.loggedAt DESC")
    List<MemberPartnerLogEntity> findByMemNoOrderByLoggedAtDesc(@Param("memNo") Long memNo);
}
```

- [ ] **Step 7: CommonCodeEntity 생성**

```java
package com.sfood.automation.common_code.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "common_code")
public class CommonCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "group_code", nullable = false)
    private String groupCode;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "code_name", nullable = false)
    private String codeName;

    private String note;

    @Column(name = "use_yn", nullable = false)
    private String useYn = "Y";

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @Column(name = "updated_at", nullable = false)
    private String updatedAt;

    // getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCodeName() { return codeName; }
    public void setCodeName(String codeName) { this.codeName = codeName; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 8: CommonCodeJpaRepository 생성**

```java
package com.sfood.automation.common_code.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommonCodeJpaRepository extends JpaRepository<CommonCodeEntity, Integer> {

    List<CommonCodeEntity> findByGroupCodeOrderByCodeAsc(String groupCode);

    List<CommonCodeEntity> findAllByOrderByGroupCodeAscCodeAsc();

    boolean existsByGroupCodeAndCodeAndIdNot(String groupCode, String code, Integer id);

    boolean existsByGroupCodeAndCode(String groupCode, String code);
}
```

- [ ] **Step 9: 빌드 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL (QueryDSL Q클래스 자동 생성 포함)

- [ ] **Step 10: 커밋**

```bash
git add src/main/java/com/sfood/automation/member/infrastructure/persistence/ \
        src/main/java/com/sfood/automation/common_code/
git commit -m "feat: 엔티티 변경 — memberStatus/duplicateMemNo 추가, 로그·공통코드 엔티티 신규"
```

---

## Task 3: DTO 변경 및 신규 생성

**Files:**
- Modify: `src/main/java/com/sfood/automation/member/application/dto/MemberPartnerUpdateRequest.java`
- Modify: `src/main/java/com/sfood/automation/member/application/dto/MemberPartnerUpdateResponse.java`
- Modify: `src/main/java/com/sfood/automation/member/application/upload/MemberRowNormalizer.java`
- Create: `src/main/java/com/sfood/automation/member/application/dto/MemberBusinessUpdateRequest.java`
- Create: `src/main/java/com/sfood/automation/member/application/dto/MemberBusinessLogListResponse.java`
- Create: `src/main/java/com/sfood/automation/member/application/dto/MemberPartnerLogListResponse.java`
- Create: `src/main/java/com/sfood/automation/common_code/application/dto/CommonCodeRequest.java`
- Create: `src/main/java/com/sfood/automation/common_code/application/dto/CommonCodeListResponse.java`

- [ ] **Step 1: MemberPartnerUpdateRequest — duplicateCheck 제거, memberStatus 추가**

```java
package com.sfood.automation.member.application.dto;

public record MemberPartnerUpdateRequest(
    String partnerRegisteredFl,
    String partnerCode,
    String partnerName,
    String taxInvoiceFl,
    String memberStatus,
    String bizLicenseFile
) {}
```

- [ ] **Step 2: MemberPartnerUpdateResponse — duplicateCheck 제거, memberStatus 추가**

```java
package com.sfood.automation.member.application.dto;

public record MemberPartnerUpdateResponse(
    String partnerRegisteredFl,
    String partnerCode,
    String partnerName,
    String taxInvoiceFl,
    String memberStatus,
    String bizLicenseFile,
    String updatedAt
) {}
```

- [ ] **Step 3: MemberRowNormalizer.applyPartnerFields — duplicateCheck 제거, memberStatus 기본값 설정**

`applyPartnerFields` 메서드를 아래로 교체한다.

```java
public void applyPartnerFields(MemberPartnerEntity partner, List<Object> row) {
    partner.setPartnerRegisteredFl(normalizePartnerRegistered(templateCell(row, 1)));
    Long partnerCode = normalizeLong(templateCell(row, 2));
    partner.setPartnerCode(partnerCode == null ? null : String.valueOf(partnerCode));
    partner.setPartnerName(normalizeString(templateCell(row, 3), null));
    partner.setTaxInvoiceFl(normalizePartnerRegistered(templateCell(row, 5)));
    partner.setMemberStatus("NORMAL");
}
```

- [ ] **Step 4: MemberBusinessUpdateRequest 생성**

```java
package com.sfood.automation.member.application.dto;

public record MemberBusinessUpdateRequest(
    String company,
    String businessNo,
    String ceoName,
    String service,
    String item,
    String comAddress,
    String bizLicenseFile
) {}
```

- [ ] **Step 5: MemberBusinessLogListResponse 생성**

```java
package com.sfood.automation.member.application.dto;

import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessLogEntity;
import java.util.List;

public record MemberBusinessLogListResponse(List<LogItem> logs) {

    public record LogItem(
        Integer id,
        Long memNo,
        String company,
        String businessNo,
        String ceoName,
        String service,
        String item,
        String comAddress,
        String bizLicenseFile,
        String loggedAt,
        String loggedBy
    ) {}

    public static MemberBusinessLogListResponse from(
            List<GodoMemberBusinessLogEntity> entities) {
        List<LogItem> items = entities.stream().map(e -> new LogItem(
            e.getId(), e.getMemNo(), e.getCompany(), e.getBusinessNo(),
            e.getCeoName(), e.getService(), e.getItem(), e.getComAddress(),
            e.getBizLicenseFile(), e.getLoggedAt(), e.getLoggedBy()
        )).toList();
        return new MemberBusinessLogListResponse(items);
    }
}
```

- [ ] **Step 6: MemberPartnerLogListResponse 생성**

```java
package com.sfood.automation.member.application.dto;

import com.sfood.automation.member.infrastructure.persistence.MemberPartnerLogEntity;
import java.util.List;

public record MemberPartnerLogListResponse(List<LogItem> logs) {

    public record LogItem(
        Integer id,
        Long memNo,
        String partnerRegisteredFl,
        String partnerCode,
        String partnerName,
        String taxInvoiceFl,
        String memberStatus,
        String loggedAt,
        String loggedBy
    ) {}

    public static MemberPartnerLogListResponse from(List<MemberPartnerLogEntity> entities) {
        List<LogItem> items = entities.stream().map(e -> new LogItem(
            e.getId(), e.getMemNo(), e.getPartnerRegisteredFl(), e.getPartnerCode(),
            e.getPartnerName(), e.getTaxInvoiceFl(), e.getMemberStatus(),
            e.getLoggedAt(), e.getLoggedBy()
        )).toList();
        return new MemberPartnerLogListResponse(items);
    }
}
```

- [ ] **Step 7: CommonCodeRequest 생성**

```java
package com.sfood.automation.common_code.application.dto;

public record CommonCodeRequest(
    String groupCode,
    String groupName,
    String code,
    String codeName,
    String note,
    String useYn
) {}
```

- [ ] **Step 8: CommonCodeListResponse 생성**

```java
package com.sfood.automation.common_code.application.dto;

import com.sfood.automation.common_code.infrastructure.persistence.CommonCodeEntity;
import java.util.List;

public record CommonCodeListResponse(List<CodeItem> codes) {

    public record CodeItem(
        Integer id,
        String groupCode,
        String groupName,
        String code,
        String codeName,
        String note,
        String useYn,
        String createdAt,
        String updatedAt
    ) {}

    public static CommonCodeListResponse from(List<CommonCodeEntity> entities) {
        List<CodeItem> items = entities.stream().map(e -> new CodeItem(
            e.getId(), e.getGroupCode(), e.getGroupName(),
            e.getCode(), e.getCodeName(), e.getNote(), e.getUseYn(),
            e.getCreatedAt(), e.getUpdatedAt()
        )).toList();
        return new CommonCodeListResponse(items);
    }
}
```

- [ ] **Step 9: 빌드 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 10: 커밋**

```bash
git add src/main/java/com/sfood/automation/member/application/dto/ \
        src/main/java/com/sfood/automation/member/application/upload/MemberRowNormalizer.java \
        src/main/java/com/sfood/automation/common_code/application/dto/
git commit -m "feat: DTO 변경 — duplicateCheck 제거, memberStatus 추가, 이력·공통코드 DTO 신규"
```

---

## Task 4: MemberRepository Port + Impl 확장

**Files:**
- Modify: `src/main/java/com/sfood/automation/member/domain/port/MemberRepository.java`
- Modify: `src/main/java/com/sfood/automation/member/infrastructure/persistence/MemberRepositoryImpl.java`

- [ ] **Step 1: MemberRepository 인터페이스에 신규 메서드 선언 추가**

```java
// 추가할 메서드들
int findMaxPartnerCodeNumeric();
void saveBusinessLog(GodoMemberBusinessLogEntity log);
void savePartnerLog(MemberPartnerLogEntity log);
List<GodoMemberBusinessLogEntity> findBusinessLogsByMemNo(Long memNo);
List<MemberPartnerLogEntity> findPartnerLogsByMemNo(Long memNo);
List<Long> findOtherMemNosByBusinessNo(String businessNo, Long excludeMemNo);
void bulkUpdateDuplicateMemNos(List<Long> memNos, List<String> duplicateMemNos);
```

import도 추가한다.
```java
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessLogEntity;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerLogEntity;
```

- [ ] **Step 2: MemberRepositoryImpl — INSERT_PARTNER_SQL, UPDATE_PARTNER_SQL 수정**

`duplicate_check` → `member_status` 로 교체한다.

```java
private static final String INSERT_PARTNER_SQL =
    "INSERT INTO member_partner (mem_no,partner_registered_fl,partner_code,partner_name,tax_invoice_fl,member_status,created_at,updated_at) " +
    "VALUES (?,?,?,?,?,?,?,?)";

private static final String UPDATE_PARTNER_SQL =
    "UPDATE member_partner SET partner_registered_fl=?,partner_code=?,partner_name=?,tax_invoice_fl=?,member_status=?,updated_at=? WHERE mem_no=?";
```

- [ ] **Step 3: MemberRepositoryImpl — bulkInsertPartners / bulkUpdatePartners 파라미터 수정**

`bulkInsertPartners` 내 `setValues`:

```java
ps.setObject(p++, m.getMemNo());
ps.setObject(p++, m.getPartnerRegisteredFl());
ps.setObject(p++, m.getPartnerCode());
ps.setObject(p++, m.getPartnerName());
ps.setObject(p++, m.getTaxInvoiceFl());
ps.setObject(p++, m.getMemberStatus());   // duplicateCheck → memberStatus
ps.setObject(p++, m.getCreatedAt());
ps.setObject(p, m.getUpdatedAt());
```

`bulkUpdatePartners` 내 `setValues`:

```java
ps.setObject(p++, m.getPartnerRegisteredFl());
ps.setObject(p++, m.getPartnerCode());
ps.setObject(p++, m.getPartnerName());
ps.setObject(p++, m.getTaxInvoiceFl());
ps.setObject(p++, m.getMemberStatus());   // duplicateCheck → memberStatus
ps.setObject(p++, m.getUpdatedAt());
ps.setObject(p, m.getMemNo());
```

- [ ] **Step 4: MemberRepositoryImpl — 신규 필드 주입 및 신규 메서드 구현**

생성자에 신규 레포지토리 두 개를 추가한다.

```java
private final GodoMemberBusinessLogJpaRepository businessLogJpaRepository;
private final MemberPartnerLogJpaRepository partnerLogJpaRepository;

public MemberRepositoryImpl(
    GodoMemberJpaRepository godoMemberJpaRepository,
    GodoMemberBusinessJpaRepository godoMemberBusinessJpaRepository,
    MemberPartnerJpaRepository memberPartnerJpaRepository,
    GodoMemberBusinessLogJpaRepository businessLogJpaRepository,
    MemberPartnerLogJpaRepository partnerLogJpaRepository,
    EntityManager entityManager,
    HashUtil hashUtil,
    JdbcTemplate jdbcTemplate,
    AesGcmEncryptor encryptor
) {
    this.godoMemberJpaRepository = godoMemberJpaRepository;
    this.godoMemberBusinessJpaRepository = godoMemberBusinessJpaRepository;
    this.memberPartnerJpaRepository = memberPartnerJpaRepository;
    this.businessLogJpaRepository = businessLogJpaRepository;
    this.partnerLogJpaRepository = partnerLogJpaRepository;
    this.queryFactory = new JPAQueryFactory(entityManager);
    this.hashUtil = hashUtil;
    this.jdbcTemplate = jdbcTemplate;
    this.encryptor = encryptor;
}
```

신규 메서드 구현을 추가한다.

```java
@Override
public int findMaxPartnerCodeNumeric() {
    Integer max = jdbcTemplate.queryForObject(
        "SELECT COALESCE(MAX(CAST(partner_code AS INTEGER)), 0) FROM member_partner WHERE partner_code IS NOT NULL",
        Integer.class);
    return max == null ? 0 : max;
}

@Override
public void saveBusinessLog(GodoMemberBusinessLogEntity log) {
    businessLogJpaRepository.save(log);
}

@Override
public void savePartnerLog(MemberPartnerLogEntity log) {
    partnerLogJpaRepository.save(log);
}

@Override
public List<GodoMemberBusinessLogEntity> findBusinessLogsByMemNo(Long memNo) {
    return businessLogJpaRepository.findByMemNoOrderByLoggedAtDesc(memNo);
}

@Override
public List<MemberPartnerLogEntity> findPartnerLogsByMemNo(Long memNo) {
    return partnerLogJpaRepository.findByMemNoOrderByLoggedAtDesc(memNo);
}

@Override
public List<Long> findOtherMemNosByBusinessNo(String businessNo, Long excludeMemNo) {
    if (businessNo == null || businessNo.isBlank()) return List.of();
    return jdbcTemplate.queryForList(
        "SELECT mem_no FROM godo_member_business WHERE business_no = ? AND mem_no != ?",
        Long.class, businessNo, excludeMemNo);
}

@Override
public void bulkUpdateDuplicateMemNos(List<Long> memNos, List<String> duplicateMemNos) {
    if (memNos.isEmpty()) return;
    jdbcTemplate.batchUpdate(
        "UPDATE godo_member_business SET duplicate_mem_no = ? WHERE mem_no = ?",
        new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setObject(1, duplicateMemNos.get(i));
                ps.setObject(2, memNos.get(i));
            }
            @Override
            public int getBatchSize() { return memNos.size(); }
        });
}
```

- [ ] **Step 5: 빌드 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/sfood/automation/member/domain/port/MemberRepository.java \
        src/main/java/com/sfood/automation/member/infrastructure/persistence/MemberRepositoryImpl.java
git commit -m "feat: MemberRepository — partner SQL 갱신, 로그·중복검사·maxCode 메서드 추가"
```

---

## Task 5: UpdateMemberPartnerUseCase 수정 (로그 추가, duplicateCheck 제거)

**Files:**
- Modify: `src/main/java/com/sfood/automation/member/application/usecase/UpdateMemberPartnerUseCase.java`
- Modify: `src/test/java/com/sfood/automation/member/application/usecase/UpdateMemberPartnerUseCaseIntegrationTest.java`

- [ ] **Step 1: 기존 테스트의 duplicateCheck → memberStatus 교체 (컴파일 오류 먼저 해결)**

`UpdateMemberPartnerUseCaseIntegrationTest`에서 `duplicateCheck` → `memberStatus`로 교체한다.

```java
// 기존
new MemberPartnerUpdateRequest("y", "P-001", "신규명", "y", "확인완료", null)
response.duplicateCheck()

// 변경 후
new MemberPartnerUpdateRequest("y", "P-001", "신규명", "y", "NORMAL", null)
response.memberStatus()
```

모든 `MemberPartnerUpdateRequest` 생성자 호출과 `response.duplicateCheck()` 참조를 일괄 수정한다.

`should_insert_partner_record_when_not_exists` 검증:
```java
assertThat(response.memberStatus()).isEqualTo("NORMAL");
```

`should_update_existing_partner_record` 검증:
```java
assertThat(response.memberStatus()).isEqualTo("NORMAL");
```

- [ ] **Step 2: 테스트 실행 (수정 전 실패 확인)**

```bash
./gradlew test --tests "com.sfood.automation.member.application.usecase.UpdateMemberPartnerUseCaseIntegrationTest" 2>&1 | tail -30
```
Expected: FAIL (컴파일 오류 → UseCase가 아직 수정 안됨)

- [ ] **Step 3: UpdateMemberPartnerUseCase 수정**

```java
package com.sfood.automation.member.application.usecase;

import com.sfood.automation.common.exception.DuplicatePartnerCodeException;
import com.sfood.automation.common.exception.MemberNotFoundException;
import com.sfood.automation.member.application.dto.MemberPartnerUpdateRequest;
import com.sfood.automation.member.application.dto.MemberPartnerUpdateResponse;
import com.sfood.automation.member.domain.port.MemberRepository;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerEntity;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerLogEntity;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateMemberPartnerUseCase {

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MemberRepository memberRepository;

    public UpdateMemberPartnerUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public MemberPartnerUpdateResponse execute(Long memNo, MemberPartnerUpdateRequest request,
                                               String loggedBy) {
        memberRepository.findByMemNo(memNo).orElseThrow(MemberNotFoundException::new);

        if (request.partnerCode() != null
                && memberRepository.existsActivePartnerCode(request.partnerCode(), memNo)) {
            throw new DuplicatePartnerCodeException();
        }

        String now = LocalDateTime.now().format(DT_FMT);

        // 기존 데이터 로그 저장
        memberRepository.findPartnerByMemNo(memNo).ifPresent(existing -> {
            MemberPartnerLogEntity log = new MemberPartnerLogEntity();
            log.setMemNo(existing.getMemNo());
            log.setPartnerRegisteredFl(existing.getPartnerRegisteredFl());
            log.setPartnerCode(existing.getPartnerCode());
            log.setPartnerName(existing.getPartnerName());
            log.setTaxInvoiceFl(existing.getTaxInvoiceFl());
            log.setMemberStatus(existing.getMemberStatus());
            log.setLoggedAt(now);
            log.setLoggedBy(loggedBy);
            log.setCreatedAt(now);
            memberRepository.savePartnerLog(log);
        });

        MemberPartnerEntity partner = memberRepository.findPartnerByMemNo(memNo)
            .orElseGet(() -> {
                MemberPartnerEntity created = new MemberPartnerEntity();
                created.setMemNo(memNo);
                created.setCreatedAt(now);
                return created;
            });

        partner.setPartnerRegisteredFl(request.partnerRegisteredFl() != null ? request.partnerRegisteredFl() : "n");
        partner.setPartnerCode(request.partnerCode());
        partner.setPartnerName(request.partnerName());
        partner.setTaxInvoiceFl(request.taxInvoiceFl() != null ? request.taxInvoiceFl() : "n");
        partner.setMemberStatus(request.memberStatus() != null ? request.memberStatus() : "NORMAL");
        partner.setUpdatedAt(now);
        memberRepository.savePartner(partner);

        memberRepository.findBusinessByMemNo(memNo).ifPresent(biz -> {
            biz.setBizLicenseFile(request.bizLicenseFile());
            biz.setUpdatedAt(now);
            memberRepository.saveBusiness(biz);
        });

        return new MemberPartnerUpdateResponse(
            partner.getPartnerRegisteredFl(),
            partner.getPartnerCode(),
            partner.getPartnerName(),
            partner.getTaxInvoiceFl(),
            partner.getMemberStatus(),
            request.bizLicenseFile(),
            now
        );
    }
}
```

> **주의:** `execute()`의 시그니처가 `loggedBy` 파라미터를 추가로 받는다. Controller에서 세션 이메일을 전달한다 (Task 11에서 처리).

- [ ] **Step 4: 테스트 내 execute() 호출에 loggedBy 추가**

```java
// 테스트에서는 null 전달 (세션 없음)
updateMemberPartnerUseCase.execute(900200L,
    new MemberPartnerUpdateRequest(...), null)
```

모든 `updateMemberPartnerUseCase.execute(memNo, request)` → `updateMemberPartnerUseCase.execute(memNo, request, null)` 로 교체한다.

- [ ] **Step 5: 로그 저장 검증 테스트 추가**

`UpdateMemberPartnerUseCaseIntegrationTest`에 주입 추가 및 테스트 추가:

```java
@Autowired
private MemberPartnerLogJpaRepository partnerLogJpaRepository;

@AfterEach
void tearDown() {
    partnerLogJpaRepository.deleteAll(); // 기존 tearDown에 추가
    memberPartnerJpaRepository.deleteAll();
    godoMemberBusinessJpaRepository.deleteAll();
    godoMemberJpaRepository.deleteAll();
}

@Test
void should_save_partner_log_before_update() {
    // 1차 저장
    updateMemberPartnerUseCase.execute(900101L,
        new MemberPartnerUpdateRequest("y", "P-001", "초기명", "n", "NORMAL", null), "test@s-food.com");

    // 2차 수정 → 1차 데이터가 로그에 남아야 함
    updateMemberPartnerUseCase.execute(900101L,
        new MemberPartnerUpdateRequest("y", "P-002", "수정명", "y", "INACTIVE", null), "test@s-food.com");

    List<MemberPartnerLogEntity> logs = partnerLogJpaRepository.findByMemNoOrderByLoggedAtDesc(900101L);
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).getPartnerCode()).isEqualTo("P-001");
    assertThat(logs.get(0).getPartnerName()).isEqualTo("초기명");
    assertThat(logs.get(0).getMemberStatus()).isEqualTo("NORMAL");
    assertThat(logs.get(0).getLoggedBy()).isEqualTo("test@s-food.com");
}

@Test
void should_not_save_log_on_first_insert() {
    updateMemberPartnerUseCase.execute(900200L,
        new MemberPartnerUpdateRequest("y", "P-001", "신규명", "y", "NORMAL", null), null);

    assertThat(partnerLogJpaRepository.findByMemNoOrderByLoggedAtDesc(900200L)).isEmpty();
}
```

- [ ] **Step 6: 테스트 실행**

```bash
./gradlew test --tests "com.sfood.automation.member.application.usecase.UpdateMemberPartnerUseCaseIntegrationTest"
```
Expected: PASS

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/sfood/automation/member/application/usecase/UpdateMemberPartnerUseCase.java \
        src/test/java/com/sfood/automation/member/application/usecase/UpdateMemberPartnerUseCaseIntegrationTest.java
git commit -m "feat: UpdateMemberPartnerUseCase — 거래처 수정 전 로그 저장, memberStatus 관리"
```

---

## Task 6: UpdateMemberBusinessUseCase 신규 (사업자 정보 수정 + 로그)

**Files:**
- Create: `src/main/java/com/sfood/automation/member/application/usecase/UpdateMemberBusinessUseCase.java`
- Create: `src/test/java/com/sfood/automation/member/application/usecase/UpdateMemberBusinessUseCaseIntegrationTest.java`

- [ ] **Step 1: 실패 테스트 작성**

```java
package com.sfood.automation.member.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.common.exception.MemberNotFoundException;
import com.sfood.automation.member.application.dto.MemberBusinessUpdateRequest;
import com.sfood.automation.member.application.dto.MemberUploadRequest;
import com.sfood.automation.member.application.upload.MemberRowFixture;
import com.sfood.automation.member.domain.enums.MemberUploadType;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessLogJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerJpaRepository;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class UpdateMemberBusinessUseCaseIntegrationTest {

    @Autowired private UploadMembersUseCase uploadMembersUseCase;
    @Autowired private UpdateMemberBusinessUseCase updateMemberBusinessUseCase;
    @Autowired private GodoMemberJpaRepository godoMemberJpaRepository;
    @Autowired private GodoMemberBusinessJpaRepository godoMemberBusinessJpaRepository;
    @Autowired private GodoMemberBusinessLogJpaRepository businessLogJpaRepository;
    @Autowired private MemberPartnerJpaRepository memberPartnerJpaRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @BeforeEach
    void setUp() {
        uploadMembersUseCase.execute(new MemberUploadRequest(
            MemberUploadType.LIST, "list.xlsx", false, List.of(MemberRowFixture.listRow())));
    }

    @AfterEach
    void tearDown() {
        businessLogJpaRepository.deleteAll();
        memberPartnerJpaRepository.deleteAll();
        godoMemberBusinessJpaRepository.deleteAll();
        godoMemberJpaRepository.deleteAll();
    }

    @Test
    void should_update_business_info_and_save_log() {
        MemberBusinessUpdateRequest request = new MemberBusinessUpdateRequest(
            "수정상호", "222-33-44444", "수정대표", "도매", "식품류", "서울시 수정구", null);

        updateMemberBusinessUseCase.execute(900101L, request, "admin@s-food.com");

        // 사업자 정보 업데이트 확인
        var biz = godoMemberBusinessJpaRepository.findByMemNo(900101L).orElseThrow();
        assertThat(biz.getCompany()).isEqualTo("수정상호");
        assertThat(biz.getBusinessNo()).isEqualTo("222-33-44444");
        assertThat(biz.getCeoName()).isEqualTo("수정대표");

        // 로그 저장 확인
        var logs = businessLogJpaRepository.findByMemNoOrderByLoggedAtDesc(900101L);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getCompany()).isEqualTo("(주)테스트"); // 수정 전 원래 값
        assertThat(logs.get(0).getLoggedBy()).isEqualTo("admin@s-food.com");
    }

    @Test
    void should_throw_404_for_unknown_member() {
        assertThatThrownBy(() -> updateMemberBusinessUseCase.execute(99999L,
            new MemberBusinessUpdateRequest(null, null, null, null, null, null, null), null))
            .isInstanceOf(MemberNotFoundException.class);
    }
}
```

- [ ] **Step 2: 테스트 실행 (실패 확인)**

```bash
./gradlew test --tests "com.sfood.automation.member.application.usecase.UpdateMemberBusinessUseCaseIntegrationTest" 2>&1 | tail -20
```
Expected: FAIL (UpdateMemberBusinessUseCase 미존재)

- [ ] **Step 3: UpdateMemberBusinessUseCase 구현**

```java
package com.sfood.automation.member.application.usecase;

import com.sfood.automation.common.exception.MemberNotFoundException;
import com.sfood.automation.member.application.dto.MemberBusinessUpdateRequest;
import com.sfood.automation.member.domain.port.MemberRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessEntity;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessLogEntity;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateMemberBusinessUseCase {

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MemberRepository memberRepository;

    public UpdateMemberBusinessUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public void execute(Long memNo, MemberBusinessUpdateRequest request, String loggedBy) {
        memberRepository.findByMemNo(memNo).orElseThrow(MemberNotFoundException::new);

        String now = LocalDateTime.now().format(DT_FMT);

        GodoMemberBusinessEntity biz = memberRepository.findBusinessByMemNo(memNo)
            .orElseGet(() -> {
                GodoMemberBusinessEntity created = new GodoMemberBusinessEntity();
                created.setMemNo(memNo);
                created.setCreatedAt(now);
                return created;
            });

        // 변경 전 스냅샷 로그 저장 (기존 레코드가 있을 때만)
        if (biz.getId() != null) {
            GodoMemberBusinessLogEntity log = new GodoMemberBusinessLogEntity();
            log.setMemNo(biz.getMemNo());
            log.setCompany(biz.getCompany());
            log.setBusinessNo(biz.getBusinessNo());
            log.setCeoName(biz.getCeoName());
            log.setService(biz.getService());
            log.setItem(biz.getItem());
            log.setComAddress(biz.getComAddress());
            log.setBizLicenseFile(biz.getBizLicenseFile());
            log.setLoggedAt(now);
            log.setLoggedBy(loggedBy);
            log.setCreatedAt(now);
            memberRepository.saveBusinessLog(log);
        }

        biz.setCompany(request.company());
        biz.setBusinessNo(request.businessNo());
        biz.setCeoName(request.ceoName());
        biz.setService(request.service());
        biz.setItem(request.item());
        biz.setComAddress(request.comAddress());
        if (request.bizLicenseFile() != null) {
            biz.setBizLicenseFile(request.bizLicenseFile());
        }
        biz.setUpdatedAt(now);
        memberRepository.saveBusiness(biz);
    }
}
```

- [ ] **Step 4: 테스트 실행**

```bash
./gradlew test --tests "com.sfood.automation.member.application.usecase.UpdateMemberBusinessUseCaseIntegrationTest"
```
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/sfood/automation/member/application/usecase/UpdateMemberBusinessUseCase.java \
        src/test/java/com/sfood/automation/member/application/usecase/UpdateMemberBusinessUseCaseIntegrationTest.java
git commit -m "feat: UpdateMemberBusinessUseCase — 사업자 정보 수정 + 변경 전 로그 저장"
```

---

## Task 7: 이력 조회 UseCase 2개 신규

**Files:**
- Create: `src/main/java/com/sfood/automation/member/application/usecase/GetMemberBusinessLogsUseCase.java`
- Create: `src/main/java/com/sfood/automation/member/application/usecase/GetMemberPartnerLogsUseCase.java`
- Create: `src/test/java/com/sfood/automation/member/application/usecase/GetMemberLogsUseCaseIntegrationTest.java`

- [ ] **Step 1: 실패 테스트 작성**

```java
package com.sfood.automation.member.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.member.application.dto.MemberBusinessLogListResponse;
import com.sfood.automation.member.application.dto.MemberBusinessUpdateRequest;
import com.sfood.automation.member.application.dto.MemberPartnerLogListResponse;
import com.sfood.automation.member.application.dto.MemberPartnerUpdateRequest;
import com.sfood.automation.member.application.dto.MemberUploadRequest;
import com.sfood.automation.member.application.upload.MemberRowFixture;
import com.sfood.automation.member.domain.enums.MemberUploadType;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessLogJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerLogJpaRepository;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class GetMemberLogsUseCaseIntegrationTest {

    @Autowired private UploadMembersUseCase uploadMembersUseCase;
    @Autowired private UpdateMemberBusinessUseCase updateMemberBusinessUseCase;
    @Autowired private UpdateMemberPartnerUseCase updateMemberPartnerUseCase;
    @Autowired private GetMemberBusinessLogsUseCase getMemberBusinessLogsUseCase;
    @Autowired private GetMemberPartnerLogsUseCase getMemberPartnerLogsUseCase;
    @Autowired private GodoMemberJpaRepository godoMemberJpaRepository;
    @Autowired private GodoMemberBusinessJpaRepository godoMemberBusinessJpaRepository;
    @Autowired private GodoMemberBusinessLogJpaRepository businessLogJpaRepository;
    @Autowired private MemberPartnerJpaRepository memberPartnerJpaRepository;
    @Autowired private MemberPartnerLogJpaRepository partnerLogJpaRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @BeforeEach
    void setUp() {
        uploadMembersUseCase.execute(new MemberUploadRequest(
            MemberUploadType.LIST, "list.xlsx", false, List.of(MemberRowFixture.listRow())));
    }

    @AfterEach
    void tearDown() {
        businessLogJpaRepository.deleteAll();
        partnerLogJpaRepository.deleteAll();
        memberPartnerJpaRepository.deleteAll();
        godoMemberBusinessJpaRepository.deleteAll();
        godoMemberJpaRepository.deleteAll();
    }

    @Test
    void should_return_business_logs_in_descending_order() {
        updateMemberBusinessUseCase.execute(900101L,
            new MemberBusinessUpdateRequest("1차수정", null, null, null, null, null, null), "admin@s-food.com");
        updateMemberBusinessUseCase.execute(900101L,
            new MemberBusinessUpdateRequest("2차수정", null, null, null, null, null, null), "admin@s-food.com");

        MemberBusinessLogListResponse response = getMemberBusinessLogsUseCase.execute(900101L);

        assertThat(response.logs()).hasSize(2);
        assertThat(response.logs().get(0).company()).isEqualTo("1차수정"); // 최신순 → 2차 직전 = 1차수정 상태
    }

    @Test
    void should_return_partner_logs_in_descending_order() {
        updateMemberPartnerUseCase.execute(900101L,
            new MemberPartnerUpdateRequest("y", "P-001", "1차", "n", "NORMAL", null), "admin@s-food.com");
        updateMemberPartnerUseCase.execute(900101L,
            new MemberPartnerUpdateRequest("y", "P-002", "2차", "y", "INACTIVE", null), "admin@s-food.com");

        MemberPartnerLogListResponse response = getMemberPartnerLogsUseCase.execute(900101L);

        assertThat(response.logs()).hasSize(1);
        assertThat(response.logs().get(0).partnerCode()).isEqualTo("P-001");
    }

    @Test
    void should_return_empty_when_no_logs() {
        MemberBusinessLogListResponse response = getMemberBusinessLogsUseCase.execute(900101L);
        assertThat(response.logs()).isEmpty();
    }
}
```

- [ ] **Step 2: 테스트 실행 (실패 확인)**

```bash
./gradlew test --tests "com.sfood.automation.member.application.usecase.GetMemberLogsUseCaseIntegrationTest" 2>&1 | tail -20
```
Expected: FAIL

- [ ] **Step 3: GetMemberBusinessLogsUseCase 구현**

```java
package com.sfood.automation.member.application.usecase;

import com.sfood.automation.member.application.dto.MemberBusinessLogListResponse;
import com.sfood.automation.member.domain.port.MemberRepository;
import org.springframework.stereotype.Service;

@Service
public class GetMemberBusinessLogsUseCase {

    private final MemberRepository memberRepository;

    public GetMemberBusinessLogsUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public MemberBusinessLogListResponse execute(Long memNo) {
        return MemberBusinessLogListResponse.from(memberRepository.findBusinessLogsByMemNo(memNo));
    }
}
```

- [ ] **Step 4: GetMemberPartnerLogsUseCase 구현**

```java
package com.sfood.automation.member.application.usecase;

import com.sfood.automation.member.application.dto.MemberPartnerLogListResponse;
import com.sfood.automation.member.domain.port.MemberRepository;
import org.springframework.stereotype.Service;

@Service
public class GetMemberPartnerLogsUseCase {

    private final MemberRepository memberRepository;

    public GetMemberPartnerLogsUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public MemberPartnerLogListResponse execute(Long memNo) {
        return MemberPartnerLogListResponse.from(memberRepository.findPartnerLogsByMemNo(memNo));
    }
}
```

- [ ] **Step 5: 테스트 실행**

```bash
./gradlew test --tests "com.sfood.automation.member.application.usecase.GetMemberLogsUseCaseIntegrationTest"
```
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/sfood/automation/member/application/usecase/GetMemberBusinessLogsUseCase.java \
        src/main/java/com/sfood/automation/member/application/usecase/GetMemberPartnerLogsUseCase.java \
        src/test/java/com/sfood/automation/member/application/usecase/GetMemberLogsUseCaseIntegrationTest.java
git commit -m "feat: GetMemberBusinessLogsUseCase, GetMemberPartnerLogsUseCase — 이력 조회 UseCase 신규"
```

---

## Task 8: FetchNewMembersUseCase 수정 (#8/#9 통합, 자동 거래처 생성, 중복 검사)

**Files:**
- Modify: `src/main/java/com/sfood/automation/member/application/usecase/FetchNewMembersUseCase.java`
- Create: `src/test/java/com/sfood/automation/member/application/usecase/FetchNewMembersUseCaseIntegrationTest.java`

- [ ] **Step 1: 실패 테스트 작성**

```java
package com.sfood.automation.member.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.member.application.dto.CrawledMember;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerJpaRepository;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class FetchNewMembersUseCaseIntegrationTest {

    @Autowired private FetchNewMembersUseCase fetchNewMembersUseCase;
    @Autowired private GodoMemberJpaRepository godoMemberJpaRepository;
    @Autowired private GodoMemberBusinessJpaRepository godoMemberBusinessJpaRepository;
    @Autowired private MemberPartnerJpaRepository memberPartnerJpaRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @AfterEach
    void tearDown() {
        memberPartnerJpaRepository.deleteAll();
        godoMemberBusinessJpaRepository.deleteAll();
        godoMemberJpaRepository.deleteAll();
    }

    private CrawledMember generalMemberWithBiz(long memNo, String memId, String company, String bizNo) {
        // CrawledMember 필드 순서: memNo, memId, groupSno, memberFl, appFl, appDt,
        // entryDt, entryPath, lastLoginDt, memName, nickName, email, cellPhone, phone,
        // address, sexFl, birthDt, marriFl, marriDate, maillingFl, smsFl,
        // job, interest, recommId, saleAmount, mileage, deposit, loginCount,
        // fax, expirationFl, memo, ex1, ex2, ex3,
        // company, businessNo, ceoName, service, item, comAddress
        return new CrawledMember(
            memNo, memId, 1, "personal", "n", null,
            null, null, null,
            "홍길동", null, "hong@test.com", "010-1111-2222", null, null,
            null, null, null, null, "y", "y",
            null, null, null, 0.0, 0.0, 0.0, 0,
            null, 1, null, null, null, null,
            company, bizNo, "대표", "도매", "식품", "서울시 강남구"
        );
    }

    @Test
    void should_save_biz_info_for_general_member_when_biz_fields_present() {
        // groupSno=1(일반회원)이지만 사업자 정보가 있으면 저장해야 함 (#9)
        fetchNewMembersUseCase.execute(List.of(
            generalMemberWithBiz(900101L, "user1", "(주)테스트", "123-45-67890")));

        assertThat(godoMemberBusinessJpaRepository.findByMemNo(900101L)).isPresent();
    }

    @Test
    void should_auto_create_partner_with_code_and_name() {
        // 거래처 자동 생성 (#2)
        fetchNewMembersUseCase.execute(List.of(
            generalMemberWithBiz(900101L, "user1", "달콤가든", "111-11-11111")));

        var partner = memberPartnerJpaRepository.findByMemNo(900101L).orElseThrow();
        assertThat(partner.getPartnerName()).isEqualTo("(에쓰푸드몰)달콤가든");
        assertThat(partner.getPartnerCode()).isNotNull();
        assertThat(partner.getMemberStatus()).isEqualTo("NORMAL");
    }

    @Test
    void should_auto_create_partner_code_max_plus_one() {
        // MAX+1 거래처코드 (#2)
        fetchNewMembersUseCase.execute(List.of(
            generalMemberWithBiz(900101L, "user1", "가게A", "111-11-11111")));
        int firstCode = Integer.parseInt(
            memberPartnerJpaRepository.findByMemNo(900101L).orElseThrow().getPartnerCode());

        List<Object> row2 = new java.util.ArrayList<>();
        fetchNewMembersUseCase.execute(List.of(
            generalMemberWithBiz(900102L, "user2", "가게B", "222-22-22222")));
        int secondCode = Integer.parseInt(
            memberPartnerJpaRepository.findByMemNo(900102L).orElseThrow().getPartnerCode());

        assertThat(secondCode).isEqualTo(firstCode + 1);
    }

    @Test
    void should_set_duplicate_mem_no_when_same_biz_no_exists() {
        // 중복 거래처 검사 (#7)
        fetchNewMembersUseCase.execute(List.of(
            generalMemberWithBiz(900101L, "user1", "가게A", "999-99-99999"),
            generalMemberWithBiz(900102L, "user2", "가게B", "999-99-99999")));

        var biz1 = godoMemberBusinessJpaRepository.findByMemNo(900101L).orElseThrow();
        var biz2 = godoMemberBusinessJpaRepository.findByMemNo(900102L).orElseThrow();

        assertThat(biz1.getDuplicateMemNo()).isEqualTo("900102");
        assertThat(biz2.getDuplicateMemNo()).isEqualTo("900101");
    }

    @Test
    void should_not_create_partner_if_already_exists() {
        // 이미 거래처 있으면 재생성 안 함
        fetchNewMembersUseCase.execute(List.of(
            generalMemberWithBiz(900101L, "user1", "달콤가든", "111-11-11111")));
        fetchNewMembersUseCase.execute(List.of(
            generalMemberWithBiz(900101L, "user1", "달콤가든(수정)", "111-11-11111")));

        assertThat(memberPartnerJpaRepository.findAll()).hasSize(1);
    }
}
```

> `CrawledMember`가 record 타입이면 생성자 파라미터가 필드 순서와 일치해야 한다. 실제 CrawledMember 필드 순서를 확인 후 맞춰서 작성한다.

- [ ] **Step 2: 테스트 실행 (실패 확인)**

```bash
./gradlew test --tests "com.sfood.automation.member.application.usecase.FetchNewMembersUseCaseIntegrationTest" 2>&1 | tail -20
```
Expected: FAIL

- [ ] **Step 3: FetchNewMembersUseCase 수정**

`hasBusiness()` 조건 제거, 자동 거래처 생성, 중복 거래처 검사를 추가한다. 기존 파일의 `execute()` 메서드를 아래로 교체한다.

```java
@Transactional
public String execute(List<CrawledMember> crawledMembers) {
    if (crawledMembers.isEmpty()) {
        return "신규 0건, 업데이트 0건";
    }

    String now = LocalDateTime.now().format(DT_FMT);
    List<Long> allMemNos = crawledMembers.stream().map(CrawledMember::memNo).toList();
    Set<Long> existingNos = new HashSet<>(memberRepository.findExistingMemNos(allMemNos));

    List<GodoMemberEntity> toInsert = crawledMembers.stream()
        .filter(m -> !existingNos.contains(m.memNo()))
        .map(m -> toEntity(m, now, true))
        .toList();

    List<GodoMemberEntity> toUpdate = crawledMembers.stream()
        .filter(m -> existingNos.contains(m.memNo()))
        .map(m -> toEntity(m, now, false))
        .toList();

    if (!toInsert.isEmpty()) memberRepository.bulkInsertMembers(toInsert);
    if (!toUpdate.isEmpty()) memberRepository.bulkUpdateMembers(toUpdate);

    // 사업자 정보: hasBusiness() 조건 제거 → 사업자 필드 유무로 판단
    List<CrawledMember> bizMembers = crawledMembers.stream()
        .filter(m -> m.company() != null || m.businessNo() != null)
        .toList();

    if (!bizMembers.isEmpty()) {
        List<Long> bizNos = bizMembers.stream().map(CrawledMember::memNo).toList();
        Set<Long> existingBizNos = new HashSet<>(memberRepository.findExistingBusinessNos(bizNos));

        List<GodoMemberBusinessEntity> bizInsert = bizMembers.stream()
            .filter(m -> !existingBizNos.contains(m.memNo()))
            .map(m -> toBizEntity(m, now, true))
            .toList();
        List<GodoMemberBusinessEntity> bizUpdate = bizMembers.stream()
            .filter(m -> existingBizNos.contains(m.memNo()))
            .map(m -> toBizEntity(m, now, false))
            .toList();

        if (!bizInsert.isEmpty()) memberRepository.bulkInsertBusinesses(bizInsert);
        if (!bizUpdate.isEmpty()) memberRepository.bulkUpdateBusinesses(bizUpdate, MemberUploadType.LIST);

        // 중복 사업자번호 검사
        checkDuplicateBusinessNo(bizMembers);
    }

    // 거래처 자동 생성 (신규 회원만, 이미 있으면 스킵)
    autoCreatePartners(toInsert, crawledMembers, now);

    return String.format("신규 %d건, 업데이트 %d건", toInsert.size(), toUpdate.size());
}

private void checkDuplicateBusinessNo(List<CrawledMember> bizMembers) {
    String now = LocalDateTime.now().format(DT_FMT);
    List<Long> memNos = new java.util.ArrayList<>();
    List<String> duplicateMemNos = new java.util.ArrayList<>();

    for (CrawledMember m : bizMembers) {
        if (m.businessNo() == null || m.businessNo().isBlank()) {
            continue;
        }
        List<Long> others = memberRepository.findOtherMemNosByBusinessNo(m.businessNo(), m.memNo());
        String duplicateValue = others.isEmpty() ? null
            : others.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(";"));
        memNos.add(m.memNo());
        duplicateMemNos.add(duplicateValue);
    }

    if (!memNos.isEmpty()) {
        memberRepository.bulkUpdateDuplicateMemNos(memNos, duplicateMemNos);
    }
}

private void autoCreatePartners(List<GodoMemberEntity> newMembers,
                                 List<CrawledMember> allCrawled, String now) {
    if (newMembers.isEmpty()) return;

    Set<Long> newMemNos = newMembers.stream()
        .map(GodoMemberEntity::getMemNo).collect(java.util.stream.Collectors.toSet());
    Set<Long> existingPartnerNos = new HashSet<>(
        memberRepository.findExistingPartnerNos(new java.util.ArrayList<>(newMemNos)));

    Map<Long, CrawledMember> crawledByMemNo = allCrawled.stream()
        .collect(java.util.stream.Collectors.toMap(CrawledMember::memNo, m -> m));

    int baseCode = memberRepository.findMaxPartnerCodeNumeric();
    List<MemberPartnerEntity> toInsert = new java.util.ArrayList<>();
    for (GodoMemberEntity member : newMembers) {
        if (existingPartnerNos.contains(member.getMemNo())) continue;

        String partnerCode = String.valueOf(baseCode + 1 + toInsert.size());

        CrawledMember crawled = crawledByMemNo.get(member.getMemNo());
        String company = crawled != null ? crawled.company() : null;
        String partnerName = "(에쓰푸드몰)" + (company != null ? company : "");

        MemberPartnerEntity partner = new MemberPartnerEntity();
        partner.setMemNo(member.getMemNo());
        partner.setPartnerRegisteredFl("n");
        partner.setPartnerCode(partnerCode);
        partner.setPartnerName(partnerName);
        partner.setTaxInvoiceFl("n");
        partner.setMemberStatus("NORMAL");
        partner.setCreatedAt(now);
        partner.setUpdatedAt(now);
        toInsert.add(partner);
    }

    if (!toInsert.isEmpty()) {
        memberRepository.bulkInsertPartners(toInsert);
    }
}
```

import 추가:
```java
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerEntity;
import java.util.Map;
```

- [ ] **Step 4: 테스트 실행**

```bash
./gradlew test --tests "com.sfood.automation.member.application.usecase.FetchNewMembersUseCaseIntegrationTest"
```
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/sfood/automation/member/application/usecase/FetchNewMembersUseCase.java \
        src/test/java/com/sfood/automation/member/application/usecase/FetchNewMembersUseCaseIntegrationTest.java
git commit -m "feat: FetchNewMembersUseCase — hasBusiness 제거, 자동 거래처 생성, 중복 사업자번호 검사"
```

---

## Task 9: UploadMembersUseCase 수정 (#9, 자동 거래처 생성)

**Files:**
- Modify: `src/main/java/com/sfood/automation/member/application/usecase/UploadMembersUseCase.java`

- [ ] **Step 1: hasBusiness() 조건 제거 및 자동 거래처 생성 로직 추가**

`UploadMembersUseCase.execute()` 내 엔티티 구성 루프(Step 3)를 수정한다.

기존 사업자 정보 처리 블록:
```java
// 기존
if (MemberGroup.of(member.getGroupSno()).hasBusiness()) {
    GodoMemberBusinessEntity biz = new GodoMemberBusinessEntity();
    ...
}
```

수정 후:
```java
// 사업자 정보: hasBusiness() 조건 제거 → 사업자 정보 필드 유무로 판단
GodoMemberBusinessEntity biz = new GodoMemberBusinessEntity();
biz.setMemNo(memNo);
biz.setCreatedAt(now);
normalizer.applyBusinessFields(biz, row, request.type());
biz.setUpdatedAt(now);
if (biz.getCompany() != null || biz.getBusinessNo() != null) {
    if (existingBizNos.contains(memNo)) bizToUpdate.add(biz);
    else bizToInsert.add(biz);
}
```

거래처 처리 블록 — TEMPLATE 분기를 제거하고 모든 타입에서 처리하되, TEMPLATE은 파일에서 읽고 그 외는 자동 생성한다:
```java
if (request.type() == MemberUploadType.TEMPLATE) {
    MemberPartnerEntity partner = new MemberPartnerEntity();
    partner.setMemNo(memNo);
    partner.setCreatedAt(now);
    normalizer.applyPartnerFields(partner, row);
    partner.setUpdatedAt(now);
    if (existingPartnerNos.contains(memNo)) partnersToUpdate.add(partner);
    else partnersToInsert.add(partner);
} else {
    // LIST / 기타 타입: 신규 회원만 자동 생성 (기존에 없으면)
    if (!existingMemberNos.contains(memNo) && !existingPartnerNos.contains(memNo)) {
        String company = normalizer.extractCompany(row, request.type());
        int maxCode = memberRepository.findMaxPartnerCodeNumeric();
        MemberPartnerEntity partner = new MemberPartnerEntity();
        partner.setMemNo(memNo);
        partner.setPartnerRegisteredFl("n");
        partner.setPartnerCode(String.valueOf(maxCode + 1 + partnersToInsert.size()));
        partner.setPartnerName("(에쓰푸드몰)" + (company != null ? company : ""));
        partner.setTaxInvoiceFl("n");
        partner.setMemberStatus("NORMAL");
        partner.setCreatedAt(now);
        partner.setUpdatedAt(now);
        partnersToInsert.add(partner);
    }
}
```

- [ ] **Step 2: MemberRowNormalizer에 extractCompany 메서드 추가**

`MemberRowNormalizer`에 추가한다.

```java
public String extractCompany(List<Object> row, MemberUploadType type) {
    return normalizeString(cell(row, 21, type), null);
}
```

- [ ] **Step 3: existingPartnerNos 조회를 TEMPLATE 조건 없이 항상 수행하도록 수정**

```java
// 기존
Set<Long> existingPartnerNos = request.type() == MemberUploadType.TEMPLATE
    ? new HashSet<>(memberRepository.findExistingPartnerNos(allMemNos))
    : Set.of();

// 수정 후
Set<Long> existingPartnerNos = new HashSet<>(memberRepository.findExistingPartnerNos(allMemNos));
```

- [ ] **Step 4: 빌드 + 기존 테스트 실행**

```bash
./gradlew test --tests "com.sfood.automation.member.application.usecase.UploadMembersUseCaseIntegrationTest"
```
Expected: PASS (기존 테스트 통과)

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/sfood/automation/member/application/usecase/UploadMembersUseCase.java \
        src/main/java/com/sfood/automation/member/application/upload/MemberRowNormalizer.java
git commit -m "feat: UploadMembersUseCase — hasBusiness 제거, 자동 거래처 생성 적용"
```

---

## Task 10: CommonCodeUseCase + CommonCodeController (공통코드 CRUD)

**Files:**
- Create: `src/main/java/com/sfood/automation/common_code/application/usecase/CommonCodeUseCase.java`
- Create: `src/main/java/com/sfood/automation/common_code/presentation/CommonCodeController.java`
- Create: `src/test/java/com/sfood/automation/common_code/presentation/CommonCodeControllerTest.java`

- [ ] **Step 1: 실패 테스트 작성**

```java
package com.sfood.automation.common_code.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.common.config.WebMvcConfig;
import com.sfood.automation.common.exception.GlobalExceptionHandler;
import com.sfood.automation.common.security.SessionAuthInterceptor;
import com.sfood.automation.common_code.application.dto.CommonCodeListResponse;
import com.sfood.automation.common_code.application.dto.CommonCodeListResponse.CodeItem;
import com.sfood.automation.common_code.application.dto.CommonCodeRequest;
import com.sfood.automation.common_code.application.usecase.CommonCodeUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CommonCodeController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, SessionAuthInterceptor.class})
class CommonCodeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CommonCodeUseCase commonCodeUseCase;

    private MockHttpSession session() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        return s;
    }

    @Test
    void should_return_200_with_code_list() throws Exception {
        when(commonCodeUseCase.findAll(null)).thenReturn(new CommonCodeListResponse(List.of(
            new CodeItem(1, "SERVICE", "업태", "001", "도소매", null, "Y", "2026-07-01", "2026-07-01"))));

        mockMvc.perform(get("/api/common-codes").session(session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codes[0].groupCode").value("SERVICE"));
    }

    @Test
    void should_return_201_on_create() throws Exception {
        doNothing().when(commonCodeUseCase).create(any(CommonCodeRequest.class));

        mockMvc.perform(post("/api/common-codes")
                .session(session())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "groupCode", "SERVICE", "groupName", "업태",
                    "code", "001", "codeName", "도소매", "useYn", "Y"))))
            .andExpect(status().isCreated());
    }

    @Test
    void should_return_200_on_update() throws Exception {
        doNothing().when(commonCodeUseCase).update(eq(1), any(CommonCodeRequest.class));

        mockMvc.perform(put("/api/common-codes/1")
                .session(session())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "groupCode", "SERVICE", "groupName", "업태",
                    "code", "001", "codeName", "소매", "useYn", "Y"))))
            .andExpect(status().isOk());
    }

    @Test
    void should_return_401_without_session() throws Exception {
        mockMvc.perform(get("/api/common-codes"))
            .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: 테스트 실행 (실패 확인)**

```bash
./gradlew test --tests "com.sfood.automation.common_code.presentation.CommonCodeControllerTest" 2>&1 | tail -20
```
Expected: FAIL

- [ ] **Step 3: CommonCodeUseCase 구현**

```java
package com.sfood.automation.common_code.application.usecase;

import com.sfood.automation.common_code.application.dto.CommonCodeListResponse;
import com.sfood.automation.common_code.application.dto.CommonCodeRequest;
import com.sfood.automation.common_code.infrastructure.persistence.CommonCodeEntity;
import com.sfood.automation.common_code.infrastructure.persistence.CommonCodeJpaRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommonCodeUseCase {

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CommonCodeJpaRepository commonCodeJpaRepository;

    public CommonCodeUseCase(CommonCodeJpaRepository commonCodeJpaRepository) {
        this.commonCodeJpaRepository = commonCodeJpaRepository;
    }

    public CommonCodeListResponse findAll(String groupCode) {
        List<CommonCodeEntity> entities = groupCode != null
            ? commonCodeJpaRepository.findByGroupCodeOrderByCodeAsc(groupCode)
            : commonCodeJpaRepository.findAllByOrderByGroupCodeAscCodeAsc();
        return CommonCodeListResponse.from(entities);
    }

    @Transactional
    public void create(CommonCodeRequest request) {
        if (commonCodeJpaRepository.existsByGroupCodeAndCode(request.groupCode(), request.code())) {
            throw new IllegalArgumentException("이미 존재하는 코드입니다: " + request.groupCode() + "/" + request.code());
        }
        String now = LocalDateTime.now().format(DT_FMT);
        CommonCodeEntity entity = new CommonCodeEntity();
        entity.setGroupCode(request.groupCode());
        entity.setGroupName(request.groupName());
        entity.setCode(request.code());
        entity.setCodeName(request.codeName());
        entity.setNote(request.note());
        entity.setUseYn(request.useYn() != null ? request.useYn() : "Y");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        commonCodeJpaRepository.save(entity);
    }

    @Transactional
    public void update(Integer id, CommonCodeRequest request) {
        CommonCodeEntity entity = commonCodeJpaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("공통코드를 찾을 수 없습니다: " + id));
        if (commonCodeJpaRepository.existsByGroupCodeAndCodeAndIdNot(
                request.groupCode(), request.code(), id)) {
            throw new IllegalArgumentException("이미 존재하는 코드입니다: " + request.groupCode() + "/" + request.code());
        }
        entity.setGroupCode(request.groupCode());
        entity.setGroupName(request.groupName());
        entity.setCode(request.code());
        entity.setCodeName(request.codeName());
        entity.setNote(request.note());
        if (request.useYn() != null) entity.setUseYn(request.useYn());
        entity.setUpdatedAt(LocalDateTime.now().format(DT_FMT));
        commonCodeJpaRepository.save(entity);
    }
}
```

- [ ] **Step 4: CommonCodeController 구현**

```java
package com.sfood.automation.common_code.presentation;

import com.sfood.automation.common_code.application.dto.CommonCodeListResponse;
import com.sfood.automation.common_code.application.dto.CommonCodeRequest;
import com.sfood.automation.common_code.application.usecase.CommonCodeUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/common-codes")
public class CommonCodeController {

    private final CommonCodeUseCase commonCodeUseCase;

    public CommonCodeController(CommonCodeUseCase commonCodeUseCase) {
        this.commonCodeUseCase = commonCodeUseCase;
    }

    @GetMapping
    public ResponseEntity<CommonCodeListResponse> findAll(
            @RequestParam(required = false) String groupCode) {
        return ResponseEntity.ok(commonCodeUseCase.findAll(groupCode));
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody CommonCodeRequest request) {
        commonCodeUseCase.create(request);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                        @RequestBody CommonCodeRequest request) {
        commonCodeUseCase.update(id, request);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 5: 테스트 실행**

```bash
./gradlew test --tests "com.sfood.automation.common_code.presentation.CommonCodeControllerTest"
```
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/sfood/automation/common_code/ \
        src/test/java/com/sfood/automation/common_code/
git commit -m "feat: CommonCodeUseCase, CommonCodeController — 공통코드 CRUD API 신규"
```

---

## Task 11: MemberManageController 확장 + 컨트롤러 테스트 갱신

**Files:**
- Modify: `src/main/java/com/sfood/automation/member/presentation/MemberManageController.java`
- Modify: `src/test/java/com/sfood/automation/member/presentation/MemberManageControllerTest.java`

- [ ] **Step 1: MemberManageController — 신규 엔드포인트 추가 + loggedBy 처리**

```java
package com.sfood.automation.member.presentation;

import com.sfood.automation.common.security.SessionAuthInterceptor;
import com.sfood.automation.member.application.dto.*;
import com.sfood.automation.member.application.usecase.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
public class MemberManageController {

    private final UpdateMemberPartnerUseCase updateMemberPartnerUseCase;
    private final UpdateMemberBusinessUseCase updateMemberBusinessUseCase;
    private final GetMemberBusinessLogsUseCase getMemberBusinessLogsUseCase;
    private final GetMemberPartnerLogsUseCase getMemberPartnerLogsUseCase;
    private final DeleteMemberUseCase deleteMemberUseCase;

    public MemberManageController(
        UpdateMemberPartnerUseCase updateMemberPartnerUseCase,
        UpdateMemberBusinessUseCase updateMemberBusinessUseCase,
        GetMemberBusinessLogsUseCase getMemberBusinessLogsUseCase,
        GetMemberPartnerLogsUseCase getMemberPartnerLogsUseCase,
        DeleteMemberUseCase deleteMemberUseCase
    ) {
        this.updateMemberPartnerUseCase = updateMemberPartnerUseCase;
        this.updateMemberBusinessUseCase = updateMemberBusinessUseCase;
        this.getMemberBusinessLogsUseCase = getMemberBusinessLogsUseCase;
        this.getMemberPartnerLogsUseCase = getMemberPartnerLogsUseCase;
        this.deleteMemberUseCase = deleteMemberUseCase;
    }

    @PutMapping("/{memNo}/partner")
    public ResponseEntity<MemberPartnerUpdateResponse> updatePartner(
            @PathVariable Long memNo,
            @RequestBody MemberPartnerUpdateRequest request,
            HttpSession session) {
        String loggedBy = (String) session.getAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY);
        return ResponseEntity.ok(updateMemberPartnerUseCase.execute(memNo, request, loggedBy));
    }

    @PutMapping("/{memNo}/business")
    public ResponseEntity<Void> updateBusiness(
            @PathVariable Long memNo,
            @RequestBody MemberBusinessUpdateRequest request,
            HttpSession session) {
        String loggedBy = (String) session.getAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY);
        updateMemberBusinessUseCase.execute(memNo, request, loggedBy);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{memNo}/business/logs")
    public ResponseEntity<MemberBusinessLogListResponse> getBusinessLogs(@PathVariable Long memNo) {
        return ResponseEntity.ok(getMemberBusinessLogsUseCase.execute(memNo));
    }

    @GetMapping("/{memNo}/partner/logs")
    public ResponseEntity<MemberPartnerLogListResponse> getPartnerLogs(@PathVariable Long memNo) {
        return ResponseEntity.ok(getMemberPartnerLogsUseCase.execute(memNo));
    }

    @DeleteMapping("/{memNo}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long memNo) {
        deleteMemberUseCase.execute(memNo);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: MemberManageControllerTest 갱신 — duplicateCheck → memberStatus + 신규 엔드포인트 테스트**

기존 테스트에서 `duplicateCheck` 참조를 `memberStatus`로 교체하고, `@MockBean` 추가, 신규 테스트를 추가한다.

```java
@MockBean private UpdateMemberBusinessUseCase updateMemberBusinessUseCase;
@MockBean private GetMemberBusinessLogsUseCase getMemberBusinessLogsUseCase;
@MockBean private GetMemberPartnerLogsUseCase getMemberPartnerLogsUseCase;
```

`should_return_200_with_updated_partner` 수정:
```java
when(updateMemberPartnerUseCase.execute(eq(900101L), any(MemberPartnerUpdateRequest.class), any()))
    .thenReturn(new MemberPartnerUpdateResponse(
        "y", "990954", "(주)테스트", "n", "NORMAL", null, "2026-06-15T00:00:00Z"));

// ...
.andExpect(jsonPath("$.memberStatus").value("NORMAL"))
```

`should_return_409_when_partner_code_is_duplicate` 수정:
```java
when(updateMemberPartnerUseCase.execute(eq(900101L), any(MemberPartnerUpdateRequest.class), any()))
    .thenThrow(new DuplicatePartnerCodeException());
```

신규 테스트 추가:
```java
@Test
void should_return_200_on_business_update() throws Exception {
    doNothing().when(updateMemberBusinessUseCase).execute(eq(900101L),
        any(MemberBusinessUpdateRequest.class), any());

    mockMvc.perform(put("/api/members/900101/business")
            .session(authenticatedSession())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "company", "수정상호", "businessNo", "111-22-33333"))))
        .andExpect(status().isOk());
}

@Test
void should_return_200_on_get_business_logs() throws Exception {
    when(getMemberBusinessLogsUseCase.execute(900101L))
        .thenReturn(new MemberBusinessLogListResponse(List.of()));

    mockMvc.perform(get("/api/members/900101/business/logs").session(authenticatedSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.logs").isArray());
}

@Test
void should_return_200_on_get_partner_logs() throws Exception {
    when(getMemberPartnerLogsUseCase.execute(900101L))
        .thenReturn(new MemberPartnerLogListResponse(List.of()));

    mockMvc.perform(get("/api/members/900101/partner/logs").session(authenticatedSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.logs").isArray());
}
```

- [ ] **Step 3: 컨트롤러 테스트 실행**

```bash
./gradlew test --tests "com.sfood.automation.member.presentation.MemberManageControllerTest"
```
Expected: PASS

- [ ] **Step 4: 전체 테스트 실행**

```bash
./gradlew test 2>&1 | tail -40
```
Expected: BUILD SUCCESSFUL (전체 통과)

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/sfood/automation/member/presentation/MemberManageController.java \
        src/test/java/com/sfood/automation/member/presentation/MemberManageControllerTest.java
git commit -m "feat: MemberManageController — 사업자 수정·이력, 거래처 이력 엔드포인트 추가"
```

---

## Task 12: 전체 테스트 통과 검증 및 최종 커밋

**Files:** 없음 (검증 전용)

- [ ] **Step 1: 전체 테스트 실행**

```bash
./gradlew test
```
Expected: BUILD SUCCESSFUL — 모든 테스트 PASS

- [ ] **Step 2: 실패 테스트 있을 경우 개별 확인**

```bash
./gradlew test 2>&1 | grep "FAILED\|ERROR"
```

- [ ] **Step 3: 최종 커밋 (변경 파일 남은 경우)**

```bash
git add -p
git commit -m "fix: 전체 테스트 통과 — 잔여 수정사항 정리"
```

---

## 참고 사항

### CrawledMember 필드 순서 확인

`FetchNewMembersUseCaseIntegrationTest`의 `CrawledMember` 생성자 파라미터 순서는 `CrawledMember.java` 파일의 record 필드 순서와 정확히 일치해야 한다. Task 8 작성 전 반드시 확인한다.

```bash
cat src/main/java/com/sfood/automation/member/application/dto/CrawledMember.java
```

### 테스트 DB 초기화

테스트 간 테이블이 꼬이는 경우 `data/sfood-mall-automation_test.db` 를 삭제하면 깨끗하게 재생성된다.

```bash
rm data/sfood-mall-automation_test.db
```

### QueryDSL Q클래스 재생성

엔티티 필드 추가 후 Q클래스 반영이 안 되면 clean 후 다시 빌드한다.

```bash
./gradlew clean compileJava
```
