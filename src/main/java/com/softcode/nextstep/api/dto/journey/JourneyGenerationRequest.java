package com.softcode.nextstep.api.dto.journey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record JourneyGenerationRequest(
        @NotBlank @Size(max = 150) String desiredJob,
        @NotEmpty List<@Size(max = 100) String> currentSkills,
        @NotEmpty List<@Size(max = 100) String> gaps) {
}

