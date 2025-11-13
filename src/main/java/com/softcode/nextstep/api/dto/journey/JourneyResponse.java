package com.softcode.nextstep.api.dto.journey;

import com.softcode.nextstep.api.dto.common.InsightDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record JourneyResponse(
        UUID journeyId,
        String desiredJob,
        int totalSteps,
        int completedSteps,
        String estimatedTime,
        int overallProgress,
        String status,
        JourneyStepResponse nextStep,
        List<JourneyStepResponse> steps,
        List<InsightDto> insights,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

