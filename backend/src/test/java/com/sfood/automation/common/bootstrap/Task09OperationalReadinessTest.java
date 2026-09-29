package com.sfood.automation.common.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class Task09OperationalReadinessTest {

    private static final Logger log = LoggerFactory.getLogger(Task09OperationalReadinessTest.class);
    private static final Path LOG_FILE = Path.of("logs/sfood-mall-automation.log");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AsyncTaskExecutor applicationTaskExecutor;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
        registry.add("app.batch.scheduler.auto-start", () -> "false");
    }

    @Test
    void should_run_work_on_virtual_thread() throws Exception {
        Future<String> future = applicationTaskExecutor.submit(() -> Thread.currentThread().getName());
        String threadName = future.get();

        assertThat(threadName.toLowerCase()).contains("virtual");
    }

    @Test
    void should_create_application_log_file() throws Exception {
        log.info("Task09 log file verification");
        for (int attempt = 0; attempt < 20 && !Files.exists(LOG_FILE); attempt++) {
            Thread.sleep(Duration.ofMillis(100));
        }
        assertThat(Files.exists(LOG_FILE)).isTrue();
    }

    @Test
    void should_keep_error_response_shape_consistent() throws Exception {
        mockMvc.perform(get("/api/goods"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
            .andExpect(jsonPath("$.message").value("Authentication required"))
            .andExpect(jsonPath("$.timestamp").exists());

        mockMvc.perform(post("/api/auth/email-verifications")
                .contentType("application/json")
                .content("""
                    {"email":"not-an-email"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").exists())
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void should_keep_seeded_data_counts() {
        assertThat(count("SELECT COUNT(*) FROM common_status_config")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM batch_schedule_config")).isGreaterThanOrEqualTo(1);
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }
}
