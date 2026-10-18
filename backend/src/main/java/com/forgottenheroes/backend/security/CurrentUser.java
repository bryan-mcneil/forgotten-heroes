package com.forgottenheroes.backend.security;

/**
 * Who is calling. Controllers ask this bean for the user id and pass it down; services never read a
 * request. Behind Cognito the id is the token's {@code sub} claim; under the {@code local} profile it is
 * the {@code X-Dev-User} header. Both arrive through {@link SecurityContextCurrentUser}.
 */
public interface CurrentUser {

    String id();
}
