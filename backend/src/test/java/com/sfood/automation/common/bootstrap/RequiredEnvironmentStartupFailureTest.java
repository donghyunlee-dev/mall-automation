package com.sfood.automation.common.bootstrap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.SfoodMallAutomationBackendApplication;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

class RequiredEnvironmentStartupFailureTest {

    private static final Path DATABASE_PATH = Path.of("build/task09-missing-env.sqlite");

    @Test
    void should_fail_to_start_when_required_environment_variable_is_missing() throws Exception {
        Files.deleteIfExists(DATABASE_PATH);
        Map<String, Object> properties = new HashMap<>();
        properties.put("DB_PATH", DATABASE_PATH.toString());
        properties.put("MAIL_HOST", "smtp.gmail.com");
        properties.put("MAIL_USERNAME", "no-reply@s-food.com");
        properties.put("MAIL_PASSWORD", "");
        properties.put("GODO_BASE_URL", "https://example.test/godo");
        properties.put("GODO_PARTNER_KEY", "partner-key");
        properties.put("GODO_KEY", "godo-key");
        properties.put("WMS_LOGIN_URL", "https://example.test/wms/login");
        properties.put("WMS_STOCK_URL", "https://example.test/wms/stock");
        properties.put("WMS_USER_ID", "tester");
        properties.put("WMS_PASSWORD", "password");
        properties.put("WMS_CENTER_CODE", "OA110");
        properties.put("WMS_WAREHOUSE_CODE", "SLP001");
        properties.put("SLACK_WEBHOOK_URL", "https://hooks.slack.test/services/test");
        properties.put("OTP_SERVER_SECRET", "test-secret");
        properties.put("SFOOD_ADMIN_BASE_URL", "https://example.test/sfood-admin");
        properties.put("SFOOD_ADMIN_MANAGER_ID", "test-admin");
        properties.put("SFOOD_ADMIN_MANAGER_PW", "test-password");
        properties.put("app.batch.scheduler.auto-start", "false");
        properties.put("spring.main.web-application-type", "none");

        assertThatThrownBy(() -> new SpringApplicationBuilder(SfoodMallAutomationBackendApplication.class)
            .web(WebApplicationType.NONE)
            .properties(properties)
            .run())
            .hasRootCauseMessage("Missing required configuration: spring.mail.password");
    }
}
