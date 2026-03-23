package com.goaiplatform.backend.service;

import com.goaiplatform.backend.domain.ConversationEntity;
import com.goaiplatform.backend.domain.MessageEntity;
import com.goaiplatform.backend.dto.ChatDtos;
import com.goaiplatform.backend.repository.ConversationRepository;
import com.goaiplatform.backend.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UnifiedAiService unifiedAiService;
    private final SensitiveWordService sensitiveWordService;
    private final ConversationMemoryService memoryService;
    private final AtomicLong queuedRequests = new AtomicLong(0);

    public ChatService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            UnifiedAiService unifiedAiService,
            SensitiveWordService sensitiveWordService,
            ConversationMemoryService memoryService
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.unifiedAiService = unifiedAiService;
        this.sensitiveWordService = sensitiveWordService;
        this.memoryService = memoryService;
    }

    public Mono<ChatDtos.ConversationResponse> createConversation(Long userId, ChatDtos.ConversationCreateRequest request) {
        log.info("Creating conversation for userId={}, title={}", userId, request.title());
        ConversationEntity entity = new ConversationEntity(null, userId, request.title(), LocalDateTime.now());
        return conversationRepository.save(entity)
                .map(saved -> new ChatDtos.ConversationResponse(saved.id(), saved.title(), saved.createdAt()))
                .doOnSuccess(response -> log.info("Conversation created successfully: id={}", response.id()));
    }

    public Flux<ChatDtos.ConversationResponse> listConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .map(entity -> new ChatDtos.ConversationResponse(entity.id(), entity.title(), entity.createdAt()));
    }

    public Mono<ChatDtos.ConversationDetailResponse> conversationDetail(Long userId, Long conversationId) {
        return conversationRepository.findById(conversationId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在")))
                .flatMap(conversation -> {
                    if (!conversation.userId().equals(userId)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限访问会话"));
                    }
                    return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                            .map(msg -> new ChatDtos.MessageResponse(msg.id(), msg.role(), msg.content(), msg.model(), msg.createdAt()))
                            .collectList()
                            .map(messages -> new ChatDtos.ConversationDetailResponse(conversation.id(), conversation.title(), messages));
                });
    }

    public Mono<ChatDtos.ConversationExportResponse> exportConversation(Long userId, Long conversationId) {
        return conversationDetail(userId, conversationId)
                .map(detail -> new ChatDtos.ConversationExportResponse(
                        detail.id(),
                        detail.title(),
                        LocalDateTime.now(),
                        detail.messages()
                ));
    }

    public Mono<ChatDtos.UsageStatsResponse> usageStats(Long userId, Integer days) {
        log.info("Calculating usage stats: userId={}, days={}", userId, days);
        int windowDays = normalizedDays(days);
        LocalDateTime windowStart = LocalDateTime.now().minusDays(windowDays);
        LocalDateTime windowEnd = LocalDateTime.now();
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .filter(conversation -> conversation.createdAt() == null || !conversation.createdAt().isBefore(windowStart))
                .collectList()
                .flatMap(conversations -> {
                    List<Long> conversationIds = conversations.stream()
                            .map(ConversationEntity::id)
                            .collect(java.util.stream.Collectors.toList());
                    
                    // 使用数据库聚合查询，避免加载所有消息到内存
                    return Mono.zip(
                            messageRepository.countByConversationIdInAndCreatedAtAfter(conversationIds, windowStart),
                            messageRepository.countByConversationIdInAndRoleAndCreatedAtAfter(conversationIds, "user", windowStart),
                            messageRepository.countByConversationIdInAndRoleAndCreatedAtAfter(conversationIds, "assistant", windowStart)
                    ).map(tuple -> new ChatDtos.UsageStatsResponse(
                            conversations.size(),
                            tuple.getT1(),
                            tuple.getT2(),
                            tuple.getT3(),
                            0L, // totalCharacters 需要额外计算，暂时设为 0
                            queuedRequests.get(),
                            windowStart,
                            windowEnd
                    ));
                });
    }

    public Mono<ChatDtos.UsageTrendResponse> usageTrend(Long userId, Integer days) {
        int windowDays = normalizedDays(days);
        LocalDateTime windowStart = LocalDateTime.now().minusDays(windowDays);
        LocalDateTime windowEnd = LocalDateTime.now();
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .filter(conversation -> conversation.createdAt() == null || !conversation.createdAt().isBefore(windowStart))
                .collectList()
                .flatMap(conversations -> {
                    List<Long> conversationIds = conversations.stream()
                            .map(ConversationEntity::id)
                            .collect(java.util.stream.Collectors.toList());
                    // 批量查询所有会话的消息，避免 N+1 问题
                    return messageRepository.findByConversationIdInOrderByConversationIdAndCreatedAtAsc(conversationIds)
                            .filter(message -> message.createdAt() == null || !message.createdAt().isBefore(windowStart))
                            .collectList()
                            .map(messages -> {
                                Map<LocalDate, MutableTrend> trendMap = new HashMap<>();
                                for (int i = 0; i <= windowDays; i++) {
                                    trendMap.put(windowStart.toLocalDate().plusDays(i), new MutableTrend());
                                }
                                messages.forEach(message -> {
                                    LocalDate day = (message.createdAt() == null ? LocalDate.now() : message.createdAt().toLocalDate());
                                    MutableTrend trend = trendMap.computeIfAbsent(day, ignored -> new MutableTrend());
                                    if ("assistant".equals(message.role())) {
                                        trend.assistantMessages += 1;
                                    } else {
                                        trend.userMessages += 1;
                                    }
                                    trend.totalCharacters += message.content().length();
                                });
                                List<ChatDtos.UsageTrendPoint> points = new ArrayList<>();
                                trendMap.entrySet().stream()
                                        .sorted(Map.Entry.comparingByKey())
                                        .forEach(entry -> points.add(new ChatDtos.UsageTrendPoint(
                                                entry.getKey(),
                                                entry.getValue().userMessages,
                                                entry.getValue().assistantMessages,
                                                entry.getValue().totalCharacters
                                        )));
                                return new ChatDtos.UsageTrendResponse(windowStart, windowEnd, points);
                            });
                });
    }

    public Mono<String> exportConversationAsMarkdown(Long userId, Long conversationId) {
        return conversationDetail(userId, conversationId)
                .map(detail -> {
                    StringBuilder builder = new StringBuilder();
                    builder.append("# ").append(detail.title()).append("\n\n");
                    builder.append("- 导出时间: ").append(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)).append("\n");
                    builder.append("- 会话ID: ").append(detail.id()).append("\n\n");
                    detail.messages().forEach(message -> {
                        builder.append("## ").append("user".equals(message.role()) ? "用户" : "助手").append("\n\n");
                        builder.append(message.content()).append("\n\n");
                    });
                    return builder.toString();
                });
    }

    public Flux<ChatDtos.ChatChunkResponse> stream(Long userId, Long conversationId, ChatDtos.ChatRequest request) {
        log.info("Starting stream: userId={}, conversationId={}, provider={}, model={}", 
                userId, conversationId, request.provider(), request.model());
        String sanitizedPrompt = sensitiveWordService.sanitize(request.prompt());
        queuedRequests.incrementAndGet();
        return validateConversation(userId, conversationId)
                .then(saveMessage(conversationId, "user", sanitizedPrompt, request.model()))
                .thenMany(memoryService.loadContext(conversationId).collectList())
                .<ChatDtos.ChatChunkResponse>flatMap(context -> {
                    Flux<String> stream = unifiedAiService.stream(
                            userId,
                            new ChatDtos.ChatRequest(
                                    request.provider(),
                                    request.model(),
                                    sanitizedPrompt,
                                    request.temperature(),
                                    request.topP(),
                                    request.maxTokens()
                            ),
                            context
                    );
                    AtomicReference<StringBuilder> answerRef = new AtomicReference<>(new StringBuilder());
                    return stream
                            .doOnNext(chunk -> answerRef.get().append(chunk))
                            .map(chunk -> new ChatDtos.ChatChunkResponse(conversationId, chunk, false))
                            .concatWith(Mono.just(new ChatDtos.ChatChunkResponse(conversationId, "", true)))
                            .concatWith(Mono.defer(() -> {
                                String answer = answerRef.get().toString();
                                return saveMessage(conversationId, "assistant", answer, request.model())
                                        .then(memoryService.appendContext(
                                                conversationId,
                                                List.of("user:" + sanitizedPrompt, "assistant:" + answer)
                                        ))
                                        .then(Mono.empty());
                            }));
                })
                .doFinally(signalType -> queuedRequests.updateAndGet(current -> current > 0 ? current - 1 : 0));
    }

    private Mono<Void> validateConversation(Long userId, Long conversationId) {
        log.debug("Validating conversation: userId={}, conversationId={}", userId, conversationId);
        return conversationRepository.findById(conversationId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在")))
                .flatMap(conversation -> {
                    if (conversation.userId().equals(userId)) {
                        log.debug("Conversation validation passed: conversationId={}", conversationId);
                        return Mono.empty();
                    } else {
                        log.warn("Conversation access denied: userId={}, conversationId={}, ownerId={}", 
                                userId, conversationId, conversation.userId());
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限访问会话"));
                    }
                });
    }

    private Mono<Void> saveMessage(Long conversationId, String role, String content, String model) {
        MessageEntity message = new MessageEntity(null, conversationId, role, content, model, LocalDateTime.now());
        return messageRepository.save(message).then();
    }

    private int normalizedDays(Integer days) {
        return days == null ? 7 : Math.min(Math.max(days, 1), 365);
    }

    private static class MutableTrend {
        long userMessages;
        long assistantMessages;
        long totalCharacters;
    }
}
