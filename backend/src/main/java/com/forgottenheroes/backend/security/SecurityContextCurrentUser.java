package com.forgottenheroes.backend.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * The one answer to "who is calling": whatever the doorman put in the security context for this
 * request. Behind Cognito that is the token's {@code sub}, the player's stable id; under the {@code local}
 * profile it is the {@code X-Dev-User} header. Controllers call {@link #id()} and never see the difference.
 */
@Component
public class SecurityContextCurrentUser implements CurrentUser {

    @Override
    public String id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            // only reachable from a door that is not behind the doorman: a wiring bug, not a client error
            throw new IllegalStateException("no signed-in user on this request; the route must require authentication");
        }
        return auth.getName();
    }
}
