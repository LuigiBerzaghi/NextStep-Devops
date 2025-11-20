package com.softcode.nextstep.api.dto.journey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JourneyGenerationRequest(
        @NotBlank @Size(max = 150) String desiredJob) {
}
