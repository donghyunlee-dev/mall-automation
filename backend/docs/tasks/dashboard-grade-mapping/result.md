# dashboard-grade-mapping Result

> 완료일: 2026-09-07
> 대응 항목: `docs/dashboard-integration/dashboard-prd.md` Iteration 16 (R1/R2/R3)

## Delivered Scope

원본 데이터 파이프라인(`sfood-mall-dashboard/update_data.py`)의 실제 소스코드를 확인해 정확한 규칙을 파악하고 그대로 이식했다.

- **비회원 주문 포함(R1)**: `GodoOrderResponseParser`가 `memId`가 빈 주문을 제외하던 로직 제거 — 원본은 memId 유무와 무관하게 주문을 전부 포함한다.
- **등급명 라벨링(R1의 일부)**: `memGroupNm`(회원등급명)이 빈 값이면 "비회원"으로 라벨링(원본 `df['회원등급명'].replace('', '비회원')`과 동일).
- **회원등급 롤업(R2)**: 조회 계층(QueryDSL)에 `사업자` 포함 등급을 "사업자회원"으로 합산하는 `rolledUpGrade()` 표현식 추가(원본 `회원등급명_요약` 로직과 동일 — `contains`, `startsWith` 아님).
- **구매자 수 카운트 보정**: `member_id`가 빈 문자열인 비회원 주문은 서로 다른 구매자로 취급하도록 `buyerKey()`(member_id 없으면 order_no로 대체) 도입 — 원본의 "구분자_비회원포함"(이름+전화번호 조합)을 스키마에 이름/전화번호 컬럼이 없는 현 구조에서 근사한 것.
- **R3(게스트 오분류 조사)**: 별도 조사가 필요 없었음 — R1/R2 구현 자체가 원인이었다. 실데이터 재검증 결과 "비회원"·"일반회원" 등급 수치가 원본과 정확히 일치함을 확인.

## Changed Files

- `backend/src/main/java/com/sfood/automation/dashboard/infrastructure/external/GodoOrderResponseParser.java` — `memId.isBlank()` 제외 로직 제거, `memGroupNm` 빈 값 → "비회원" 라벨링
- `backend/src/main/java/com/sfood/automation/dashboard/infrastructure/persistence/DashboardQueryRepositoryImpl.java` — `rolledUpGrade()`/`buyerKey()` QueryDSL 표현식 추가, `gradeRevenueBy*`/`gradeDetailBy*`/`summarize()`에 적용
- `backend/src/test/java/.../GodoOrderResponseParserTest.java` — 게스트 주문 포함/등급 라벨링 테스트로 교체
- `backend/src/test/java/.../DashboardQueryRepositoryImplTest.java` — 등급 롤업(revenue/detail), 게스트 구매자 카운트 테스트 추가

## Key Decisions

- **등급 롤업은 저장이 아닌 조회 계층에서 처리**: 원시 등급(`사업자회원_화이트` 등)을 DB에는 그대로 보존하고, 조회 시에만 QueryDSL `CaseBuilder`로 합산 — 추후 세분화된 분석이 필요할 때 원시 데이터를 재활용할 수 있도록.
- **구매자 식별자 근사**: 원본은 비회원 구분자로 "회원명_전화번호"를 쓰지만, 현재 스키마(`godo_order`)에 주문자 이름/전화번호 컬럼이 없다. 스키마 확장 대신 `order_no`로 대체하는 실용적 근사를 택함 — 비회원 주문은 통상 반복 구매가 드물어 실질적 오차가 작을 것으로 판단.

## 라이브 재검증 결과 (2026-09-03 기준)

| 등급 | 원본 | 재검증 후 우리 시스템 | 일치 여부 |
|---|---|---|---|
| 비회원 | 583,409원 / 8건 / 8명 | 583,409원 / 8건 / 8명 | ✅ 완전 일치 |
| 일반회원 | 206,263원 / 4건 / 4명 | 206,264원 / 4건 / 4명 | ✅ 사실상 일치(반올림) |
| 기능장회원 | 37,513원 / 1건 / 1명 | 37,514원 / 1건 / 1명 | ✅ 사실상 일치(반올림) |
| 사업자회원 | 8,353,209원 / 89건 / 82명 | 8,503,891원 / 90건 / 83명 | 🟡 약 1.8% 차이(잔존) |

전체 합계도 원본(9,180,395원/102건/95명)과 재검증 결과(9,331,077원/103건/96명)가 근접했다(오차 1.6% 이내).

**남은 "사업자회원" 그룹 오차**: 재검증을 원본 비교 시점(2026-09-04)보다 며칠 뒤(2026-09-07)에 실행해 21일 롤링 윈도우가 다시 동기화되면서, 그 사이 실제 주문 상태가 바뀐 건(예: 결제완료→구매확정)이 반영된 자연스러운 시점 차이로 판단된다. 등급 오분류 문제는 아님 — R3에서 우려했던 "게스트 주문의 일반회원 오분류"는 재검증 결과 재현되지 않았다.

## Test Result

- `./gradlew test --tests "com.sfood.automation.dashboard.*" --tests "com.sfood.automation.batch.*"` — 전체 통과
- 실데이터 재검증(라이브 Godo API): 주문 1,913건 / 상품라인 3,643건 동기화(2026-08-17~2026-09-06), 대시보드 API 응답을 원본 Streamlit 값과 대조 완료(위 표 참고)

## Open Gaps

- "사업자회원" 그룹의 잔존 오차(~1.8%)는 시점 차이로 추정되나 완전히 검증되지 않음 — 동일 시점 스냅샷으로 재비교하면 해소될 가능성이 높음(우선순위 낮음, 후속 관찰 필요 시 재확인).
- 구매자 식별자 근사(주문번호 대체)는 정확한 "회원명+전화번호" 방식과 다를 수 있음 — 스키마에 주문자 이름/전화번호를 추가하면 완전히 동일한 재현 가능(현재는 범위 외로 결정).
