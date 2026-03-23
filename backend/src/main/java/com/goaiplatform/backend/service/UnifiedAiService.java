package com.goaiplatform.backend.service;

import com.goaiplatform.backend.ai.AiProviderClient;
import com.goaiplatform.backend.dto.ChatDtos;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UnifiedAiService {

    private final Map<ChatDtos.Provider, AiProviderClient> providerClientMap;

    public UnifiedAiService(List<AiProviderClient> clients) {
        this.providerClientMap = clients.stream().collect(Collectors.toMap(AiProviderClient::provider, Function.identity()));
    }

    public Flux<String> stream(Long userId, ChatDtos.ChatRequest request, List<String> context) {
        AiProviderClient client = providerClientMap.get(request.provider());
        if (client == null) {
            String message = "[" + request.provider().name() + "] " + request.prompt();
            return Flux.fromArray(message.split("")).delayElements(Duration.ofMillis(20));
        }
        return client.stream(userId, request.model(), request.prompt(), request.temperature(), request.topP(), request.maxTokens(), context);
    }
}
