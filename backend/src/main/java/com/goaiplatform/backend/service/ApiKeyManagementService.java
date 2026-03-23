package com.goaiplatform.backend.service;

import com.goaiplatform.backend.domain.ApiKeyEntity;
import com.goaiplatform.backend.dto.ApiKeyDtos;
import com.goaiplatform.backend.dto.ChatDtos;
import com.goaiplatform.backend.repository.ApiKeyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class ApiKeyManagementService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyCryptoService apiKeyCryptoService;
    private final OpenAiKeyValidator openAiKeyValidator;

    public ApiKeyManagementService(
            ApiKeyRepository apiKeyRepository,
            ApiKeyCryptoService apiKeyCryptoService,
            OpenAiKeyValidator openAiKeyValidator
    ) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyCryptoService = apiKeyCryptoService;
        this.openAiKeyValidator = openAiKeyValidator;
    }

    public Mono<ApiKeyDtos.ApiKeySummary> getOpenAiKeySummary(Long userId) {
        return apiKeyRepository.findFirstByUserIdAndProviderAndActiveTrueOrderByVersionDesc(userId, ChatDtos.Provider.OPENAI.name())
                .map(this::toSummary)
                .defaultIfEmpty(new ApiKeyDtos.ApiKeySummary(ChatDtos.Provider.OPENAI.name(), false, "", "", 0, null, null, null));
    }

    public Mono<ApiKeyDtos.ApiKeySummary> saveOpenAiKey(Long userId, String apiKey, boolean rotate) {
        String normalized = normalizeOpenAiKey(apiKey);
        LocalDateTime now = LocalDateTime.now();
        return apiKeyRepository.findFirstByUserIdAndProviderAndActiveTrueOrderByVersionDesc(userId, ChatDtos.Provider.OPENAI.name())
                .flatMap(current -> deactivate(current, now).then(createActive(userId, normalized, current.getVersion() + 1, now, rotate)))
                .switchIfEmpty(createActive(userId, normalized, 1, now, rotate))
                .map(this::toSummary);
    }

    public Mono<ApiKeyDtos.ValidateKeyResponse> validateCurrentOpenAiKey(Long userId) {
        return apiKeyRepository.findFirstByUserIdAndProviderAndActiveTrueOrderByVersionDesc(userId, ChatDtos.Provider.OPENAI.name())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到可用的 OpenAI Key")))
                .flatMap(entity -> openAiKeyValidator.validate(apiKeyCryptoService.decrypt(entity.getKeyCiphertext()))
                        .flatMap(valid -> {
                            if (valid) {
                                entity.setValidatedAt(LocalDateTime.now());
                                entity.setUpdatedAt(LocalDateTime.now());
                                return apiKeyRepository.save(entity).thenReturn(new ApiKeyDtos.ValidateKeyResponse(true, "Key 校验通过"));
                            }
                            return Mono.just(new ApiKeyDtos.ValidateKeyResponse(false, "Key 无效或权限不足"));
                        }));
    }

    public Mono<ApiKeyDtos.ValidateKeyResponse> validateOpenAiKeyInput(String apiKey) {
        String normalized = normalizeOpenAiKey(apiKey);
        return openAiKeyValidator.validate(normalized)
                .map(valid -> valid
                        ? new ApiKeyDtos.ValidateKeyResponse(true, "输入的 Key 可用")
                        : new ApiKeyDtos.ValidateKeyResponse(false, "输入的 Key 不可用"));
    }

    public Mono<Void> disableOpenAiKey(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return apiKeyRepository.findFirstByUserIdAndProviderAndActiveTrueOrderByVersionDesc(userId, ChatDtos.Provider.OPENAI.name())
                .flatMap(entity -> deactivate(entity, now))
                .then();
    }

    public Mono<String> resolveProviderKey(Long userId, ChatDtos.Provider provider) {
        return apiKeyRepository.findFirstByUserIdAndProviderAndActiveTrueOrderByVersionDesc(userId, provider.name())
                .map(entity -> apiKeyCryptoService.decrypt(entity.getKeyCiphertext()))
                .switchIfEmpty(Mono.empty());
    }

    private Mono<ApiKeyEntity> createActive(Long userId, String apiKey, int version, LocalDateTime now, boolean rotate) {
        ApiKeyEntity next = new ApiKeyEntity();
        next.setUserId(userId);
        next.setProvider(ChatDtos.Provider.OPENAI.name());
        next.setKeyCiphertext(apiKeyCryptoService.encrypt(apiKey));
        next.setKeyFingerprint(apiKeyCryptoService.fingerprint(apiKey));
        next.setKeyLast4(last4(apiKey));
        next.setVersion(version);
        next.setActive(true);
        next.setRotatedAt(rotate ? now : null);
        next.setCreatedAt(now);
        next.setUpdatedAt(now);
        return apiKeyRepository.save(next);
    }

    private Mono<ApiKeyEntity> deactivate(ApiKeyEntity current, LocalDateTime now) {
        current.setActive(false);
        current.setUpdatedAt(now);
        current.setRotatedAt(now);
        return apiKeyRepository.save(current);
    }

    private ApiKeyDtos.ApiKeySummary toSummary(ApiKeyEntity entity) {
        return new ApiKeyDtos.ApiKeySummary(
                entity.getProvider(),
                true,
                "sk-****" + entity.getKeyLast4(),
                entity.getKeyFingerprint(),
                entity.getVersion(),
                entity.getValidatedAt(),
                entity.getRotatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String normalizeOpenAiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "API Key 不能为空");
        }
        String normalized = apiKey.trim();
        if (!normalized.startsWith("sk-")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OpenAI API Key 格式不正确");
        }
        return normalized;
    }

    private String last4(String raw) {
        if (raw.length() <= 4) {
            return raw;
        }
        return raw.substring(raw.length() - 4);
    }
}
