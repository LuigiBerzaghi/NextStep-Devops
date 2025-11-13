package com.softcode.nextstep.api.controller;

import com.softcode.nextstep.api.dto.profile.DeleteProfileResponse;
import com.softcode.nextstep.api.dto.profile.ProfileResponse;
import com.softcode.nextstep.api.dto.profile.ProfileUpdateRequest;
import com.softcode.nextstep.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponse> profile() {
        return ResponseEntity.ok(profileService.getProfile());
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> update(@Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(request));
    }

    @DeleteMapping
    public ResponseEntity<DeleteProfileResponse> delete() {
        return ResponseEntity.ok(profileService.deleteProfile());
    }
}

