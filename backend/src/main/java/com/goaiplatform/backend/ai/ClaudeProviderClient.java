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
 * Claude (Anthropic) AI 提供商客户端
 * API 格式与 OpenAI 不同
 */
@Component
public class ClaudeProviderClient implements AiProviderClient {

    private final WebClient webClient;
    private final ApiKeyManagementService apiKeyManagementService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String fallbackApiKey;

    public ClaudeProviderClient(
            ApiKeyManagementService apiKeyManagementService,
            @Value("${spring.ai.claude.api-key:}") String fallbackApiKey
    ) {
        this.apiKeyManagementService = apiKeyManagementService;
        this.fallbackApiKey = fallbackApiKey;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.anthropic.com/v1")
                .build();
    }

    @Override
    public ChatDtos.Provider provider() {
        return ChatDtos.Provider.CLAUDE;
    }

    @Override
    public Flux<String> stream(Long userId, String model, String prompt, Double temperature, Double topP, Integer maxTokens, List<String> context) {
        String fullPrompt = String.join("\n", context) + "\n" + prompt;
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens == null ? 1024 : maxTokens,
                "messages", List.of(Map.of("role", "user", "content", fullPrompt)),
                "stream", true
        );
        return resolveApiKey(userId)
                .flatMapMany(apiKey -> webClient.post()
                        .uri("/messages")
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .flatMap(this::parseSseChunk)
                )
                .onErrorResume(ex -> Flux.just("Claude调用失败：" + ex.getMessage()));
    }

    private Mono<String> resolveApiKey(Long userId) {
        return apiKeyManagementService.resolveProviderKey(userId, ChatDtos.Provider.CLAUDE)
                .switchIfEmpty(Mono.justOrEmpty(fallbackApiKey))
                .switchIfEmpty(Mono.error(new IllegalStateException("Claude未配置API Key")));
    }

    private Flux<String> parseSseChunk(String rawChunk) {
        return Flux.fromArray(rawChunk.split("\n"))
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6).trim())
                .filter(payload -> !payload.equals("[DONE]"))
                .flatMap(payload -> {
                    try {
                        JsonNode root = objectMapper.readTree(payload);
                        String eventType = root.path("type").asText("");
                        
                        // 处理 content_block_delta 事件
                        if ("content_block_delta".equals(eventType)) {
                            JsonNode delta = root.path("delta").path("text");
                            if (delta.isMissingNode() || delta.isNull()) {
                                return Mono.empty();
                            }
                            return Mono.just(delta.asText(""));
                        }
                        return Mono.empty();
                    } catch (Exception ignored) {
                        return Mono.empty();
                    }
                });
    }
}
