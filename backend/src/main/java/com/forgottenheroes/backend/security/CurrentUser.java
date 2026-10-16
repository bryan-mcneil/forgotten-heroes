package com.forgottenheroes.backend.security;

/**
 * Who is calling. Controllers ask this bean for the user id and pass it down; services never read a
 * request. Step 31 replaces the stub with the Cognito {@code sub} claim from the JWT.
 */
public interface CurrentUser {

    String id();
}
