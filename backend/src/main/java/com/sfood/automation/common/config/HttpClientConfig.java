package com.sfood.automation.common.config;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.net.http.HttpClient;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    @Bean
    @Qualifier("wmsRestClient")
    RestClient wmsRestClient(RestClient.Builder builder) {
        return builder
            .requestFactory(new JdkClientHttpRequestFactory(insecureWmsHttpClient()))
            .build();
    }

    @Bean
    @Qualifier("godoRestClient")
    RestClient godoRestClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Bean
    @Qualifier("axAuthRestClient")
    RestClient axAuthRestClient(RestClient.Builder builder, @Value("${app.ax-auth.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    private HttpClient insecureWmsHttpClient() {
        try {
            TrustManager[] trustAllManagers = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllManagers, new SecureRandom());
            return HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to create WMS HTTP client", exception);
        }
    }
}
