# Spec — 대시보드 등급별 매출 조회 PostgreSQL 오류 수정

> 작성일: 2026-09-09
> 심각도: **P0(운영 장애)** — 4개 탭(일간/주간/월간/분기) 전부에서 대시보드 데이터 조회 실패

## 증상

운영 환경에서 대시보드 4개 탭이 전부 "대시보드 데이터를 불러오지 못했어요." 에러를 표시. 프론트 응답 확인 결과 `{"code": "INTERNAL_SERVER_ERROR"}`(500), 백엔드 로그에는 다음 PostgreSQL 오류가 남음:

```
column "goe1_0.member_grade" must appear in the GROUP BY clause
```

## 근본 원인

`DashboardQueryRepositoryImpl.gradeRevenueByOrderDate` / `gradeRevenueByShippedDate`가 등급 롤업("사업자회원_*" → "사업자회원")을 위해 `CaseBuilder`로 만든 `StringExpression`(CASE WHEN member_grade LIKE ... THEN ... ELSE member_grade END)을 **SELECT와 GROUP BY 양쪽에 동일 객체로 재사용**했다.

- SQLite: GROUP BY의 함수적 종속성을 엄격히 검증하지 않아 이 패턴이 그대로 동작함 — 로컬/CI 테스트(전부 SQLite 기준)를 통과.
- PostgreSQL: SELECT의 CASE 표현식과 GROUP BY의 CASE 표현식이 Hibernate/QueryDSL을 거치며 텍스트상 동일하게 렌더링되지 않아, PostgreSQL이 "member_grade가 GROUP BY에 없다"고 판단해 쿼리 자체를 거부(SQL 표준의 GROUP BY 함수적 종속성 규칙).
- 이 코드 경로는 운영에서만 쓰는 PostgreSQL로 실제 검증된 적이 없었다(`build.gradle`에 `testcontainers`/`spring-boot-testcontainers` 의존성은 있으나 실제 사용하는 테스트 클래스가 없어 전 구간이 SQLite로만 테스트됨).

## 영향 범위

- `GetDashboardGradeRevenueUseCase`(등급별 매출 비중 도넛 차트) — 4개 탭 전부에서 호출
- 프론트는 `Promise.all`로 여러 API를 한 번에 호출해, 이 API 하나만 실패해도 탭 전체가 "대시보드 데이터를 불러오지 못했어요."로 표시됨(다른 API는 정상이었음)

## 해결 방향

SQL 레벨 GROUP BY는 원시 컬럼(`member_grade`)으로만 수행(어떤 SQL 방언에서도 안전)하고, "사업자회원_*" 롤업은 애플리케이션(Java) 레벨에서 처리한다. `gradeDetailByOrderDate`/`ByShippedDate`(도넛 아닌 등급별 상세 테이블/피벗용)는 애초에 SQL GROUP BY 없이 Java에서 집계하는 방식이라 이 문제가 없었다 — 동일한 안전한 패턴을 적용한다.
