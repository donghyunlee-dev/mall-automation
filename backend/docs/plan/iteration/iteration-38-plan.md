# Iteration 38 Plan — 파트너 코드 수정 API

> 작성일: 2026-06-11
> 의존: Iteration 37 (조회 API 완료, MemberRepository 포트 확립)
> 이후: 프론트엔드 Iteration 10 (회원 상세 페이지)
> 참조: `backend/docs/plan/member-management-ui-plan.md` 2-2·7-5

---

## 목표

B2B 거래처 회원의 ERP 연동 코드(`partner_code`)와 부가 거래처 정보를 수정하는 API를 구현한다.
`member_partner` 레코드가 없으면 신규 생성, 있으면 갱신(upsert)한다.

---

## 범위

### 신규 파일

```
backend/src/main/java/com/sfood/automation/member/
├── application/
│   ├── dto/
│   │   ├── UpdateMemberPartnerRequest.java
│   │   └── MemberPartnerResponse.java
│   └── usecase/
│       └── UpdateMemberPartnerUseCase.java
└── presentation/
    └── MemberPartnerController.java

backend/src/test/java/com/sfood/automation/member/
└── presentation/
    └── MemberPartnerControllerTest.java
```

### 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `member/domain/port/MemberRepository.java` | `savePartner(MemberPartnerEntity)`, `findPartnerByMemNo(Long)` 추가 |
| `member/infrastructure/persistence/MemberRepositoryImpl.java` | 위 구현 추가 |

---

## 구현 명세

### UpdateMemberPartnerRequest.java

```java
public record UpdateMemberPartnerRequest(
    @NotNull String partnerRegisteredFl,  // "y" | "n"
    String partnerCode,                   // null 허용 (거래처 미등록 시)
    String partnerName,
    @NotNull String taxInvoiceFl,         // "y" | "n"
    String duplicateCheck
) {}
```

**검증 규칙:**
- `partnerRegisteredFl = "y"` 이면 `partnerCode` 필수 (없으면 400)
- `partnerRegisteredFl`, `taxInvoiceFl` 허용값: "y" / "n" (그 외 400)

### MemberPartnerResponse.java

```java
public record MemberPartnerResponse(
    long memNo,
    String partnerRegisteredFl,
    String partnerCode,
    String partnerName,
    String taxInvoiceFl,
    String duplicateCheck,
    String updatedAt
) {}
```

---

### UpdateMemberPartnerUseCase.java

```
1. memNo로 godo_member 존재 확인 — 없으면 MemberNotFoundException (404)
2. member_partner findByMemNo
3. 존재하면 갱신, 없으면 신규 생성
4. 갱신 컬럼: partnerRegisteredFl, partnerCode, partnerName, taxInvoiceFl, duplicateCheck, updatedAt
5. 저장 후 MemberPartnerResponse 반환
```

트랜잭션: `@Transactional`

---

### MemberPartnerController.java

```
PUT /api/members/{memNo}/partner → updatePartner
```

응답: `200 OK` + `MemberPartnerResponse`

---

## 테스트 명세

### MemberPartnerControllerTest.java (`@WebMvcTest`)

| 시나리오 | 검증 |
|----------|------|
| 유효한 수정 요청 | 200, partnerCode 반영 확인 |
| partnerRegisteredFl="y" + partnerCode 없음 | 400 |
| 없는 memNo | 404 |
| partnerRegisteredFl 허용값 외 | 400 |

### 통합 테스트 (`@SpringBootTest`)

| 시나리오 | 검증 |
|----------|------|
| 파트너 신규 생성 | member_partner 레코드 존재 확인 |
| 파트너 갱신 | 변경된 partnerCode DB 반영 확인 |
| 갱신 후 상세 조회 | GET /api/members/{memNo} 응답의 partner 섹션에 반영 |
| LIST 업로드 후 파트너 수동 갱신 | 업로드 재실행 후 파트너 데이터 보존 확인 |

---

## 완료 기준

- `./gradlew test` — 위 모든 테스트 GREEN
- `./gradlew build` — 오류 없음
- PUT 요청 후 GET /api/members/{memNo} 응답에 partner 섹션 반영 확인
- 기존 전체 테스트 영향 없음

---

## 백엔드 전체 완료 기준 (Iteration 35~38)

모든 백엔드 Iteration(35~38)이 완료되면 다음 상태가 된다.

| 항목 | 상태 |
|------|------|
| DB 스키마 3테이블 | 생성 완료 |
| 암호화 모듈 | 단위 테스트 검증 완료 |
| 엑셀 업로드 파이프라인 | 실파일 테스트 완료 |
| 목록 조회 (서버 페이징·검색·필터) | 통합 테스트 완료 |
| 요약 카드 API | 완료 |
| 상세 조회 (복호화 평문) | 완료 |
| 파트너 코드 수정 | 완료 |
| API 명세 | `backend/docs/product/api-spec-member.md` 기준 |

이 상태에서 프론트엔드 Iteration 09(회원 목록)와 Iteration 10(상세·파트너 수정)을 착수할 수 있다.
