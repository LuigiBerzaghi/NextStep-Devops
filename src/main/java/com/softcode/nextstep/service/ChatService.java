package com.softcode.nextstep.service;

import com.softcode.nextstep.api.dto.chat.ChatHistoryResponse;
import com.softcode.nextstep.api.dto.chat.ChatMessageRequest;
import com.softcode.nextstep.api.dto.chat.ChatMessageResponse;
import com.softcode.nextstep.domain.chat.ChatMessage;
import com.softcode.nextstep.domain.chat.ChatRole;
import com.softcode.nextstep.domain.user.User;
import com.softcode.nextstep.repository.ChatMessageRepository;
import com.softcode.nextstep.security.AuthenticatedUserContext;
import com.softcode.nextstep.service.ai.GeminiService;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final AuthenticatedUserContext authenticatedUserContext;
    private final ChatMessageRepository chatMessageRepository;
    private final GeminiService geminiService;

    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request) {
        User user = authenticatedUserContext.getCurrentUser();
        ChatMessage userMessage = new ChatMessage();
        userMessage.setUser(user);
        userMessage.setConversationId(request.conversationId());
        userMessage.setRole(ChatRole.USER);
        userMessage.setMessage(request.message());
        userMessage.setTimestamp(LocalDateTime.now());
        chatMessageRepository.save(userMessage);

        String aiResponse = geminiService.answerChat(user, request.message());
        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setUser(user);
        aiMessage.setConversationId(request.conversationId());
        aiMessage.setRole(ChatRole.AI);
        aiMessage.setMessage(aiResponse.trim());
        aiMessage.setTimestamp(LocalDateTime.now());
        chatMessageRepository.save(aiMessage);
        return map(aiMessage);
    }

    public ChatHistoryResponse history(String conversationId, int limit) {
        User user = authenticatedUserContext.getCurrentUser();
        List<ChatMessage> messages =
                chatMessageRepository.findByUserAndConversationIdOrderByTimestampAsc(user, conversationId);
        int safeLimit = Math.max(1, limit);
        if (messages.size() > safeLimit) {
            messages = messages.subList(messages.size() - safeLimit, messages.size());
        }
        List<ChatMessageResponse> dtos = messages.stream().map(this::map).collect(Collectors.toList());
        return new ChatHistoryResponse(conversationId, dtos);
    }

    private ChatMessageResponse map(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getRole().name().toLowerCase(),
                message.getMessage(),
                message.getTimestamp());
    }
}
