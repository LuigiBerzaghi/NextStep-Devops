package com.softcode.nextstep.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteProfileRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 150) String currentJob) {
}

