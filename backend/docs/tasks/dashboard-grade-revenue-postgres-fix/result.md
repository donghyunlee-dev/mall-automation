# 결과 — 대시보드 등급별 매출 조회 PostgreSQL 오류 수정

> 완료일: 2026-09-09

## 변경 내용

`DashboardQueryRepositoryImpl`:
- `gradeRevenueByOrderDate`/`gradeRevenueByShippedDate`: SQL `GROUP BY`를 CASE 표현식 대신 원시 컬럼 `ORDER.memberGrade`로 변경
- 신규 `rollUpGradeRevenue(List<Tuple>)` / `rollUpGradeName(String)`: 조회된 원시 등급 행을 Java에서 합산하며 "사업자회원_*" → "사업자회원" 롤업 수행
- 기존 `toGradeRevenue(StringExpression, List<Tuple>)` 헬퍼는 더 이상 쓰이지 않아 제거
- `rolledUpGrade()`(CASE 표현식)는 `buildGradeDetail`에서 계속 사용(그쪽은 GROUP BY 자체가 없어 문제가 없음) — 변경 없음

## 검증

- `./gradlew test --tests "*DashboardQueryRepositoryImplTest*"` — ✅ 12/12 통과(등급 롤업 합산 동작 기존과 동일함을 확인)
- `./gradlew test`(전체) — ✅ 510건, 실패 0 / 에러 0, 스킵 4(기존과 동일)

## 검증 한계

- 이 저장소/샌드박스 환경에 Docker가 없어(`testcontainers` 의존성은 있으나 미사용) **실제 PostgreSQL에 대해 직접 재현·검증하지 못했다.** 수정 방향은 원본 PostgreSQL 오류 메시지("member_grade가 GROUP BY에 없다")와 코드 패턴(CASE-in-GROUP-BY, SELECT/GROUP BY 동일 표현식 재사용) 분석으로 도출한 것이며, SQL 표준상 원시 컬럼 단독 GROUP BY는 모든 방언에서 항상 유효하므로 구조적으로 이 오류 자체가 재발할 수 없는 형태로 바꿨다.
- **배포 후 반드시 운영 환경에서 4개 탭 전체(특히 등급별 매출 비중 도넛)가 정상 조회되는지 확인 필요.**
- 재발 방지를 위해 `testcontainers-postgresql` 기반 통합 테스트를 추가해 이 종류의 SQLite/PostgreSQL 방언 차이를 CI에서 잡을 수 있게 하는 것을 권장(별도 작업으로 제안, 이번 핫픽스 범위 밖).
