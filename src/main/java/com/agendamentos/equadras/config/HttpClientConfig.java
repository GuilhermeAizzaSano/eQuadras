package com.agendamentos.equadras.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class HttpClientConfig {

    public static final Duration TIMEOUT = Duration.ofMillis(4000);

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }
}
