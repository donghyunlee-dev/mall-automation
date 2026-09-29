package com.sfood.automation.common.config;

import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class RequiredEnvironmentValidator implements InitializingBean {

    private static final List<String> REQUIRED_KEYS = List.of(
        "spring.mail.host",
        "spring.mail.username",
        "spring.mail.password",
        "app.database.path",
        "app.godo.base-url",
        "app.godo.partner-key",
        "app.godo.key",
        "app.wms.login-url",
        "app.wms.stock-url",
        "app.wms.user-id",
        "app.wms.password",
        "app.wms.center-code",
        "app.wms.warehouse-code",
        "app.slack.webhook-url",
        "app.otp.server-secret",
        "app.sfood-admin.base-url",
        "app.sfood-admin.manager-id",
        "app.sfood-admin.manager-pw"
    );

    private final Environment environment;

    public RequiredEnvironmentValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        for (String key : REQUIRED_KEYS) {
            String value = environment.getProperty(key);
            if (value == null || value.isBlank() || value.contains("${")) {
                throw new IllegalStateException("Missing required configuration: " + key);
            }
        }
    }
}
