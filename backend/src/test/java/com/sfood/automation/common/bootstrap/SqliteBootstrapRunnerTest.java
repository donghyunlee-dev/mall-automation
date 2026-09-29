package com.sfood.automation.common.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class SqliteBootstrapRunnerTest {

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @Test
    void should_initialize_shared_database_when_application_starts() throws Exception {
        assertThat(Files.exists(SharedSqliteTestDatabase.DATABASE_PATH)).isTrue();
        assertThat(SharedSqliteTestDatabase.DATABASE_PATH.getFileName().toString())
            .isEqualTo("sfood-mall-automation_test.db");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SharedSqliteTestDatabase.DATABASE_PATH);
             Statement statement = connection.createStatement()) {
            assertThat(queryForString(statement, "PRAGMA journal_mode")).isEqualToIgnoringCase("wal");
            assertThat(queryForCount(statement, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"))
                .isEqualTo(18);
            assertThat(queryForCount(statement,
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='common_status_config'"))
                .isEqualTo(1);
            assertThat(queryForCount(statement,
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='batch_schedule_config'"))
                .isEqualTo(1);
            assertThat(queryForCount(statement,
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='users'"))
                .isEqualTo(1);
            assertThat(queryForCount(statement,
                "SELECT COUNT(*) FROM common_status_config WHERE scope IN ('GENERAL', 'EXCLUSIVE')"))
                .isEqualTo(2);
            assertThat(queryForCount(statement,
                "SELECT COUNT(*) FROM batch_schedule_config WHERE job_name = 'product-sync'"))
                .isEqualTo(1);
        }
    }

    private String queryForString(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private long queryForCount(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
