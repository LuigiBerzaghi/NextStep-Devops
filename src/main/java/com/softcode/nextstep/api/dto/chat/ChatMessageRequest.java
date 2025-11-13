package com.softcode.nextstep.api.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank @Size(max = 500) String message,
        @NotBlank @Size(max = 64) String conversationId) {
}

