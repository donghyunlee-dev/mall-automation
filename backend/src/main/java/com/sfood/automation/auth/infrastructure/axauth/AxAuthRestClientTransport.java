package com.sfood.automation.auth.infrastructure.axauth;

import com.sfood.automation.auth.application.port.AxAuthVerifyPort;
import com.sfood.automation.auth.application.port.AxAuthVerifyResult;
import com.sfood.automation.common.exception.MsAuthGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AxAuthRestClientTransport implements AxAuthVerifyPort {

    private static final Logger log = LoggerFactory.getLogger(AxAuthRestClientTransport.class);

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public AxAuthRestClientTransport(
        @Qualifier("axAuthRestClient") RestClient restClient,
        @Value("${app.ax-auth.client-id}") String clientId,
        @Value("${app.ax-auth.client-secret}") String clientSecret
    ) {
        this.restClient = restClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public AxAuthVerifyResult verify(String loginToken) {
        try {
            return restClient.post()
                .uri("/auth/token/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new VerifyRequest(clientId, clientSecret, loginToken))
                .retrieve()
                .body(AxAuthVerifyResult.class);
        } catch (RestClientException exception) {
            log.error("AX-Auth token verify call failed", exception);
            throw new MsAuthGatewayException();
        }
    }

    private record VerifyRequest(String clientId, String clientSecret, String loginToken) {
    }
}
