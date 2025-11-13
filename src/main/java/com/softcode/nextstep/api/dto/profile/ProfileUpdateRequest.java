package com.softcode.nextstep.api.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank @Size(max = 150) String name,
        @Email @NotBlank String email,
        @NotBlank @Size(max = 150) String currentJob) {
}
