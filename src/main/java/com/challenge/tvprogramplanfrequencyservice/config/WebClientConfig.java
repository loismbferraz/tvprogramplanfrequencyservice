package com.challenge.tvprogramplanfrequencyservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * Creates a WebClient bean.
     *
     * @param builder the WebClient.Builder used to build the WebClient
     * @return a configured WebClient instance
     */
    @Bean
    public WebClient webClient(final WebClient.Builder builder) {
        return builder.build();
    }
}