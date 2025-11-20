package com.softcode.nextstep.api.dto.journey;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record JourneyStepResponse(
        UUID stepId,
        int order,
        String title,
        String objective,
        String resources,
        List<String> platforms,
        String estimatedTime,
        boolean progress,
        String status,
        LocalDateTime updatedAt) {
}
