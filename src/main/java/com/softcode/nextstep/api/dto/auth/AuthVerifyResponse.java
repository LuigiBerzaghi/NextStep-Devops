package com.softcode.nextstep.api.dto.auth;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuthVerifyResponse(
        UUID userId,
        String firebaseUid,
        String email,
        String name,
        String currentJob,
        boolean hasActiveJourney,
        LocalDateTime createdAt) {
}

