package com.goaiplatform.backend.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class OpenAiKeyValidator {

    private final WebClient webClient;

    public OpenAiKeyValidator() {
        this.webClient = WebClient.builder().baseUrl("https://api.openai.com/v1").build();
    }

    public Mono<Boolean> validate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Mono.just(false);
        }
        return webClient.get()
                .uri("/models")
                .headers(headers -> headers.setBearerAuth(apiKey))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorReturn(false);
    }
}
