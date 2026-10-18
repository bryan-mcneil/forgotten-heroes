package com.forgottenheroes.backend.security.local;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Puts the player named by {@code X-Dev-User} into the security context, exactly where the real doorman
 * puts the token's subject, so {@code SecurityContextCurrentUser} needs no second code path.
 */
final class DevUserHeaderFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Dev-User";
    static final String DEFAULT_USER = "dev";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        String user = header == null || header.isBlank() ? DEFAULT_USER : header.trim();
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(user, HEADER, List.of());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
