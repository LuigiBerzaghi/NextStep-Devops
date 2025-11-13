package com.softcode.nextstep.api.dto.chat;

import java.util.List;

public record ChatHistoryResponse(String conversationId, List<ChatMessageResponse> messages) {
}

