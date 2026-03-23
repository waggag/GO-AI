package com.goaiplatform.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

public class ChatDtos {

    public enum Provider {
        OPENAI, QWEN, ERNIE, SPARK, CLAUDE, GEMINI, XIAOMI
    }

    public record ChatRequest(
            @NotNull Provider provider,
            @NotBlank String model,
            @NotBlank String prompt,
            Double temperature,
            Double topP,
            Integer maxTokens
    ) {
    }

    public record ChatChunkResponse(
            Long conversationId,
            String delta,
            boolean done
    ) {
    }

    public record ConversationCreateRequest(
            @NotBlank String title
    ) {
    }

    public record ConversationResponse(
            Long id,
            String title,
            LocalDateTime createdAt
    ) {
    }

    public record MessageResponse(
            Long id,
            String role,
            String content,
            String model,
            LocalDateTime createdAt
    ) {
    }

    public record ConversationDetailResponse(
            Long id,
            String title,
            List<MessageResponse> messages
    ) {
    }

    public record UsageStatsResponse(
            long conversationCount,
            long messageCount,
            long userMessageCount,
            long assistantMessageCount,
            long totalCharacters,
            long queuedRequests,
            LocalDateTime windowStart,
            LocalDateTime windowEnd
    ) {
    }

    public record ConversationExportResponse(
            Long id,
            String title,
            LocalDateTime exportedAt,
            List<MessageResponse> messages
    ) {
    }

    public record UsageTrendPoint(
            LocalDate date,
            long userMessages,
            long assistantMessages,
            long totalCharacters
    ) {
    }

    public record UsageTrendResponse(
            LocalDateTime windowStart,
            LocalDateTime windowEnd,
            List<UsageTrendPoint> points
    ) {
    }
}
