package com.softcode.nextstep.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationMessage(String type, UUID userId, Map<String, Object> payload, Instant createdAt) {

    public NotificationMessage(String type, UUID userId, Map<String, Object> payload) {
        this(type, userId, payload, Instant.now());
    }
}
