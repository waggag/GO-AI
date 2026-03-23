package com.goaiplatform.backend.ai;

import com.goaiplatform.backend.dto.ChatDtos;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AiProviderClient {
    ChatDtos.Provider provider();

    Flux<String> stream(Long userId, String model, String prompt, Double temperature, Double topP, Integer maxTokens, List<String> context);
}
