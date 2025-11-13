package com.softcode.nextstep.api.controller;

import com.softcode.nextstep.api.dto.chat.ChatHistoryResponse;
import com.softcode.nextstep.api.dto.chat.ChatMessageRequest;
import com.softcode.nextstep.api.dto.chat.ChatMessageResponse;
import com.softcode.nextstep.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    public ResponseEntity<ChatMessageResponse> send(@Valid @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(request));
    }

    @GetMapping("/history")
    public ResponseEntity<ChatHistoryResponse> history(
            @RequestParam String conversationId, @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(chatService.history(conversationId, limit));
    }
}

