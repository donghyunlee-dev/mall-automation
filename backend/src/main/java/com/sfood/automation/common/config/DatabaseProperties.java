package com.sfood.automation.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.database")
public record DatabaseProperties(String path) {
}
