package com.goaiplatform.backend.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class ApiKeyDtos {

    public record UpsertOpenAiKeyRequest(
            @NotBlank String apiKey,
            boolean rotate
    ) {
    }

    public record ValidateKeyRequest(
            @NotBlank String apiKey
    ) {
    }

    public record ApiKeySummary(
            String provider,
            boolean configured,
            String maskedKey,
            String fingerprint,
            Integer version,
            LocalDateTime validatedAt,
            LocalDateTime rotatedAt,
            LocalDateTime updatedAt
    ) {
    }

    public record ValidateKeyResponse(
            boolean valid,
            String message
    ) {
    }
}
