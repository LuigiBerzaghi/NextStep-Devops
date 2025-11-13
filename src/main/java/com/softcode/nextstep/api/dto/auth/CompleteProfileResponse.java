package com.softcode.nextstep.api.dto.auth;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompleteProfileResponse(UUID userId, String name, String currentJob, LocalDateTime updatedAt) {
}

