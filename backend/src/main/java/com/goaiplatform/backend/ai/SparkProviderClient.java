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
 * 讯飞星火 AI 提供商客户端
 * 注意：星火 API 使用 WebSocket 协议
 * 这里提供简化的 HTTP 实现，实际使用需要 WebSocket 支持
 */
@Component
public class SparkProviderClient implements AiProviderClient {

    private final WebClient webClient;
    private final ApiKeyManagementService apiKeyManagementService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String fallbackApiKey;

    public SparkProviderClient(
            ApiKeyManagementService apiKeyManagementService,
            @Value("${spring.ai.spark.api-key:}") String fallbackApiKey
    ) {
        this.apiKeyManagementService = apiKeyManagementService;
        this.fallbackApiKey = fallbackApiKey;
        // 星火 API 的 HTTP 端点（兼容模式）
        this.webClient = WebClient.builder()
                .baseUrl("https://spark-api-open.xf-yun.com/v1")
                .build();
    }

    @Override
    public ChatDtos.Provider provider() {
        return ChatDtos.Provider.SPARK;
    }

    @Override
    public Flux<String> stream(Long userId, String model, String prompt, Double temperature, Double topP, Integer maxTokens, List<String> context) {
        String fullPrompt = String.join("\n", context) + "\n" + prompt;
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", fullPrompt)),
                "temperature", temperature == null ? 0.7 : temperature,
                "top_p", topP == null ? 0.9 : topP,
                "max_tokens", maxTokens == null ? 1024 : maxTokens,
                "stream", true
        );
        return resolveApiKey(userId)
                .flatMapMany(apiKey -> webClient.post()
                        .uri("/chat/completions")
                        .headers(headers -> headers.setBearerAuth(apiKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .flatMap(this::parseSseChunk)
                )
                .onErrorResume(ex -> Flux.just("讯飞星火调用失败：" + ex.getMessage()));
    }

    private Mono<String> resolveApiKey(Long userId) {
        return apiKeyManagementService.resolveProviderKey(userId, ChatDtos.Provider.SPARK)
                .switchIfEmpty(Mono.justOrEmpty(fallbackApiKey))
                .switchIfEmpty(Mono.error(new IllegalStateException("讯飞星火未配置API Key")));
    }

    private Flux<String> parseSseChunk(String rawChunk) {
        return Flux.fromArray(rawChunk.split("\n"))
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6).trim())
                .filter(payload -> !payload.equals("[DONE]"))
                .flatMap(payload -> {
                    try {
                        JsonNode root = objectMapper.readTree(payload);
                        JsonNode delta = root.path("choices").path(0).path("delta").path("content");
                        if (delta.isMissingNode() || delta.isNull()) {
                            return Mono.empty();
                        }
                        return Mono.just(delta.asText(""));
                    } catch (Exception ignored) {
                        return Mono.empty();
                    }
                });
    }
}
