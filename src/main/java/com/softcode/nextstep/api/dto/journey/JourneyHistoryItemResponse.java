package com.softcode.nextstep.api.dto.journey;

import java.time.LocalDateTime;
import java.util.UUID;

public record JourneyHistoryItemResponse(
        UUID journeyId, String desiredJob, LocalDateTime completedAt, int overallProgress, int totalSteps) {
}

