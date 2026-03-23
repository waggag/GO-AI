package com.goaiplatform.backend.controller;

import com.goaiplatform.backend.dto.ChatDtos;
import com.goaiplatform.backend.security.JwtUserAuthentication;
import com.goaiplatform.backend.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "实时对话、历史管理、会话导出与统计")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/conversations")
    @Operation(summary = "创建会话", description = "创建一个新的对话会话，返回会话 ID 和创建时间")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功",
                    content = @Content(schema = @Schema(implementation = ChatDtos.ConversationResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数无效", content = @Content),
            @ApiResponse(responseCode = "401", description = "未授权", content = @Content)
    })
    public Mono<ChatDtos.ConversationResponse> createConversation(
            Authentication authentication,
            @Valid @RequestBody ChatDtos.ConversationCreateRequest request
    ) {
        return chatService.createConversation(toUserId(authentication), request);
    }

    @GetMapping("/conversations")
    @Operation(summary = "查询会话列表", description = "获取当前用户的所有会话，按创建时间倒序")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未授权", content = @Content)
    })
    public Flux<ChatDtos.ConversationResponse> listConversations(Authentication authentication) {
        return chatService.listConversations(toUserId(authentication));
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "查询会话详情", description = "获取指定会话的详细信息，包括所有消息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = ChatDtos.ConversationDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "会话不存在", content = @Content),
            @ApiResponse(responseCode = "403", description = "无权限访问", content = @Content)
    })
    public Mono<ChatDtos.ConversationDetailResponse> conversationDetail(
            Authentication authentication,
            @Parameter(description = "会话 ID") @PathVariable Long id
    ) {
        return chatService.conversationDetail(toUserId(authentication), id);
    }

    @GetMapping("/conversations/{id}/export")
    @Operation(summary = "导出会话内容", description = "导出指定会话的内容为 JSON 格式")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "导出成功",
                    content = @Content(schema = @Schema(implementation = ChatDtos.ConversationExportResponse.class))),
            @ApiResponse(responseCode = "404", description = "会话不存在", content = @Content),
            @ApiResponse(responseCode = "403", description = "无权限访问", content = @Content)
    })
    public Mono<ChatDtos.ConversationExportResponse> exportConversation(
            Authentication authentication,
            @Parameter(description = "会话 ID") @PathVariable Long id
    ) {
        return chatService.exportConversation(toUserId(authentication), id);
    }

    @GetMapping(value = "/conversations/{id}/export.md", produces = "text/markdown;charset=UTF-8")
    @Operation(summary = "导出会话为Markdown", description = "导出指定会话的内容为 Markdown 格式")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "导出成功", content = @Content(mediaType = "text/markdown")),
            @ApiResponse(responseCode = "404", description = "会话不存在", content = @Content),
            @ApiResponse(responseCode = "403", description = "无权限访问", content = @Content)
    })
    public Mono<String> exportConversationMarkdown(
            Authentication authentication,
            @Parameter(description = "会话 ID") @PathVariable Long id
    ) {
        return chatService.exportConversationAsMarkdown(toUserId(authentication), id);
    }

    @GetMapping("/stats")
    @Operation(summary = "查询当前用户对话使用统计", description = "获取指定天数内的对话使用统计，包括会话数、消息数等")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = ChatDtos.UsageStatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "未授权", content = @Content)
    })
    public Mono<ChatDtos.UsageStatsResponse> usageStats(
            Authentication authentication,
            @Parameter(description = "统计天数，默认 7 天，范围 1-365") @RequestParam(required = false, defaultValue = "7") Integer days
    ) {
        return chatService.usageStats(toUserId(authentication), days);
    }

    @GetMapping("/stats/trend")
    @Operation(summary = "查询当前用户对话趋势", description = "获取指定天数内的对话使用趋势，按天聚合")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = ChatDtos.UsageTrendResponse.class))),
            @ApiResponse(responseCode = "401", description = "未授权", content = @Content)
    })
    public Mono<ChatDtos.UsageTrendResponse> usageTrend(
            Authentication authentication,
            @Parameter(description = "统计天数，默认 7 天，范围 1-365") @RequestParam(required = false, defaultValue = "7") Integer days
    ) {
        return chatService.usageTrend(toUserId(authentication), days);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE流式对话", description = "发送消息并接收流式响应，支持多厂商模型")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "对话成功", content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)),
            @ApiResponse(responseCode = "400", description = "请求参数无效", content = @Content),
            @ApiResponse(responseCode = "404", description = "会话不存在", content = @Content),
            @ApiResponse(responseCode = "403", description = "无权限访问", content = @Content),
            @ApiResponse(responseCode = "429", description = "请求频率超限", content = @Content)
    })
    public Flux<ServerSentEvent<ChatDtos.ChatChunkResponse>> stream(
            Authentication authentication,
            @Parameter(description = "会话 ID", required = true) @RequestParam @NotNull Long conversationId,
            @Valid @RequestBody ChatDtos.ChatRequest request
    ) {
        return chatService.stream(toUserId(authentication), conversationId, request)
                .map(chunk -> ServerSentEvent.builder(chunk).event("message").build());
    }

    private Long toUserId(Authentication authentication) {
        if (authentication instanceof JwtUserAuthentication jwtAuth) {
            return jwtAuth.getUserId();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无效的认证信息");
    }
}
