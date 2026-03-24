package com.goaiplatform.backend.ai;

import com.goaiplatform.backend.dto.ChatDtos;
import com.goaiplatform.backend.service.ApiKeyManagementService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Gemini (Google) AI 提供商客户端
 * API 格式与 OpenAI 不同
 */
@Component
public class GeminiProviderClient implements AiProviderClient {

    private final WebClient webClient;
    private final ApiKeyManagementService apiKeyManagementService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String fallbackApiKey;

    public GeminiProviderClient(
            ApiKeyManagementService apiKeyManagementService,
            @Value("${spring.ai.gemini.api-key:}") String fallbackApiKey
    ) {
        this.apiKeyManagementService = apiKeyManagementService;
        this.fallbackApiKey = fallbackApiKey;
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    @Override
    public ChatDtos.Provider provider() {
        return ChatDtos.Provider.GEMINI;
    }

    @Override
    public Flux<String> stream(Long userId, String model, String prompt, Double temperature, Double topP, Integer maxTokens, List<String> context) {
        String fullPrompt = String.join("\n", context) + "\n" + prompt;
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", fullPrompt)))),
                "generationConfig", Map.of(
                        "temperature", temperature == null ? 0.7 : temperature,
                        "topP", topP == null ? 0.9 : topP,
                        "maxOutputTokens", maxTokens == null ? 1024 : maxTokens
                )
        );
        return resolveApiKey(userId)
                .flatMapMany(apiKey -> webClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/models/{model}:streamGenerateContent")
                                .queryParam("key", apiKey)
                                .build(model))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .flatMap(this::parseSseChunk)
                )
                .onErrorResume(ex -> Flux.just("Gemini调用失败：" + ex.getMessage()));
    }

    private Mono<String> resolveApiKey(Long userId) {
        return apiKeyManagementService.resolveProviderKey(userId, ChatDtos.Provider.GEMINI)
                .switchIfEmpty(Mono.justOrEmpty(fallbackApiKey))
                .switchIfEmpty(Mono.error(new IllegalStateException("Gemini未配置API Key")));
    }

    private Flux<String> parseSseChunk(String rawChunk) {
        return Flux.fromArray(rawChunk.split("\n"))
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6).trim())
                .filter(payload -> !payload.equals("[DONE]"))
                .flatMap(payload -> {
                    try {
                        JsonNode root = objectMapper.readTree(payload);
                        JsonNode candidates = root.path("candidates");
                        if (candidates.isArray() && !candidates.isEmpty()) {
                            JsonNode content = candidates.get(0).path("content").path("parts");
                            if (content.isArray() && !content.isEmpty()) {
                                JsonNode text = content.get(0).path("text");
                                if (!text.isMissingNode() && !text.isNull()) {
                                    return Mono.just(text.asText(""));
                                }
                            }
                        }
                        return Mono.empty();
                    } catch (Exception ignored) {
                        return Mono.empty();
                    }
                });
    }
}
