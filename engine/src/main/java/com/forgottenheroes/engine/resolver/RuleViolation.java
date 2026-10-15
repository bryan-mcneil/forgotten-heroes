package com.forgottenheroes.engine.resolver;

import java.util.Objects;

/**
 * An action the rules do not allow. Thrown by a resolver before any state changes, so the state
 * after a violation is exactly the state before it. The {@code code} is stable text the API and the
 * frontend can switch on; the message is for humans.
 */
public class RuleViolation extends RuntimeException {

    public enum Code {
        RUN_OVER,
        WRONG_PHASE,
        NOT_ENOUGH_GOLD,
        SLOT_EMPTY,
        INVALID_INDEX,
        ALREADY_FROZEN,
        NOT_FROZEN,
        BAND_FULL,
        NOT_SAME_HERO,
        MAX_LEVEL,
        TARGET_REQUIRED,
        TARGET_NOT_ALLOWED,
        ENGINE_GUARD
    }

    private final Code code;

    public RuleViolation(Code code, String message) {
        super(code + ": " + message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public Code code() {
        return code;
    }

    /** Throws unless the condition holds; the resolvers read like the rulebook with it. */
    public static void require(boolean condition, Code code, String message) {
        if (!condition) {
            throw new RuleViolation(code, message);
        }
    }
}
