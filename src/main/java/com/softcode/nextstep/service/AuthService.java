package com.softcode.nextstep.service;

import com.softcode.nextstep.api.dto.auth.AuthVerifyResponse;
import com.softcode.nextstep.api.dto.auth.CompleteProfileRequest;
import com.softcode.nextstep.api.dto.auth.CompleteProfileResponse;
import com.softcode.nextstep.domain.user.User;
import com.softcode.nextstep.security.TokenService;
import com.softcode.nextstep.security.TokenService.DecodedToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TokenService tokenService;
    private final UserService userService;

    public AuthVerifyResponse verify(String authorizationHeader) {
        String token = tokenService.sanitizeAuthorizationHeader(authorizationHeader);
        DecodedToken decodedToken = tokenService.extractUserFromToken(token);
        User user = userService.findOrCreate(decodedToken.firebaseUid(), decodedToken.email());
        boolean hasActiveJourney = userService.hasActiveJourney(user);
        return new AuthVerifyResponse(
                user.getId(),
                user.getFirebaseUid(),
                user.getEmail(),
                user.getName(),
                user.getCurrentJob(),
                hasActiveJourney,
                user.getCreatedAt());
    }

    public CompleteProfileResponse completeProfile(String authorizationHeader, CompleteProfileRequest request) {
        String token = tokenService.sanitizeAuthorizationHeader(authorizationHeader);
        DecodedToken decodedToken = tokenService.extractUserFromToken(token);
        User user = userService.findOrCreate(decodedToken.firebaseUid(), decodedToken.email());
        User updated = userService.updateProfile(user, request.name(), request.currentJob(), user.getEmail());
        return new CompleteProfileResponse(updated.getId(), updated.getName(), updated.getCurrentJob(), updated.getUpdatedAt());
    }
}
