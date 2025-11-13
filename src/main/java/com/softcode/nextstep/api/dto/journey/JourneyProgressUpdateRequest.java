package com.softcode.nextstep.api.dto.journey;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record JourneyProgressUpdateRequest(@Min(0) @Max(100) int progress) {
}

