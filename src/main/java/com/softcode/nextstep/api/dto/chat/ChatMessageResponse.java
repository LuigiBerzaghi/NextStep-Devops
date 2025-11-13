package com.softcode.nextstep.api.dto.chat;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageResponse(
        UUID messageId, String conversationId, String role, String message, LocalDateTime timestamp) {
}

