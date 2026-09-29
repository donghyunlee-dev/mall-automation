# 업태/업종 코드 맵핑 및 공통코드 조회 API 설계

**작성일**: 2026-07-03  
**브랜치**: feature/member-batch-groupsno-filter-removal  
**요구사항**: Req 10 (업태/업종 코드 맵핑), Req 11 (공통코드 조회 API)

---

## 1. 목적

| 요구사항 | 내용 |
|---|---|
| Req 10 | `godo_member_business`, `godo_member_business_log`에 `service_code`, `item_code` 컬럼 추가. 배치 수집·엑셀 업로드 시 업태/업종 텍스트를 `common_code` 테이블 기준으로 자동 맵핑. 맵핑 실패 시 `_code` 컬럼 NULL, 텍스트는 그대로 저장. |
| Req 11 | `GET /api/common-codes?groupCode=X&useYn=Y` — 활성 코드만 반환 (프론트엔드 업태/업종 선택 UI용). |

---

## 2. 데이터 구조

### 2-1. Excel 원본 (`docs/analysis/common_code.xlsx`)

| 구분 | group_code | 건수 | 코드 형식 |
|---|---|---|---|
| 업태 | `SERVICE` | 80건 | `001` ~ `079` + `999` |
| 업종 | `ITEM` | 6,393건 | `0001` ~ `6393+` |

`group_name`: 업태 → `업태`, 업종 → `업종`

### 2-2. common_code 테이블 (기존)

```
id, group_code, group_name, code, code_name, note, use_yn, created_at, updated_at
UNIQUE (group_code, code)
```

### 2-3. 신규 컬럼 (godo_member_business, godo_member_business_log)

```sql
service_code TEXT   -- nullable. 업태 공통코드 (예: '046')
item_code    TEXT   -- nullable. 업종 공통코드 (예: '0046')
```

---

## 3. 아키텍처 결정

### 초기 데이터 로딩

- `data.sql` (SQLite): `INSERT OR IGNORE INTO common_code ...`
- `data-postgresql.sql` (PostgreSQL): `INSERT INTO common_code ... ON CONFLICT (group_code, code) DO NOTHING`
- Python 스크립트로 Excel → SQL 변환 후 두 파일 모두 추가
- 기존 `common_status_config` 초기 데이터와 동일한 패턴

### 텍스트 → 코드 맵핑

`CommonCodeMappingService` (Application layer, `common_code` 패키지):
- 앱 시작 시 `use_yn='Y'` 코드 전체를 `Map<String, String>`(trimmed lowercase name → code)으로 캐시
- `findServiceCode(String text)` → `Optional<String>`
- `findItemCode(String text)` → `Optional<String>`
- 매칭 전략: trim + lowercase exact match
- 매칭 실패 → `Optional.empty()` (code 컬럼 null, 텍스트는 그대로 저장)

### Req 11 API 설계

기존 `GET /api/common-codes?groupCode=X`에 선택적 `useYn` 파라미터 추가 (기존 동작 유지):

| 호출 방식 | 대상 | 반환 |
|---|---|---|
| `?groupCode=SERVICE` | 관리자 CRUD | 전체 코드 |
| `?groupCode=SERVICE&useYn=Y` | 프론트엔드 | 활성 코드만 |

---

## 4. 변경 파일 목록

### 4-1. 스키마 (4곳)

| 파일 | 변경 내용 |
|---|---|
| `src/main/resources/schema.sql` | `godo_member_business`, `godo_member_business_log`에 `service_code`, `item_code` 컬럼 추가 |
| `src/main/resources/postgresql/schema-member-postgresql.sql` | 동일 — ALTER TABLE IF EXISTS 구문 |
| `SqliteSchemaMigration.java` | `ensureGodoMemberBusinessCodeColumns()` 메서드 추가 (기존 SQLite 인스턴스 migration) |
| `SharedSqliteTestDatabase.java` | `ensureSchemaCompatibility()`에 컬럼 추가 |

### 4-2. 초기 데이터 (2곳)

| 파일 | 변경 내용 |
|---|---|
| `src/main/resources/data.sql` | 업태 80건 + 업종 6,393건 INSERT OR IGNORE |
| `src/main/resources/postgresql/data-postgresql.sql` | 동일 — ON CONFLICT DO NOTHING |

### 4-3. 신규 클래스 (1개)

| 파일 | 역할 |
|---|---|
| `common_code/application/usecase/CommonCodeMappingService.java` | 공통코드 인메모리 캐시 + 텍스트 맵핑 |

### 4-4. Entity 변경 (2곳)

| 파일 | 추가 필드 |
|---|---|
| `GodoMemberBusinessEntity.java` | `serviceCode`, `itemCode` |
| `GodoMemberBusinessLogEntity.java` | `serviceCode`, `itemCode` |

### 4-5. DTO 변경 (3곳)

| 파일 | 추가 필드 |
|---|---|
| `MemberBusinessUpdateRequest.java` | `serviceCode`, `itemCode` |
| `MemberBusinessDetailResponse.java` | `serviceCode`, `itemCode` |
| `MemberBusinessLogListResponse.LogItem` | `serviceCode`, `itemCode` |

### 4-6. UseCase / Repository 변경 (5곳)

| 파일 | 변경 내용 |
|---|---|
| `UpdateMemberBusinessUseCase.java` | request의 `serviceCode`, `itemCode` → entity·log 저장 |
| `FetchNewMembersUseCase.java` | `toBizEntity()` 내 `CommonCodeMappingService` 호출 추가 |
| `UploadMembersUseCase.java` | 사업자 엔티티 생성 시 코드 맵핑 추가 |
| `MemberRepositoryImpl.java` | raw JDBC INSERT/UPDATE SQL에 `service_code`, `item_code` 추가 |
| `GetMemberDetailUseCase.java` | `MemberBusinessDetailResponse`에 코드 필드 포함 확인 |

### 4-7. 공통코드 API 변경 (3곳, Req 11)

| 파일 | 변경 내용 |
|---|---|
| `CommonCodeJpaRepository.java` | `findByGroupCodeAndUseYnOrderByCodeAsc()` 추가 |
| `CommonCodeUseCase.java` | `findAll(groupCode, useYn)` 시그니처 변경 |
| `CommonCodeController.java` | `@RequestParam(required=false) String useYn` 추가 |

---

## 5. 데이터 흐름

### 배치 수집 (FetchNewMembersUseCase)

```
CrawledMember.service() → CommonCodeMappingService.findServiceCode() → Optional<String>
CrawledMember.item()    → CommonCodeMappingService.findItemCode()    → Optional<String>
↓
GodoMemberBusinessEntity.setService(text)
GodoMemberBusinessEntity.setServiceCode(code or null)
GodoMemberBusinessEntity.setItem(text)
GodoMemberBusinessEntity.setItemCode(code or null)
```

### 수동 수정 (UpdateMemberBusinessUseCase)

```
MemberBusinessUpdateRequest { service, serviceCode, item, itemCode }
↓
GodoMemberBusinessLogEntity (기존 스냅샷 — serviceCode 포함)
GodoMemberBusinessEntity (신규 값 저장)
```

### 공통코드 조회 (프론트엔드)

```
GET /api/common-codes?groupCode=SERVICE&useYn=Y
→ CommonCodeUseCase.findAll("SERVICE", "Y")
→ findByGroupCodeAndUseYnOrderByCodeAsc("SERVICE", "Y")
→ [{ id, groupCode, code, codeName, ... }, ...]
```

---

## 6. 오류 처리

- `CommonCodeMappingService`: DB 조회 실패 시 빈 Map으로 폴백 (로그 경고 출력). 맵핑 실패는 예외가 아닌 `Optional.empty()` 반환.
- `UpdateMemberBusinessUseCase`: `serviceCode`/`itemCode`가 null이면 null로 저장 (허용).

---

## 7. 테스트 범위

| 테스트 | 내용 |
|---|---|
| `CommonCodeMappingServiceTest` | 캐시 로드, 정확한 매칭, 대소문자 처리, 미매칭 시 empty |
| `UpdateMemberBusinessUseCaseIntegrationTest` | serviceCode/itemCode 저장·로그 검증 |
| `FetchNewMembersUseCaseIntegrationTest` | 배치 수집 시 코드 자동 맵핑 검증 |
| `CommonCodeControllerTest` | useYn=Y 파라미터로 필터링 검증 |
| `SqliteBootstrapRunnerTest` | 테이블 수 카운트 변경 없음 (컬럼만 추가) |
