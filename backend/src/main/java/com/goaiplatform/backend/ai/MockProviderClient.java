package com.goaiplatform.backend.ai;

import com.goaiplatform.backend.dto.ChatDtos;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@Component
public class MockProviderClient implements AiProviderClient {

    @Override
    public ChatDtos.Provider provider() {
        return ChatDtos.Provider.QWEN;
    }

    @Override
    public Flux<String> stream(Long userId, String model, String prompt, Double temperature, Double topP, Integer maxTokens, List<String> context) {
        String content = "Mock[" + model + "]: " + prompt;
        return Flux.fromArray(content.split(""))
                .delayElements(Duration.ofMillis(15));
    }
}
