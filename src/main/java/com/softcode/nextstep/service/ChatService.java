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
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ChatHistoryResponse history(String conversationId, int page, int size) {
        User user = authenticatedUserContext.getCurrentUser();
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        Page<ChatMessage> pageResult = chatMessageRepository.findByUserAndConversationId(
                user,
                conversationId,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "timestamp")));
        List<ChatMessageResponse> dtos = pageResult.getContent().stream()
                .sorted(Comparator.comparing(ChatMessage::getTimestamp))
                .map(this::map)
                .collect(Collectors.toList());
        return new ChatHistoryResponse(
                conversationId, dtos, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements(), pageResult.getTotalPages());
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
