package com.sfood.automation.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.auth.domain.model.UserAccount;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
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
@Import(UserAccountRepositoryImpl.class)
class UserAccountRepositoryImplTest {

    @Autowired
    private UserAccountJpaRepository userAccountJpaRepository;

    @Autowired
    private UserAccountRepositoryImpl userAccountRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @BeforeEach
    void setUp() {
        userAccountJpaRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userAccountJpaRepository.deleteAll();
    }

    @Test
    void should_save_and_find_user_account() {
        Instant now = Instant.parse("2026-04-01T00:00:00Z");
        String email = "user-repo-" + System.nanoTime() + "@s-food.com";

        UserAccount saved = userAccountRepository.save(new UserAccount(
            null,
            email,
            "Y",
            "N",
            now,
            now,
            now
        ));

        assertThat(saved.getId()).isNotNull();
        assertThat(userAccountRepository.findByEmail(email)).isPresent();
        assertThat(userAccountRepository.findAllEmailNotificationEnabled()).hasSize(1);
    }
}
