# 업태/업종 코드 맵핑 및 공통코드 조회 API 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `godo_member_business` / `godo_member_business_log`에 `service_code`, `item_code` 컬럼을 추가하고, 배치 수집·엑셀 업로드 시 업태/업종 텍스트를 공통코드로 자동 맵핑한다. 공통코드 조회 API에 `useYn` 필터 파라미터를 추가한다.

**Architecture:** 인메모리 캐시(`CommonCodeMappingService`)로 텍스트→코드 맵핑을 O(1)로 처리한다. 초기 데이터(업태 79건, 업종 6372건)는 `data.sql` / `data-postgresql.sql`에 `INSERT OR IGNORE` / `ON CONFLICT DO NOTHING` 구문으로 추가한다. `useYn` 필터는 기존 `GET /api/common-codes` 파라미터 확장으로 구현해 기존 동작을 깨지 않는다.

**Tech Stack:** Java 21, Spring Boot 3, JPA, JdbcTemplate, SQLite(test), PostgreSQL(prod), openpyxl(SQL 생성 스크립트)

---

## 파일 변경 목록

| 구분 | 파일 | 작업 |
|---|---|---|
| 신규 | `common_code/application/usecase/CommonCodeMappingService.java` | 텍스트→코드 인메모리 캐시 |
| 신규 | `common_code/application/usecase/CommonCodeMappingServiceTest.java` | 단위 테스트 |
| 수정 | `resources/schema.sql` | godo_member_business, log에 컬럼 추가 |
| 수정 | `resources/postgresql/schema-member-postgresql.sql` | ALTER TABLE IF EXISTS |
| 수정 | `common/bootstrap/SqliteSchemaMigration.java` | ensureGodoMemberBusinessCodeColumns() |
| 수정 | `support/SharedSqliteTestDatabase.java` | ensureColumn() 추가 |
| 수정 | `resources/data.sql` | 업태/업종 INSERT OR IGNORE |
| 수정 | `resources/postgresql/data-postgresql.sql` | 업태/업종 ON CONFLICT DO NOTHING |
| 수정 | `persistence/GodoMemberBusinessEntity.java` | serviceCode, itemCode 필드 |
| 수정 | `persistence/GodoMemberBusinessLogEntity.java` | serviceCode, itemCode 필드 |
| 수정 | `dto/MemberBusinessUpdateRequest.java` | serviceCode, itemCode |
| 수정 | `dto/MemberBusinessDetailResponse.java` | serviceCode, itemCode |
| 수정 | `dto/MemberBusinessLogListResponse.java` | LogItem에 serviceCode, itemCode |
| 수정 | `persistence/MemberRepositoryImpl.java` | JDBC SQL + 파라미터 바인딩 |
| 수정 | `usecase/UpdateMemberBusinessUseCase.java` | 코드 저장 |
| 수정 | `usecase/GetMemberDetailUseCase.java` | 응답에 코드 포함 |
| 수정 | `usecase/FetchNewMembersUseCase.java` | 배치 맵핑 |
| 수정 | `usecase/UploadMembersUseCase.java` | 업로드 맵핑 |
| 수정 | `persistence/CommonCodeJpaRepository.java` | useYn 필터 메서드 |
| 수정 | `usecase/CommonCodeUseCase.java` | findAll(groupCode, useYn) |
| 수정 | `presentation/CommonCodeController.java` | useYn RequestParam |
| 수정 | `presentation/CommonCodeControllerTest.java` | useYn 파라미터 테스트 |

---

## Task 1: 스키마 변경 — service_code, item_code 컬럼 추가

**Files:**
- Modify: `src/main/resources/schema.sql`
- Modify: `src/main/resources/postgresql/schema-member-postgresql.sql`
- Modify: `src/main/java/com/sfood/automation/common/bootstrap/SqliteSchemaMigration.java`
- Modify: `src/test/java/com/sfood/automation/support/SharedSqliteTestDatabase.java`

- [ ] **Step 1: schema.sql — godo_member_business에 컬럼 추가**

`src/main/resources/schema.sql`의 `godo_member_business` 테이블 정의에서 `duplicate_mem_no TEXT,` 다음 줄에 추가:

```sql
    service_code TEXT,
    item_code    TEXT,
```

변경 후 `godo_member_business` 테이블 하단부:
```sql
    com_address     TEXT,
    biz_license_file TEXT,
    duplicate_mem_no TEXT,
    service_code    TEXT,
    item_code       TEXT,
    created_at      TEXT NOT NULL,
    updated_at      TEXT NOT NULL,
```

- [ ] **Step 2: schema.sql — godo_member_business_log에 컬럼 추가**

`src/main/resources/schema.sql`의 `godo_member_business_log` 테이블 정의에서 `biz_license_file TEXT,` 다음 줄에 추가:

```sql
    service_code TEXT,
    item_code    TEXT,
```

변경 후:
```sql
    com_address     TEXT,
    biz_license_file TEXT,
    service_code    TEXT,
    item_code       TEXT,
    logged_at       TEXT NOT NULL,
```

- [ ] **Step 3: schema-member-postgresql.sql — ALTER TABLE 추가**

`src/main/resources/postgresql/schema-member-postgresql.sql`의 마지막 `ALTER TABLE` 블록 (godo_member_business 변경 섹션)에 추가:

```sql
ALTER TABLE IF EXISTS godo_member_business ADD COLUMN IF NOT EXISTS service_code TEXT;
ALTER TABLE IF EXISTS godo_member_business ADD COLUMN IF NOT EXISTS item_code TEXT;

ALTER TABLE IF EXISTS godo_member_business_log ADD COLUMN IF NOT EXISTS service_code TEXT;
ALTER TABLE IF EXISTS godo_member_business_log ADD COLUMN IF NOT EXISTS item_code TEXT;
```

- [ ] **Step 4: SqliteSchemaMigration — ensureGodoMemberBusinessCodeColumns() 추가**

`src/main/java/com/sfood/automation/common/bootstrap/SqliteSchemaMigration.java`의 `afterPropertiesSet()` 메서드에 호출 추가:

```java
@Override
public void afterPropertiesSet() {
    ensureGoodsSyncBlockedColumn();
    ensureGoodsRemainValidDaysColumn();
    ensureCommonStatusConfigRemainValidDaysColumn();
    synchronizeGoodsRemainValidDaysFromConfig();
    ensureGoodsStatusLogRemainValidDaysColumn();
    ensureGoodsStatusLogNullableValidPercent();
    ensureMemberPartnerMemberStatusColumn();
    ensureGodoMemberBusinessDuplicateMemNoColumn();
    ensureGodoMemberBusinessCodeColumns();      // 추가
    ensureGodoMemberBusinessLogTable();
    ensureMemberPartnerLogTable();
    ensureCommonCodeTable();
}
```

클래스 내 메서드 추가 (예: `ensureGodoMemberBusinessDuplicateMemNoColumn()` 바로 아래):

```java
private void ensureGodoMemberBusinessCodeColumns() {
    ensureColumn("godo_member_business", "service_code",
        "ALTER TABLE godo_member_business ADD COLUMN service_code TEXT");
    ensureColumn("godo_member_business", "item_code",
        "ALTER TABLE godo_member_business ADD COLUMN item_code TEXT");
    ensureColumn("godo_member_business_log", "service_code",
        "ALTER TABLE godo_member_business_log ADD COLUMN service_code TEXT");
    ensureColumn("godo_member_business_log", "item_code",
        "ALTER TABLE godo_member_business_log ADD COLUMN item_code TEXT");
}
```

- [ ] **Step 5: SharedSqliteTestDatabase — ensureColumn 추가**

`src/test/java/com/sfood/automation/support/SharedSqliteTestDatabase.java`의 `ensureSchemaCompatibility()` 메서드에서 `ensureTable(statement, "common_code", ...)` 호출 바로 앞에 추가:

```java
ensureColumn(statement, "godo_member_business", "service_code",
    "ALTER TABLE godo_member_business ADD COLUMN service_code TEXT");
ensureColumn(statement, "godo_member_business", "item_code",
    "ALTER TABLE godo_member_business ADD COLUMN item_code TEXT");
ensureColumn(statement, "godo_member_business_log", "service_code",
    "ALTER TABLE godo_member_business_log ADD COLUMN service_code TEXT");
ensureColumn(statement, "godo_member_business_log", "item_code",
    "ALTER TABLE godo_member_business_log ADD COLUMN item_code TEXT");
```

- [ ] **Step 6: 빌드 확인**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/schema.sql \
        src/main/resources/postgresql/schema-member-postgresql.sql \
        src/main/java/com/sfood/automation/common/bootstrap/SqliteSchemaMigration.java \
        src/test/java/com/sfood/automation/support/SharedSqliteTestDatabase.java
git commit -m "feat: godo_member_business, log 테이블에 service_code, item_code 컬럼 추가"
```

---

## Task 2: 공통코드 초기 데이터 — data.sql / data-postgresql.sql

**Files:**
- Modify: `src/main/resources/data.sql`
- Modify: `src/main/resources/postgresql/data-postgresql.sql`

- [ ] **Step 1: Python 스크립트로 SQL 생성**

프로젝트 루트(`backend/`)에서 아래 스크립트를 실행해 두 파일 내용을 생성한다.

```bash
python3 - <<'EOF'
import openpyxl

wb = openpyxl.load_workbook('docs/analysis/common_code.xlsx')
ws = wb.active
DT = '2026-07-03 00:00:00'

service_rows, item_rows = [], []
for row in ws.iter_rows(min_row=3, values_only=True):
    if row[5] is not None:
        code = str(row[5]).strip()
        name = (str(row[6]).strip() if row[6] else '').replace("'", "''")
        if code not in ('common_code',) and name and name != 'common_name':
            service_rows.append((code, name))
    if row[1] is not None:
        code = str(row[1]).strip()
        name = (str(row[2]).strip() if row[2] else '').replace("'", "''")
        if code not in ('common_code',) and name and name != 'common_name':
            item_rows.append((code, name))

# SQLite
with open('/tmp/common_code_sqlite.sql', 'w') as f:
    f.write('-- 공통코드: 업태(SERVICE)\n')
    for code, name in service_rows:
        f.write(f"INSERT OR IGNORE INTO common_code (group_code,group_name,code,code_name,use_yn,created_at,updated_at) VALUES ('SERVICE','업태','{code}','{name}','Y','{DT}','{DT}');\n")
    f.write('-- 공통코드: 업종(ITEM)\n')
    for code, name in item_rows:
        f.write(f"INSERT OR IGNORE INTO common_code (group_code,group_name,code,code_name,use_yn,created_at,updated_at) VALUES ('ITEM','업종','{code}','{name}','Y','{DT}','{DT}');\n")

# PostgreSQL
with open('/tmp/common_code_postgresql.sql', 'w') as f:
    f.write('-- 공통코드: 업태(SERVICE)\n')
    for code, name in service_rows:
        f.write(f"INSERT INTO common_code (group_code,group_name,code,code_name,use_yn,created_at,updated_at) VALUES ('SERVICE','업태','{code}','{name}','Y','{DT}','{DT}') ON CONFLICT (group_code,code) DO NOTHING;\n")
    f.write('-- 공통코드: 업종(ITEM)\n')
    for code, name in item_rows:
        f.write(f"INSERT INTO common_code (group_code,group_name,code,code_name,use_yn,created_at,updated_at) VALUES ('ITEM','업종','{code}','{name}','Y','{DT}','{DT}') ON CONFLICT (group_code,code) DO NOTHING;\n")

print(f"업태 {len(service_rows)}건, 업종 {len(item_rows)}건 생성 완료")
EOF
```

Expected output: `업태 79건, 업종 6372건 생성 완료`

- [ ] **Step 2: data.sql에 추가**

```bash
echo "" >> src/main/resources/data.sql
cat /tmp/common_code_sqlite.sql >> src/main/resources/data.sql
```

- [ ] **Step 3: data-postgresql.sql에 추가**

```bash
echo "" >> src/main/resources/postgresql/data-postgresql.sql
cat /tmp/common_code_postgresql.sql >> src/main/resources/postgresql/data-postgresql.sql
```

- [ ] **Step 4: 행 수 확인**

```bash
grep -c "INSERT OR IGNORE INTO common_code" src/main/resources/data.sql
grep -c "INSERT INTO common_code" src/main/resources/postgresql/data-postgresql.sql
```

Expected: 두 출력 모두 `6451` (업태 79 + 업종 6372 = 6451)

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/data.sql src/main/resources/postgresql/data-postgresql.sql
git commit -m "feat: 공통코드 업태(79건), 업종(6372건) 초기 데이터 추가"
```

---

## Task 3: CommonCodeMappingService — 텍스트→코드 인메모리 캐시

**Files:**
- Create: `src/main/java/com/sfood/automation/common_code/application/usecase/CommonCodeMappingService.java`
- Create: `src/test/java/com/sfood/automation/common_code/application/usecase/CommonCodeMappingServiceTest.java`

- [ ] **Step 1: 실패 단위 테스트 작성**

```java
// src/test/java/com/sfood/automation/common_code/application/usecase/CommonCodeMappingServiceTest.java
package com.sfood.automation.common_code.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sfood.automation.common_code.infrastructure.persistence.CommonCodeEntity;
import com.sfood.automation.common_code.infrastructure.persistence.CommonCodeJpaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommonCodeMappingServiceTest {

    @Mock
    private CommonCodeJpaRepository repo;

    private CommonCodeMappingService service;

    @BeforeEach
    void setUp() {
        CommonCodeEntity s1 = entity("SERVICE", "업태", "046", "식품");
        CommonCodeEntity s2 = entity("SERVICE", "업태", "001", "가구");
        CommonCodeEntity i1 = entity("ITEM", "업종", "0037", "각종식품");
        CommonCodeEntity i2 = entity("ITEM", "업종", "9999", "기타");
        when(repo.findByUseYn("Y")).thenReturn(List.of(s1, s2, i1, i2));
        service = new CommonCodeMappingService(repo);
        service.loadCache();
    }

    @Test
    void findServiceCode_exactMatch() {
        assertThat(service.findServiceCode("식품")).isEqualTo(Optional.of("046"));
    }

    @Test
    void findServiceCode_trimAndCaseInsensitive() {
        assertThat(service.findServiceCode("  식품  ")).isEqualTo(Optional.of("046"));
    }

    @Test
    void findServiceCode_noMatch() {
        assertThat(service.findServiceCode("없는업태")).isEmpty();
    }

    @Test
    void findServiceCode_null() {
        assertThat(service.findServiceCode(null)).isEmpty();
    }

    @Test
    void findItemCode_exactMatch() {
        assertThat(service.findItemCode("각종식품")).isEqualTo(Optional.of("0037"));
    }

    @Test
    void findItemCode_noMatch() {
        assertThat(service.findItemCode("없는업종")).isEmpty();
    }

    private CommonCodeEntity entity(String groupCode, String groupName, String code, String codeName) {
        CommonCodeEntity e = new CommonCodeEntity();
        e.setGroupCode(groupCode);
        e.setGroupName(groupName);
        e.setCode(code);
        e.setCodeName(codeName);
        e.setUseYn("Y");
        e.setCreatedAt("2026-07-03 00:00:00");
        e.setUpdatedAt("2026-07-03 00:00:00");
        return e;
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./mvnw test -pl . -Dtest=CommonCodeMappingServiceTest -q 2>&1 | tail -5
```

Expected: FAIL (CommonCodeMappingService, findByUseYn 미존재)

- [ ] **Step 3: CommonCodeJpaRepository — findByUseYn 추가**

`src/main/java/com/sfood/automation/common_code/infrastructure/persistence/CommonCodeJpaRepository.java`에 추가:

```java
List<CommonCodeEntity> findByUseYn(String useYn);
```

전체 파일:
```java
package com.sfood.automation.common_code.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommonCodeJpaRepository extends JpaRepository<CommonCodeEntity, Integer> {

    List<CommonCodeEntity> findByGroupCodeOrderByCodeAsc(String groupCode);

    List<CommonCodeEntity> findAllByOrderByGroupCodeAscCodeAsc();

    List<CommonCodeEntity> findByGroupCodeAndUseYnOrderByCodeAsc(String groupCode, String useYn);

    List<CommonCodeEntity> findByUseYn(String useYn);

    boolean existsByGroupCodeAndCodeAndIdNot(String groupCode, String code, Integer id);

    boolean existsByGroupCodeAndCode(String groupCode, String code);
}
```

- [ ] **Step 4: CommonCodeMappingService 구현**

```java
// src/main/java/com/sfood/automation/common_code/application/usecase/CommonCodeMappingService.java
package com.sfood.automation.common_code.application.usecase;

import com.sfood.automation.common_code.infrastructure.persistence.CommonCodeJpaRepository;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CommonCodeMappingService {

    private static final Logger log = LoggerFactory.getLogger(CommonCodeMappingService.class);

    private final CommonCodeJpaRepository repo;
    private Map<String, String> serviceNameToCode = new HashMap<>();
    private Map<String, String> itemNameToCode = new HashMap<>();

    public CommonCodeMappingService(CommonCodeJpaRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void loadCache() {
        try {
            Map<String, String> svc = new HashMap<>();
            Map<String, String> itm = new HashMap<>();
            repo.findByUseYn("Y").forEach(e -> {
                String key = e.getCodeName() == null ? null : e.getCodeName().trim().toLowerCase();
                if (key == null || key.isEmpty()) return;
                if ("SERVICE".equals(e.getGroupCode())) svc.put(key, e.getCode());
                else if ("ITEM".equals(e.getGroupCode())) itm.put(key, e.getCode());
            });
            this.serviceNameToCode = svc;
            this.itemNameToCode = itm;
        } catch (Exception ex) {
            log.warn("공통코드 캐시 로드 실패 — 코드 맵핑 비활성화: {}", ex.getMessage());
        }
    }

    public Optional<String> findServiceCode(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        return Optional.ofNullable(serviceNameToCode.get(text.trim().toLowerCase()));
    }

    public Optional<String> findItemCode(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        return Optional.ofNullable(itemNameToCode.get(text.trim().toLowerCase()));
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

```bash
./mvnw test -pl . -Dtest=CommonCodeMappingServiceTest -q 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`, `Tests run: 6, Failures: 0`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/sfood/automation/common_code/infrastructure/persistence/CommonCodeJpaRepository.java \
        src/main/java/com/sfood/automation/common_code/application/usecase/CommonCodeMappingService.java \
        src/test/java/com/sfood/automation/common_code/application/usecase/CommonCodeMappingServiceTest.java
git commit -m "feat: CommonCodeMappingService — 텍스트→코드 인메모리 캐시"
```

---

## Task 4: Entity / DTO 변경 — serviceCode, itemCode 필드 추가

**Files:**
- Modify: `src/main/java/com/sfood/automation/member/infrastructure/persistence/GodoMemberBusinessEntity.java`
- Modify: `src/main/java/com/sfood/automation/member/infrastructure/persistence/GodoMemberBusinessLogEntity.java`
- Modify: `src/main/java/com/sfood/automation/member/application/dto/MemberBusinessUpdateRequest.java`
- Modify: `src/main/java/com/sfood/automation/member/application/dto/MemberBusinessDetailResponse.java`
- Modify: `src/main/java/com/sfood/automation/member/application/dto/MemberBusinessLogListResponse.java`

- [ ] **Step 1: GodoMemberBusinessEntity — 필드 추가**

`src/main/java/com/sfood/automation/member/infrastructure/persistence/GodoMemberBusinessEntity.java`:

`private String duplicateMemNo;` 필드 아래에 추가:

```java
@Column(name = "service_code")
private String serviceCode;

@Column(name = "item_code")
private String itemCode;
```

getter/setter 추가 (클래스 말미):

```java
public String getServiceCode() { return serviceCode; }
public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
public String getItemCode() { return itemCode; }
public void setItemCode(String itemCode) { this.itemCode = itemCode; }
```

- [ ] **Step 2: GodoMemberBusinessLogEntity — 필드 추가**

`src/main/java/com/sfood/automation/member/infrastructure/persistence/GodoMemberBusinessLogEntity.java`:

`private String item;` 필드 아래에 추가:

```java
@Column(name = "service_code")
private String serviceCode;

@Column(name = "item_code")
private String itemCode;
```

getter/setter 추가:

```java
public String getServiceCode() { return serviceCode; }
public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
public String getItemCode() { return itemCode; }
public void setItemCode(String itemCode) { this.itemCode = itemCode; }
```

- [ ] **Step 3: MemberBusinessUpdateRequest — 필드 추가**

```java
// src/main/java/com/sfood/automation/member/application/dto/MemberBusinessUpdateRequest.java
package com.sfood.automation.member.application.dto;

public record MemberBusinessUpdateRequest(
    String company,
    String businessNo,
    String ceoName,
    String service,
    String serviceCode,
    String item,
    String itemCode,
    String comAddress,
    String bizLicenseFile
) {}
```

- [ ] **Step 4: MemberBusinessDetailResponse — 필드 추가**

```java
// src/main/java/com/sfood/automation/member/application/dto/MemberBusinessDetailResponse.java
package com.sfood.automation.member.application.dto;

public record MemberBusinessDetailResponse(
    String company,
    String businessNo,
    String ceoName,
    String service,
    String serviceCode,
    String item,
    String itemCode,
    String comAddress,
    String bizLicenseFile
) {}
```

- [ ] **Step 5: MemberBusinessLogListResponse — LogItem에 필드 추가**

```java
// src/main/java/com/sfood/automation/member/application/dto/MemberBusinessLogListResponse.java
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
        String serviceCode,
        String item,
        String itemCode,
        String comAddress,
        String bizLicenseFile,
        String loggedAt,
        String loggedBy
    ) {}

    public static MemberBusinessLogListResponse from(
            List<GodoMemberBusinessLogEntity> entities) {
        List<LogItem> items = entities.stream().map(e -> new LogItem(
            e.getId(), e.getMemNo(), e.getCompany(), e.getBusinessNo(),
            e.getCeoName(), e.getService(), e.getServiceCode(),
            e.getItem(), e.getItemCode(), e.getComAddress(),
            e.getBizLicenseFile(), e.getLoggedAt(), e.getLoggedBy()
        )).toList();
        return new MemberBusinessLogListResponse(items);
    }
}
```

- [ ] **Step 6: 기존 테스트의 MemberBusinessUpdateRequest 생성 코드 수정**

`MemberBusinessUpdateRequest` record가 7인수 → 9인수로 변경되므로 아래 두 테스트를 수정한다.

**UpdateMemberBusinessUseCaseIntegrationTest.java** (기존 7인수: company, businessNo, ceoName, service, item, comAddress, bizLicenseFile → 신규 9인수: company, businessNo, ceoName, service, **serviceCode**, item, **itemCode**, comAddress, bizLicenseFile):

```java
// should_update_business_info_and_save_log()
MemberBusinessUpdateRequest request = new MemberBusinessUpdateRequest(
    "수정상호", "222-33-44444", "수정대표", "도매", null, "식품류", null, "서울시 수정구", null);

// should_throw_404_for_unknown_member()
new MemberBusinessUpdateRequest(null, null, null, null, null, null, null, null, null)
```

**GetMemberLogsUseCaseIntegrationTest.java**:

```java
new MemberBusinessUpdateRequest("1차수정", null, null, null, null, null, null, null, null)
new MemberBusinessUpdateRequest("2차수정", null, null, null, null, null, null, null, null)
```

- [ ] **Step 7: 컴파일 확인**

```bash
./mvnw compile -q 2>&1 | grep -E "ERROR|error" | head -20
```

Expected: 에러 없음 (UseCase 호출부 오류는 Task 6에서 처리)

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/sfood/automation/member/infrastructure/persistence/GodoMemberBusinessEntity.java \
        src/main/java/com/sfood/automation/member/infrastructure/persistence/GodoMemberBusinessLogEntity.java \
        src/main/java/com/sfood/automation/member/application/dto/MemberBusinessUpdateRequest.java \
        src/main/java/com/sfood/automation/member/application/dto/MemberBusinessDetailResponse.java \
        src/main/java/com/sfood/automation/member/application/dto/MemberBusinessLogListResponse.java \
        src/test/java/com/sfood/automation/member/application/usecase/UpdateMemberBusinessUseCaseIntegrationTest.java \
        src/test/java/com/sfood/automation/member/application/usecase/GetMemberLogsUseCaseIntegrationTest.java
git commit -m "feat: GodoMemberBusiness entity/DTO에 serviceCode, itemCode 필드 추가"
```

---

## Task 5: MemberRepositoryImpl — JDBC SQL 변경

**Files:**
- Modify: `src/main/java/com/sfood/automation/member/infrastructure/persistence/MemberRepositoryImpl.java`

현재 SQL 상수 및 파라미터 바인딩을 변경한다.

- [ ] **Step 1: SQL 상수 수정**

`MemberRepositoryImpl.java`에서 기존 SQL 상수 3개를 수정:

```java
private static final String INSERT_BIZ_SQL =
    "INSERT INTO godo_member_business (mem_no,company,business_no,ceo_name,service,service_code,item,item_code,com_address,biz_license_file,created_at,updated_at) " +
    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

private static final String UPDATE_BIZ_LIST_SQL =
    "UPDATE godo_member_business SET company=?,business_no=?,ceo_name=?,service=?,service_code=?,item=?,item_code=?,com_address=?,updated_at=? WHERE mem_no=?";

private static final String UPDATE_BIZ_TEMPLATE_SQL =
    "UPDATE godo_member_business SET company=?,business_no=?,ceo_name=?,service=?,service_code=?,item=?,item_code=?,com_address=?,biz_license_file=?,updated_at=? WHERE mem_no=?";
```

- [ ] **Step 2: bulkInsertBusinesses() 파라미터 바인딩 수정**

`bulkInsertBusinesses()`의 `setValues()` 메서드:

```java
@Override
public void setValues(PreparedStatement ps, int i) throws SQLException {
    GodoMemberBusinessEntity b = businesses.get(i);
    ps.setObject(1, b.getMemNo());
    ps.setObject(2, b.getCompany());
    ps.setObject(3, b.getBusinessNo());
    ps.setObject(4, enc(b.getCeoName()));
    ps.setObject(5, b.getService());
    ps.setObject(6, b.getServiceCode());
    ps.setObject(7, b.getItem());
    ps.setObject(8, b.getItemCode());
    ps.setObject(9, enc(b.getComAddress()));
    ps.setObject(10, b.getBizLicenseFile());
    ps.setObject(11, b.getCreatedAt());
    ps.setObject(12, b.getUpdatedAt());
}
```

- [ ] **Step 3: bulkUpdateBusinesses() 파라미터 바인딩 수정**

`bulkUpdateBusinesses()`의 `setValues()` 메서드:

```java
@Override
public void setValues(PreparedStatement ps, int i) throws SQLException {
    GodoMemberBusinessEntity b = businesses.get(i);
    int p = 1;
    ps.setObject(p++, b.getCompany());
    ps.setObject(p++, b.getBusinessNo());
    ps.setObject(p++, enc(b.getCeoName()));
    ps.setObject(p++, b.getService());
    ps.setObject(p++, b.getServiceCode());
    ps.setObject(p++, b.getItem());
    ps.setObject(p++, b.getItemCode());
    ps.setObject(p++, enc(b.getComAddress()));
    if (type == MemberUploadType.TEMPLATE) {
        ps.setObject(p++, b.getBizLicenseFile());
    }
    ps.setObject(p++, b.getUpdatedAt());
    ps.setObject(p, b.getMemNo());
}
```

- [ ] **Step 4: 컴파일 확인**

```bash
./mvnw compile -q 2>&1 | grep -E "ERROR|error" | head -20
```

Expected: 에러 없음 (UseCase 호출부 오류는 다음 Task에서 처리)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/sfood/automation/member/infrastructure/persistence/MemberRepositoryImpl.java
git commit -m "feat: MemberRepositoryImpl JDBC SQL에 service_code, item_code 컬럼 추가"
```

---

## Task 6: UpdateMemberBusinessUseCase / GetMemberDetailUseCase — 코드 저장 및 응답

**Files:**
- Modify: `src/main/java/com/sfood/automation/member/application/usecase/UpdateMemberBusinessUseCase.java`
- Modify: `src/main/java/com/sfood/automation/member/application/usecase/GetMemberDetailUseCase.java`

- [ ] **Step 1: UpdateMemberBusinessUseCase — log와 entity에 코드 저장**

```java
// src/main/java/com/sfood/automation/member/application/usecase/UpdateMemberBusinessUseCase.java
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
            .map(existing -> {
                GodoMemberBusinessLogEntity log = new GodoMemberBusinessLogEntity();
                log.setMemNo(existing.getMemNo());
                log.setCompany(existing.getCompany());
                log.setBusinessNo(existing.getBusinessNo());
                log.setCeoName(existing.getCeoName());
                log.setService(existing.getService());
                log.setServiceCode(existing.getServiceCode());
                log.setItem(existing.getItem());
                log.setItemCode(existing.getItemCode());
                log.setComAddress(existing.getComAddress());
                log.setBizLicenseFile(existing.getBizLicenseFile());
                log.setLoggedAt(now);
                log.setLoggedBy(loggedBy);
                log.setCreatedAt(now);
                memberRepository.saveBusinessLog(log);
                return existing;
            })
            .orElseGet(() -> {
                GodoMemberBusinessEntity created = new GodoMemberBusinessEntity();
                created.setMemNo(memNo);
                created.setCreatedAt(now);
                return created;
            });

        biz.setCompany(request.company());
        biz.setBusinessNo(request.businessNo());
        biz.setCeoName(request.ceoName());
        biz.setService(request.service());
        biz.setServiceCode(request.serviceCode());
        biz.setItem(request.item());
        biz.setItemCode(request.itemCode());
        biz.setComAddress(request.comAddress());
        if (request.bizLicenseFile() != null) {
            biz.setBizLicenseFile(request.bizLicenseFile());
        }
        biz.setUpdatedAt(now);
        memberRepository.saveBusiness(biz);
    }
}
```

- [ ] **Step 2: GetMemberDetailUseCase — MemberBusinessDetailResponse에 코드 포함**

`src/main/java/com/sfood/automation/member/application/usecase/GetMemberDetailUseCase.java`의 `business` 생성 블록을 수정:

```java
business = memberRepository.findBusinessByMemNo(memNo)
    .map(entity -> new MemberBusinessDetailResponse(
        entity.getCompany(),
        entity.getBusinessNo(),
        entity.getCeoName(),
        entity.getService(),
        entity.getServiceCode(),
        entity.getItem(),
        entity.getItemCode(),
        entity.getComAddress(),
        entity.getBizLicenseFile()))
    .orElse(null);
```

- [ ] **Step 3: 컴파일 확인**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/sfood/automation/member/application/usecase/UpdateMemberBusinessUseCase.java \
        src/main/java/com/sfood/automation/member/application/usecase/GetMemberDetailUseCase.java
git commit -m "feat: UpdateMemberBusinessUseCase, GetMemberDetailUseCase — serviceCode/itemCode 저장 및 응답"
```

---

## Task 7: FetchNewMembersUseCase / UploadMembersUseCase — 배치·업로드 코드 맵핑

**Files:**
- Modify: `src/main/java/com/sfood/automation/member/application/usecase/FetchNewMembersUseCase.java`
- Modify: `src/main/java/com/sfood/automation/member/application/usecase/UploadMembersUseCase.java`

- [ ] **Step 1: FetchNewMembersUseCase — CommonCodeMappingService 주입 및 toBizEntity() 수정**

`FetchNewMembersUseCase.java`의 import에 추가:

```java
import com.sfood.automation.common_code.application.usecase.CommonCodeMappingService;
```

생성자에 `CommonCodeMappingService` 추가:

```java
private final CommonCodeMappingService commonCodeMappingService;

public FetchNewMembersUseCase(MemberRepository memberRepository,
                               CommonCodeMappingService commonCodeMappingService) {
    this.memberRepository = memberRepository;
    this.commonCodeMappingService = commonCodeMappingService;
}
```

`toBizEntity()` 메서드 수정:

```java
private GodoMemberBusinessEntity toBizEntity(CrawledMember m, String now, boolean isNew) {
    GodoMemberBusinessEntity entity = new GodoMemberBusinessEntity();
    entity.setMemNo(m.memNo());
    entity.setCompany(m.company());
    entity.setBusinessNo(m.businessNo());
    entity.setCeoName(m.ceoName());
    entity.setService(m.service());
    entity.setServiceCode(commonCodeMappingService.findServiceCode(m.service()).orElse(null));
    entity.setItem(m.item());
    entity.setItemCode(commonCodeMappingService.findItemCode(m.item()).orElse(null));
    entity.setComAddress(m.comAddress());
    entity.setUpdatedAt(now);
    if (isNew) {
        entity.setCreatedAt(now);
    }
    return entity;
}
```

- [ ] **Step 2: UploadMembersUseCase — CommonCodeMappingService 주입 및 코드 맵핑 적용**

`UploadMembersUseCase.java`의 import에 추가:

```java
import com.sfood.automation.common_code.application.usecase.CommonCodeMappingService;
```

생성자에 `CommonCodeMappingService` 추가:

```java
private final CommonCodeMappingService commonCodeMappingService;

public UploadMembersUseCase(MemberRowNormalizer normalizer,
                             MemberRepository memberRepository,
                             CommonCodeMappingService commonCodeMappingService) {
    this.normalizer = normalizer;
    this.memberRepository = memberRepository;
    this.commonCodeMappingService = commonCodeMappingService;
}
```

`execute()` 내 biz 엔티티 생성 직후 (`biz.setUpdatedAt(now);` 앞에) 코드 맵핑 추가:

```java
GodoMemberBusinessEntity biz = new GodoMemberBusinessEntity();
biz.setMemNo(memNo);
biz.setCreatedAt(now);
normalizer.applyBusinessFields(biz, row, request.type());
biz.setServiceCode(commonCodeMappingService.findServiceCode(biz.getService()).orElse(null));
biz.setItemCode(commonCodeMappingService.findItemCode(biz.getItem()).orElse(null));
biz.setUpdatedAt(now);
```

- [ ] **Step 3: FetchNewMembersUseCaseTest — 생성자 수정**

`FetchNewMembersUseCase` 생성자가 `(MemberRepository, HashUtil)` → `(MemberRepository, HashUtil, CommonCodeMappingService)`로 변경된다.

`src/test/java/com/sfood/automation/member/application/usecase/FetchNewMembersUseCaseTest.java` 수정:

```java
import com.sfood.automation.common_code.application.usecase.CommonCodeMappingService;
import java.util.Optional;

// 클래스 상단 필드
private final MemberRepository memberRepository = mock(MemberRepository.class);
private final HashUtil hashUtil = mock(HashUtil.class);
private final CommonCodeMappingService commonCodeMappingService = mock(CommonCodeMappingService.class);
private final FetchNewMembersUseCase useCase =
    new FetchNewMembersUseCase(memberRepository, hashUtil, commonCodeMappingService);
```

또한 `setUp` 또는 각 테스트에 mock 기본 동작 추가 (빈 Optional 반환):

```java
// should_insert_new_member_and_business_for_b2b_group() 내 추가
when(commonCodeMappingService.findServiceCode(any())).thenReturn(Optional.empty());
when(commonCodeMappingService.findItemCode(any())).thenReturn(Optional.empty());
```

- [ ] **Step 4: 컴파일 확인**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/sfood/automation/member/application/usecase/FetchNewMembersUseCase.java \
        src/main/java/com/sfood/automation/member/application/usecase/UploadMembersUseCase.java \
        src/test/java/com/sfood/automation/member/application/usecase/FetchNewMembersUseCaseTest.java
git commit -m "feat: FetchNewMembersUseCase, UploadMembersUseCase — 업태/업종 텍스트→코드 자동 맵핑"
```

---

## Task 8: Req 11 — 공통코드 조회 API useYn 필터 추가

**Files:**
- Modify: `src/main/java/com/sfood/automation/common_code/application/usecase/CommonCodeUseCase.java`
- Modify: `src/main/java/com/sfood/automation/common_code/presentation/CommonCodeController.java`
- Modify: `src/test/java/com/sfood/automation/common_code/presentation/CommonCodeControllerTest.java`

(CommonCodeJpaRepository의 `findByGroupCodeAndUseYnOrderByCodeAsc`는 Task 3 Step 3에서 이미 추가됨)

- [ ] **Step 1: CommonCodeControllerTest — 실패 테스트 추가**

`CommonCodeControllerTest.java`에 테스트 메서드 추가:

```java
@Test
void should_return_active_codes_only_when_useYn_Y() throws Exception {
    when(commonCodeUseCase.findAll("SERVICE", "Y")).thenReturn(new CommonCodeListResponse(List.of(
        new CodeItem(1, "SERVICE", "업태", "046", "식품", null, "Y", "2026-07-01", "2026-07-01"))));

    mockMvc.perform(get("/api/common-codes")
                    .param("groupCode", "SERVICE")
                    .param("useYn", "Y")
                    .session(session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codes[0].code").value("046"));
}
```

또한 기존 `should_return_200_with_code_list()` 테스트를 수정해 2인수 시그니처에 맞춘다:

```java
@Test
void should_return_200_with_code_list() throws Exception {
    when(commonCodeUseCase.findAll(null, null)).thenReturn(new CommonCodeListResponse(List.of(
        new CodeItem(1, "SERVICE", "업태", "001", "도소매", null, "Y", "2026-07-01", "2026-07-01"))));

    mockMvc.perform(get("/api/common-codes").session(session()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codes[0].groupCode").value("SERVICE"));
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./mvnw test -pl . -Dtest=CommonCodeControllerTest -q 2>&1 | tail -5
```

Expected: FAIL (findAll 시그니처 미변경)

- [ ] **Step 3: CommonCodeUseCase.findAll() 시그니처 변경**

```java
public CommonCodeListResponse findAll(String groupCode, String useYn) {
    List<CommonCodeEntity> entities;
    if (groupCode != null && useYn != null) {
        entities = commonCodeJpaRepository.findByGroupCodeAndUseYnOrderByCodeAsc(groupCode, useYn);
    } else if (groupCode != null) {
        entities = commonCodeJpaRepository.findByGroupCodeOrderByCodeAsc(groupCode);
    } else {
        entities = commonCodeJpaRepository.findAllByOrderByGroupCodeAscCodeAsc();
    }
    return CommonCodeListResponse.from(entities);
}
```

- [ ] **Step 4: CommonCodeController — useYn RequestParam 추가**

```java
@GetMapping
public ResponseEntity<CommonCodeListResponse> findAll(
        @RequestParam(required = false) String groupCode,
        @RequestParam(required = false) String useYn) {
    return ResponseEntity.ok(commonCodeUseCase.findAll(groupCode, useYn));
}
```

- [ ] **Step 5: 테스트 통과 확인**

```bash
./mvnw test -pl . -Dtest=CommonCodeControllerTest -q 2>&1 | tail -5
```

Expected: `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/sfood/automation/common_code/application/usecase/CommonCodeUseCase.java \
        src/main/java/com/sfood/automation/common_code/presentation/CommonCodeController.java \
        src/test/java/com/sfood/automation/common_code/presentation/CommonCodeControllerTest.java
git commit -m "feat: GET /api/common-codes — useYn 필터 파라미터 추가 (Req 11)"
```

---

## Task 9: 전체 테스트 통과 확인 및 수정

**Files:**
- 실패하는 테스트 파일들 (실행 후 결정)

- [ ] **Step 1: 전체 테스트 실행**

```bash
./mvnw test -q 2>&1 | tail -20
```

- [ ] **Step 2: 실패 테스트 분석 및 수정**

예상 실패 지점 및 수정 방법:

**FetchNewMembersUseCaseIntegrationTest** — `CommonCodeMappingService` 빈 필요
- 테스트 컨텍스트가 `CommonCodeMappingService`를 로드해야 함
- 통합 테스트는 Spring 컨텍스트 전체 로드이므로 자동으로 포함됨
- 다만 `@PostConstruct`의 `loadCache()`가 DB에 공통코드 없어도 빈 Map으로 폴백하므로 추가 설정 불필요

**UploadMembersUseCase 관련 테스트** — 생성자 파라미터 변경
- `UploadMembersUseCase`를 직접 생성하는 테스트가 있으면 `CommonCodeMappingService` Mock 추가 필요
- 해당 테스트 파일을 찾아: `grep -rn "new UploadMembersUseCase" src/test`
- Mock 추가: `@MockBean CommonCodeMappingService commonCodeMappingService;`

**UpdateMemberBusinessUseCaseIntegrationTest** — DTO record 생성자 인수 변경
- `MemberBusinessUpdateRequest` 생성 코드를 `serviceCode`, `itemCode` null 포함으로 수정

**MemberBusinessDetailResponse 생성 위치** — record 인수 부족
- `GetMemberDetailUseCase` 외 다른 곳에서 직접 생성하면 수정 필요
- 확인: `grep -rn "new MemberBusinessDetailResponse" src`

- [ ] **Step 3: 테스트 재실행 — 전체 통과 확인**

```bash
./mvnw test -q 2>&1 | grep -E "Tests run:|BUILD"
```

Expected:
```
Tests run: NNN, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 4: 최종 Commit**

```bash
git add -A
git commit -m "fix: 전체 테스트 통과 — serviceCode/itemCode 추가에 따른 테스트 수정"
```
