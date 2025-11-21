package com.softcode.nextstep.security;

import com.softcode.nextstep.domain.user.User;
import com.softcode.nextstep.security.TokenService.DecodedToken;
import com.softcode.nextstep.service.RateLimitService;
import com.softcode.nextstep.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_API_PATHS =
            List.of("/api/auth/verify", "/api/auth/complete-profile", "/api/public");

    private final TokenService tokenService;
    private final UserService userService;
    private final RateLimitService rateLimitService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }
        return PUBLIC_API_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = tokenService.sanitizeAuthorizationHeader(authorization);
        DecodedToken decodedToken = tokenService.extractUserFromToken(token);
        User user = userService.findOrCreate(decodedToken.firebaseUid(), decodedToken.email());
        rateLimitService.validateWithinLimit("api:" + user.getId(), 100, Duration.ofMinutes(1));
        request.setAttribute(AuthenticatedUserContext.ATTRIBUTE, user);
        try {
            filterChain.doFilter(request, response);
        } finally {
            request.removeAttribute(AuthenticatedUserContext.ATTRIBUTE);
        }
    }
}
