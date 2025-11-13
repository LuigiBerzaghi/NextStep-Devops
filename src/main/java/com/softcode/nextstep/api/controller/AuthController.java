package com.softcode.nextstep.api.controller;

import com.softcode.nextstep.api.dto.auth.AuthVerifyResponse;
import com.softcode.nextstep.api.dto.auth.CompleteProfileRequest;
import com.softcode.nextstep.api.dto.auth.CompleteProfileResponse;
import com.softcode.nextstep.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/verify")
    public ResponseEntity<AuthVerifyResponse> verify(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return ResponseEntity.ok(authService.verify(authorization));
    }

    @PostMapping("/complete-profile")
    public ResponseEntity<CompleteProfileResponse> completeProfile(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody CompleteProfileRequest request) {
        CompleteProfileResponse response = authService.completeProfile(authorization, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}

