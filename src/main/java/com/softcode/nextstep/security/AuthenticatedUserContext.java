package com.softcode.nextstep.security;

import com.softcode.nextstep.domain.user.User;
import com.softcode.nextstep.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuthenticatedUserContext {

    public static final String ATTRIBUTE = "NEXTSTEP_AUTHENTICATED_USER";

    public User getCurrentUser() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttributes)) {
            throw new UnauthorizedException("Usuario nao autenticado");
        }
        HttpServletRequest request = servletAttributes.getRequest();
        Object candidate = request.getAttribute(ATTRIBUTE);
        if (candidate instanceof User user) {
            return user;
        }
        throw new UnauthorizedException("Usuario nao autenticado");
    }
}
