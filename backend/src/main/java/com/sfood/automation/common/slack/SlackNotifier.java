package com.sfood.automation.common.slack;

import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SlackNotifier {

    private static final Logger log = LoggerFactory.getLogger(SlackNotifier.class);

    private final RestClient restClient;
    private final String webhookUrl;

    public SlackNotifier(RestClient.Builder builder, @Value("${app.slack.webhook-url:}") String webhookUrl) {
        this.restClient = builder.build();
        this.webhookUrl = webhookUrl;
    }

    public void send(String location, String message) {
        sendError(location, message, OffsetDateTime.now());
    }

    public void sendInfo(String location, String message) {
        sendInfo(location, message, OffsetDateTime.now());
    }

    void send(String location, String message, OffsetDateTime occurredAt) {
        sendError(location, message, occurredAt);
    }

    void sendInfo(String location, String message, OffsetDateTime occurredAt) {
        sendPayload("[INFO] ", location, message, occurredAt);
    }

    private void sendError(String location, String message, OffsetDateTime occurredAt) {
        sendPayload("[ERROR] ", location, message, occurredAt);
    }

    private void sendPayload(String level, String location, String message, OffsetDateTime occurredAt) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Slack webhook URL is not configured");
            return;
        }

        try {
            restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SlackPayload(level + location + "\n" + message + "\n발생시각: " + occurredAt))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception exception) {
            log.warn("Failed to send Slack notification", exception);
        }
    }

    private record SlackPayload(String text) {
    }
}
