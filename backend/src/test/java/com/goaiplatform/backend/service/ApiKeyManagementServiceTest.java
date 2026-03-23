package com.goaiplatform.backend.service;

import com.goaiplatform.backend.domain.ApiKeyEntity;
import com.goaiplatform.backend.dto.ApiKeyDtos;
import com.goaiplatform.backend.repository.ApiKeyRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiKeyManagementServiceTest {

    @Test
    void saveOpenAiKeyShouldCreateActiveVersion() {
        ApiKeyRepository repository = mock(ApiKeyRepository.class);
        ApiKeyCryptoService cryptoService = new ApiKeyCryptoService("unit-test-secret");
        OpenAiKeyValidator validator = mock(OpenAiKeyValidator.class);
        ApiKeyManagementService service = new ApiKeyManagementService(repository, cryptoService, validator);

        when(repository.findFirstByUserIdAndProviderAndActiveTrueOrderByVersionDesc(eq(100L), eq("OPENAI")))
                .thenReturn(Mono.empty());
        when(repository.save(any(ApiKeyEntity.class))).thenAnswer(invocation -> {
            ApiKeyEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return Mono.just(entity);
        });

        Mono<ApiKeyDtos.ApiKeySummary> mono = service.saveOpenAiKey(100L, "sk-test-1234", false);

        StepVerifier.create(mono)
                .assertNext(summary -> {
                    assertTrue(summary.configured());
                    assertEquals(1, summary.version());
                    assertEquals("sk-****1234", summary.maskedKey());
                })
                .verifyComplete();
    }
}
