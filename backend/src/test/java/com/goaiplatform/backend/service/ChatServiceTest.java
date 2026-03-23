package com.goaiplatform.backend.service;

import com.goaiplatform.backend.domain.ConversationEntity;
import com.goaiplatform.backend.domain.MessageEntity;
import com.goaiplatform.backend.dto.ChatDtos;
import com.goaiplatform.backend.repository.ConversationRepository;
import com.goaiplatform.backend.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private ChatService createService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            UnifiedAiService unifiedAiService,
            SensitiveWordService sensitiveWordService,
            ConversationMemoryService memoryService
    ) {
        return new ChatService(
                conversationRepository,
                messageRepository,
                unifiedAiService,
                sensitiveWordService,
                memoryService
        );
    }

    @Test
    void createConversationShouldPersistEntity() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        UnifiedAiService unifiedAiService = mock(UnifiedAiService.class);
        SensitiveWordService sensitiveWordService = mock(SensitiveWordService.class);
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        ChatService service = createService(conversationRepository, messageRepository, unifiedAiService, sensitiveWordService, memoryService);

        ConversationEntity savedEntity = new ConversationEntity(1L, 1L, "Test Title", LocalDateTime.now());

        when(conversationRepository.save(any(ConversationEntity.class)))
                .thenReturn(Mono.just(savedEntity));

        ChatDtos.ConversationCreateRequest request = new ChatDtos.ConversationCreateRequest("Test Title");

        StepVerifier.create(service.createConversation(1L, request))
                .assertNext(response -> {
                    assertEquals(1L, response.id());
                    assertEquals("Test Title", response.title());
                    assertNotNull(response.createdAt());
                })
                .verifyComplete();
    }

    @Test
    void conversationDetailShouldReturnMessages() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        UnifiedAiService unifiedAiService = mock(UnifiedAiService.class);
        SensitiveWordService sensitiveWordService = mock(SensitiveWordService.class);
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        ChatService service = createService(conversationRepository, messageRepository, unifiedAiService, sensitiveWordService, memoryService);

        ConversationEntity conversation = new ConversationEntity(1L, 1L, "Test Conversation", LocalDateTime.now());

        MessageEntity message1 = new MessageEntity(1L, 1L, "user", "Hello", null, LocalDateTime.now());
        MessageEntity message2 = new MessageEntity(2L, 1L, "assistant", "Hi there!", null, LocalDateTime.now());

        when(conversationRepository.findById(1L)).thenReturn(Mono.just(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(1L))
                .thenReturn(Flux.just(message1, message2));

        StepVerifier.create(service.conversationDetail(1L, 1L))
                .assertNext(detail -> {
                    assertEquals(1L, detail.id());
                    assertEquals("Test Conversation", detail.title());
                    assertEquals(2, detail.messages().size());
                    assertEquals("user", detail.messages().get(0).role());
                    assertEquals("assistant", detail.messages().get(1).role());
                })
                .verifyComplete();
    }

    @Test
    void conversationDetailShouldReturn404WhenNotFound() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        UnifiedAiService unifiedAiService = mock(UnifiedAiService.class);
        SensitiveWordService sensitiveWordService = mock(SensitiveWordService.class);
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        ChatService service = createService(conversationRepository, messageRepository, unifiedAiService, sensitiveWordService, memoryService);

        when(conversationRepository.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(service.conversationDetail(1L, 999L))
                .expectErrorMatches(throwable -> throwable.getMessage().contains("会话不存在"))
                .verify();
    }

    @Test
    void conversationDetailShouldReturn403WhenNotOwner() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        UnifiedAiService unifiedAiService = mock(UnifiedAiService.class);
        SensitiveWordService sensitiveWordService = mock(SensitiveWordService.class);
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        ChatService service = createService(conversationRepository, messageRepository, unifiedAiService, sensitiveWordService, memoryService);

        ConversationEntity conversation = new ConversationEntity(1L, 2L, "Test Conversation", LocalDateTime.now());

        when(conversationRepository.findById(1L)).thenReturn(Mono.just(conversation));

        StepVerifier.create(service.conversationDetail(1L, 1L))
                .expectErrorMatches(throwable -> throwable.getMessage().contains("无权限访问会话"))
                .verify();
    }

    @Test
    void streamShouldReturnChunksAndSaveMessages() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        UnifiedAiService unifiedAiService = mock(UnifiedAiService.class);
        SensitiveWordService sensitiveWordService = mock(SensitiveWordService.class);
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        ChatService service = new ChatService(
                conversationRepository,
                messageRepository,
                unifiedAiService,
                sensitiveWordService,
                memoryService
        );

        // Mock conversation exists and belongs to user
        ConversationEntity conversation = new ConversationEntity(1L, 1L, "test conversation", LocalDateTime.now());

        when(conversationRepository.findById(1L)).thenReturn(Mono.just(conversation));
        
        // Mock save message
        when(messageRepository.save(any(MessageEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        
        // Mock memory service
        when(memoryService.loadContext(1L)).thenReturn(Flux.empty());
        when(memoryService.appendContext(eq(1L), anyList())).thenReturn(Mono.empty());
        
        // Mock AI service returns stream
        when(unifiedAiService.stream(eq(1L), any(ChatDtos.ChatRequest.class), anyList()))
                .thenReturn(Flux.just("Hello", " ", "World"));
        
        // Mock sanitize service
        when(sensitiveWordService.sanitize("test prompt")).thenReturn("test prompt");

        ChatDtos.ChatRequest request = new ChatDtos.ChatRequest(
                ChatDtos.Provider.OPENAI,
                "gpt-4o",
                "test prompt",
                0.7,
                0.9,
                1024
        );

        // Execute and verify
        StepVerifier.create(service.stream(1L, 1L, request))
                .expectNextCount(4) // 3 chunks + 1 done marker
                .verifyComplete();
    }

    @Test
    void streamShouldReturn404WhenConversationNotFound() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        UnifiedAiService unifiedAiService = mock(UnifiedAiService.class);
        SensitiveWordService sensitiveWordService = mock(SensitiveWordService.class);
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        ChatService service = new ChatService(
                conversationRepository,
                messageRepository,
                unifiedAiService,
                sensitiveWordService,
                memoryService
        );

        when(conversationRepository.findById(999L)).thenReturn(Mono.empty());
        when(sensitiveWordService.sanitize(anyString())).thenAnswer(i -> i.getArgument(0));
        // Add mock for messageRepository.save() to prevent NPE if error handling reaches saveMessage
        when(messageRepository.save(any(MessageEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(memoryService.loadContext(anyLong())).thenReturn(Flux.empty());
        when(memoryService.appendContext(anyLong(), anyList())).thenReturn(Mono.empty());
        when(unifiedAiService.stream(anyLong(), any(ChatDtos.ChatRequest.class), anyList()))
                .thenReturn(Flux.just("response"));

        ChatDtos.ChatRequest request = new ChatDtos.ChatRequest(
                ChatDtos.Provider.OPENAI,
                "gpt-4o",
                "test prompt",
                0.7,
                0.9,
                1024
        );

        StepVerifier.create(service.stream(1L, 999L, request))
                .expectErrorMatches(throwable -> 
                    throwable.getMessage().contains("会话不存在"))
                .verify();
    }

    @Test
    void streamShouldReturn403WhenUserNotOwner() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        UnifiedAiService unifiedAiService = mock(UnifiedAiService.class);
        SensitiveWordService sensitiveWordService = mock(SensitiveWordService.class);
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        ChatService service = new ChatService(
                conversationRepository,
                messageRepository,
                unifiedAiService,
                sensitiveWordService,
                memoryService
        );

        ConversationEntity conversation = new ConversationEntity(1L, 2L, "test conversation", LocalDateTime.now());

        when(conversationRepository.findById(1L)).thenReturn(Mono.just(conversation));
        when(sensitiveWordService.sanitize(anyString())).thenAnswer(i -> i.getArgument(0));
        // Add mock for messageRepository.save() to prevent NPE if error handling reaches saveMessage
        when(messageRepository.save(any(MessageEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(memoryService.loadContext(anyLong())).thenReturn(Flux.empty());
        when(memoryService.appendContext(anyLong(), anyList())).thenReturn(Mono.empty());
        when(unifiedAiService.stream(anyLong(), any(ChatDtos.ChatRequest.class), anyList()))
                .thenReturn(Flux.just("response"));

        ChatDtos.ChatRequest request = new ChatDtos.ChatRequest(
                ChatDtos.Provider.OPENAI,
                "gpt-4o",
                "test prompt",
                0.7,
                0.9,
                1024
        );

        StepVerifier.create(service.stream(1L, 1L, request))
                .expectErrorMatches(throwable -> 
                    throwable.getMessage().contains("无权限访问会话"))
                .verify();
    }

    @Test
    void usageStatsShouldAggregateConversationAndMessageMetrics() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        UnifiedAiService unifiedAiService = mock(UnifiedAiService.class);
        SensitiveWordService sensitiveWordService = mock(SensitiveWordService.class);
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        ChatService service = new ChatService(
                conversationRepository,
                messageRepository,
                unifiedAiService,
                sensitiveWordService,
                memoryService
        );

        ConversationEntity conversation = new ConversationEntity(9L, 1L, "t1", LocalDateTime.now());

        when(conversationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L)))
                .thenReturn(Flux.just(conversation));
        
        // Mock the aggregate query methods
        when(messageRepository.countByConversationIdInAndCreatedAtAfter(anyList(), any(LocalDateTime.class)))
                .thenReturn(Mono.just(2L));
        when(messageRepository.countByConversationIdInAndRoleAndCreatedAtAfter(anyList(), eq("user"), any(LocalDateTime.class)))
                .thenReturn(Mono.just(1L));
        when(messageRepository.countByConversationIdInAndRoleAndCreatedAtAfter(anyList(), eq("assistant"), any(LocalDateTime.class)))
                .thenReturn(Mono.just(1L));

        StepVerifier.create(service.usageStats(1L, 7))
                .assertNext(stats -> {
                    assertEquals(1, stats.conversationCount());
                    assertEquals(2, stats.messageCount());
                    assertEquals(1, stats.userMessageCount());
                    assertEquals(1, stats.assistantMessageCount());
                    long diffDays = java.time.Duration.between(stats.windowStart(), stats.windowEnd()).toDays();
                    org.junit.jupiter.api.Assertions.assertTrue(diffDays >= 6 && diffDays <= 7);
                })
                .verifyComplete();
    }

    @Test
    void usageTrendShouldReturnOrderedPoints() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        UnifiedAiService unifiedAiService = mock(UnifiedAiService.class);
        SensitiveWordService sensitiveWordService = mock(SensitiveWordService.class);
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        ChatService service = new ChatService(
                conversationRepository,
                messageRepository,
                unifiedAiService,
                sensitiveWordService,
                memoryService
        );

        ConversationEntity conversation = new ConversationEntity(9L, 1L, "trend", LocalDateTime.now().minusDays(1));

        MessageEntity userMessage = new MessageEntity(null, 9L, "user", "u1", null, LocalDateTime.now().minusDays(1));
        MessageEntity assistantMessage = new MessageEntity(null, 9L, "assistant", "a123", null, LocalDateTime.now());

        when(conversationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L)))
                .thenReturn(Flux.just(conversation));
        when(messageRepository.findByConversationIdInOrderByConversationIdAndCreatedAtAsc(anyList()))
                .thenReturn(Flux.just(userMessage, assistantMessage));

        StepVerifier.create(service.usageTrend(1L, 7))
                .assertNext(trend -> {
                    org.junit.jupiter.api.Assertions.assertFalse(trend.points().isEmpty());
                    long totalUser = trend.points().stream().mapToLong(ChatDtos.UsageTrendPoint::userMessages).sum();
                    long totalAssistant = trend.points().stream().mapToLong(ChatDtos.UsageTrendPoint::assistantMessages).sum();
                    assertEquals(1, totalUser);
                    assertEquals(1, totalAssistant);
                })
                .verifyComplete();
    }
}
