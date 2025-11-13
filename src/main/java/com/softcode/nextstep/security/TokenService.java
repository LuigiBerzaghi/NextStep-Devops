package com.softcode.nextstep.security;

import com.softcode.nextstep.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TokenService {

    public DecodedToken extractUserFromAuthorizationHeader(String authorizationHeader) {
        String token = sanitizeAuthorizationHeader(authorizationHeader);
        return extractUserFromToken(token);
    }

    public String sanitizeAuthorizationHeader(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Authorization header ausente ou invalido");
        }
        return authorizationHeader.substring("Bearer ".length()).trim();
    }

    public DecodedToken extractUserFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("JWT incompleto");
            }
            byte[] decodedPayload = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(decodedPayload, StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(payload);
            String uid = json.optString("user_id", json.optString("uid", null));
            String email = json.optString("email", null);
            if (!StringUtils.hasText(uid) || !StringUtils.hasText(email)) {
                throw new IllegalArgumentException("Campos obrigatorios ausentes no token");
            }
            return new DecodedToken(uid, email.toLowerCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new UnauthorizedException("Token invalido");
        }
    }

    public record DecodedToken(String firebaseUid, String email) {}
}
