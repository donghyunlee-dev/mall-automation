package com.sfood.automation.member.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.member.application.dto.MemberUploadCompleteResponse;
import com.sfood.automation.member.application.dto.MemberUploadRequest;
import com.sfood.automation.member.application.upload.MemberRowFixture;
import com.sfood.automation.member.domain.enums.MemberUploadType;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerJpaRepository;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class BulkUploadPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(BulkUploadPerformanceTest.class);
    private static final int ROW_COUNT = 20_000;
    private static final long THRESHOLD_MS = 10_000;

    @Autowired
    private UploadMembersUseCase uploadMembersUseCase;

    @Autowired
    private GodoMemberJpaRepository godoMemberJpaRepository;

    @Autowired
    private GodoMemberBusinessJpaRepository godoMemberBusinessJpaRepository;

    @Autowired
    private MemberPartnerJpaRepository memberPartnerJpaRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @BeforeEach
    void setUp() {
        memberPartnerJpaRepository.deleteAll();
        godoMemberBusinessJpaRepository.deleteAll();
        godoMemberJpaRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        memberPartnerJpaRepository.deleteAll();
        godoMemberBusinessJpaRepository.deleteAll();
        godoMemberJpaRepository.deleteAll();
    }

    @Test
    void should_insert_20000_rows_within_10_seconds() {
        List<List<Object>> rows = new ArrayList<>(ROW_COUNT);
        for (int i = 1; i <= ROW_COUNT; i++) {
            List<Object> row = MemberRowFixture.listRow();
            row.set(0, (long) i);           // 고유 회원번호
            row.set(8, "perfuser" + i);     // 고유 아이디
            rows.add(row);
        }

        long startMs = System.currentTimeMillis();

        MemberUploadCompleteResponse response = uploadMembersUseCase.execute(
            new MemberUploadRequest(MemberUploadType.LIST, "perf-test.xlsx", false, rows)
        );

        long elapsedMs = System.currentTimeMillis() - startMs;
        log.info("20,000건 bulk insert 완료: {}ms (godo_member={}, biz={})",
            elapsedMs,
            godoMemberJpaRepository.count(),
            godoMemberBusinessJpaRepository.count()
        );

        assertThat(response.totalInserted()).isEqualTo(ROW_COUNT);
        assertThat(response.totalUpdated()).isZero();
        assertThat(response.totalFailed()).isZero();
        assertThat(godoMemberJpaRepository.count()).isEqualTo(ROW_COUNT);
        assertThat(godoMemberBusinessJpaRepository.count()).isEqualTo(ROW_COUNT); // group_sno=9 → B2B → hasBusiness
        assertThat(elapsedMs).isLessThan(THRESHOLD_MS);
    }
}
