package com.forgottenheroes.engine.model;

/** Identifies one unit for the whole run, so events can say which hero changed. RunState hands them out: u1, u2, u3 ... */
public record UnitId(String value) {

    public UnitId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("unit id cannot be blank");
        }
    }
}
