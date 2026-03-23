package com.goaiplatform.backend.controller;

import com.goaiplatform.backend.dto.ApiKeyDtos;
import com.goaiplatform.backend.service.ApiKeyManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/keys")
@Tag(name = "API Keys", description = "OpenAI API Key 安全配置与验证")
public class ApiKeyController {

    private final ApiKeyManagementService apiKeyManagementService;

    public ApiKeyController(ApiKeyManagementService apiKeyManagementService) {
        this.apiKeyManagementService = apiKeyManagementService;
    }

    @GetMapping("/openai")
    @Operation(summary = "查询当前用户 OpenAI Key 配置摘要")
    public Mono<ApiKeyDtos.ApiKeySummary> openAiSummary(Authentication authentication) {
        return apiKeyManagementService.getOpenAiKeySummary(toUserId(authentication));
    }

    @PutMapping("/openai")
    @Operation(summary = "保存或轮换 OpenAI Key")
    public Mono<ApiKeyDtos.ApiKeySummary> saveOpenAiKey(
            Authentication authentication,
            @Valid @RequestBody ApiKeyDtos.UpsertOpenAiKeyRequest request
    ) {
        return apiKeyManagementService.saveOpenAiKey(toUserId(authentication), request.apiKey(), request.rotate());
    }

    @PostMapping("/openai/verify")
    @Operation(summary = "校验当前保存的 OpenAI Key")
    public Mono<ApiKeyDtos.ValidateKeyResponse> verifyOpenAiKey(Authentication authentication) {
        return apiKeyManagementService.validateCurrentOpenAiKey(toUserId(authentication));
    }

    @PostMapping("/openai/verify-input")
    @Operation(summary = "校验输入的 OpenAI Key 是否有效")
    public Mono<ApiKeyDtos.ValidateKeyResponse> verifyInput(
            @Valid @RequestBody ApiKeyDtos.ValidateKeyRequest request
    ) {
        return apiKeyManagementService.validateOpenAiKeyInput(request.apiKey());
    }

    @DeleteMapping("/openai")
    @Operation(summary = "停用当前 OpenAI Key")
    public Mono<Void> disableOpenAiKey(Authentication authentication) {
        return apiKeyManagementService.disableOpenAiKey(toUserId(authentication));
    }

    private Long toUserId(Authentication authentication) {
        return Math.abs((long) authentication.getName().hashCode());
    }
}
