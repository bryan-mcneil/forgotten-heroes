package com.forgottenheroes.backend.service;

/**
 * A request that is well-formed JSON but asks for something the game does not allow: a season that is
 * not a month, a display name that breaks the naming rules. The handler turns it into 400 with the
 * {@code code} given here, so the client can switch on it (architecture §4.2).
 */
public class InvalidRequestException extends RuntimeException {

    private final String code;

    public InvalidRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
