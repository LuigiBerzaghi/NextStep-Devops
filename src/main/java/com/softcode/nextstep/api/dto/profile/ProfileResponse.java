package com.softcode.nextstep.api.dto.profile;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProfileResponse(
        UUID userId,
        String name,
        String email,
        String currentJob,
        String profilePicture,
        LocalDateTime createdAt,
        ProfileStatsDto stats) {
}

