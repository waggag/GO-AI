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
 * 文心一言（百度）AI 提供商客户端
 * 注意：百度文心 API 需要先获取 access_token
 * 这里假设用户提供的是 access_token
 */
@Component
public class ErnieProviderClient implements AiProviderClient {

    private final WebClient webClient;
    private final ApiKeyManagementService apiKeyManagementService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String fallbackApiKey;

    public ErnieProviderClient(
            ApiKeyManagementService apiKeyManagementService,
            @Value("${spring.ai.ernie.api-key:}") String fallbackApiKey
    ) {
        this.apiKeyManagementService = apiKeyManagementService;
        this.fallbackApiKey = fallbackApiKey;
        this.webClient = WebClient.builder()
                .baseUrl("https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop")
                .build();
    }

    @Override
    public ChatDtos.Provider provider() {
        return ChatDtos.Provider.ERNIE;
    }

    @Override
    public Flux<String> stream(Long userId, String model, String prompt, Double temperature, Double topP, Integer maxTokens, List<String> context) {
        String fullPrompt = String.join("\n", context) + "\n" + prompt;
        Map<String, Object> body = Map.of(
                "messages", List.of(Map.of("role", "user", "content", fullPrompt)),
                "temperature", temperature == null ? 0.7 : temperature,
                "top_p", topP == null ? 0.9 : topP,
                "max_tokens", maxTokens == null ? 1024 : maxTokens,
                "stream", true
        );
        return resolveApiKey(userId)
                .flatMapMany(accessToken -> webClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/chat/{model}")
                                .queryParam("access_token", accessToken)
                                .build(model))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .flatMap(this::parseSseChunk)
                )
                .onErrorResume(ex -> Flux.just("文心一言调用失败：" + ex.getMessage()));
    }

    private Mono<String> resolveApiKey(Long userId) {
        return apiKeyManagementService.resolveProviderKey(userId, ChatDtos.Provider.ERNIE)
                .switchIfEmpty(Mono.justOrEmpty(fallbackApiKey))
                .switchIfEmpty(Mono.error(new IllegalStateException("文心一言未配置API Key")));
    }

    private Flux<String> parseSseChunk(String rawChunk) {
        return Flux.fromArray(rawChunk.split("\n"))
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6).trim())
                .filter(payload -> !payload.equals("[DONE]"))
                .flatMap(payload -> {
                    try {
                        JsonNode root = objectMapper.readTree(payload);
                        JsonNode result = root.path("result");
                        if (result.isMissingNode() || result.isNull()) {
                            return Mono.empty();
                        }
                        return Mono.just(result.asText(""));
                    } catch (Exception ignored) {
                        return Mono.empty();
                    }
                });
    }
}
