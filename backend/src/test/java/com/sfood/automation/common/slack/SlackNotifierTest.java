package com.sfood.automation.common.slack;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SlackNotifierTest {

    @Test
    void should_send_payload_with_text_field() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SlackNotifier notifier = new SlackNotifier(builder, "https://hooks.slack.com/services/test");

        server.expect(requestTo("https://hooks.slack.com/services/test"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"text\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("[ERROR] batch-job")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("sync failed")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("발생시각:")))
            .andRespond(withSuccess());

        notifier.send("batch-job", "sync failed");

        server.verify();
    }

    @Test
    void should_not_throw_when_webhook_request_fails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SlackNotifier notifier = new SlackNotifier(builder, "https://hooks.slack.com/services/test");

        server.expect(requestTo("https://hooks.slack.com/services/test"))
            .andRespond(withServerError());

        assertThatCode(() -> notifier.send("batch-job", "sync failed")).doesNotThrowAnyException();
    }

    @Test
    void should_send_info_payload_with_info_prefix() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SlackNotifier notifier = new SlackNotifier(builder, "https://hooks.slack.com/services/test");

        server.expect(requestTo("https://hooks.slack.com/services/test"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("[INFO] batch-job")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("수집한 데이터 로우 수: 10")))
            .andRespond(withSuccess());

        notifier.sendInfo("batch-job", "수집한 데이터 로우 수: 10");

        server.verify();
    }
}
