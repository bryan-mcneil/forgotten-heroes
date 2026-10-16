package com.forgottenheroes.backend.security;

import org.springframework.stereotype.Component;

/** Everyone is {@code "dev"} until Step 31 wires Cognito; enough to play on a laptop. */
@Component
public class DevCurrentUser implements CurrentUser {

    @Override
    public String id() {
        return "dev";
    }
}
