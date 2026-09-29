package com.sfood.automation.batch.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.batch.domain.enums.EmailSendType;
import com.sfood.automation.batch.domain.model.EmailSendLog;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EmailSendLogRepositoryImpl.class)
class EmailSendLogRepositoryImplTest {

    @Autowired
    private EmailSendLogJpaRepository emailSendLogJpaRepository;

    @Autowired
    private EmailSendLogRepositoryImpl emailSendLogRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @AfterEach
    void tearDown() {
        emailSendLogJpaRepository.deleteAll();
    }

    @Test
    void should_save_email_send_log() {
        Instant now = Instant.parse("2026-04-01T00:00:00Z");

        EmailSendLog saved = emailSendLogRepository.save(new EmailSendLog(
            null,
            EmailSendType.BATCH_RESULT,
            "user@s-food.com",
            "subject",
            false,
            null,
            now,
            now
        ));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.sendType()).isEqualTo(EmailSendType.BATCH_RESULT);
        assertThat(saved.isSuccess()).isFalse();
    }
}
