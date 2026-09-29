package com.sfood.automation.member.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.member.application.dto.MemberUploadCompleteResponse;
import com.sfood.automation.member.application.dto.MemberUploadRequest;
import com.sfood.automation.member.domain.enums.MemberUploadType;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberEntity;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerJpaRepository;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 실제 고도몰 list 엑셀에서 추출한 행(JSON)을 업로드 파이프라인에 통과시키는 수동 검증 테스트.
 * 실데이터(PII)는 저장소에 커밋하지 않으므로 환경변수로 JSON 경로를 받을 때만 실행된다.
 *
 * 실행: MEMBER_SAMPLE_JSON=$PWD/build/member-list-sample-100.json ./gradlew test \
 *       --tests 'com.sfood.automation.member.application.usecase.MemberUploadRealFileVerificationTest'
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "MEMBER_SAMPLE_JSON", matches = ".+")
class MemberUploadRealFileVerificationTest {

    @Autowired
    private UploadMembersUseCase uploadMembersUseCase;

    @Autowired
    private GodoMemberJpaRepository godoMemberJpaRepository;

    @Autowired
    private GodoMemberBusinessJpaRepository godoMemberBusinessJpaRepository;

    @Autowired
    private MemberPartnerJpaRepository memberPartnerJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Test
    void should_upload_real_list_rows_and_store_encrypted() throws Exception {
        Path samplePath = Path.of(System.getenv("MEMBER_SAMPLE_JSON"));
        List<List<Object>> rows = new ObjectMapper().readValue(
            Files.readString(samplePath), new TypeReference<>() {});

        MemberUploadCompleteResponse response = uploadMembersUseCase.execute(
            new MemberUploadRequest(MemberUploadType.LIST, "godomall-member-list.xlsx", false, rows));

        assertThat(response.totalFailed()).isEqualTo(0);
        assertThat(response.failedRows()).isEmpty();
        assertThat(godoMemberJpaRepository.count()).isEqualTo(rows.size());

        for (List<Object> row : rows) {
            Long memNo = ((Number) row.get(0)).longValue();
            GodoMemberEntity member = godoMemberJpaRepository.findByMemNo(memNo).orElseThrow();

            // 날짜: 시리얼 → "YYYY-MM-DD HH:MM:SS", 제로 날짜 → null
            assertThat(member.getEntryDt()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
            if (member.getLastLoginDt() != null) {
                assertThat(member.getLastLoginDt()).doesNotStartWith("0000");
            }
            if (member.getBirthDt() != null) {
                assertThat(member.getBirthDt()).doesNotStartWith("0000");
            }

            // 암호화 컬럼: 주소가 있으면 DB에는 평문이 아닌 암호문이어야 한다
            if (member.getAddress() != null) {
                String storedAddress = jdbcTemplate.queryForObject(
                    "SELECT address FROM godo_member WHERE mem_no = " + memNo, String.class);
                assertThat(storedAddress).isNotEqualTo(member.getAddress());
            }

            // 사업자 등급이면 business 행 존재 + business_no 평문 확인
            if (member.getGroupSno() == 2) {
                var business = godoMemberBusinessJpaRepository.findByMemNo(memNo).orElseThrow();
                assertThat(business.getBusinessNo()).matches("\\d{3}-\\d{2}-\\d{5}");
            }
        }
    }
}
