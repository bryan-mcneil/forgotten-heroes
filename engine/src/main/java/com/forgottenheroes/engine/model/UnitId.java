package com.forgottenheroes.engine.model;

/** Identifies one unit for the whole run, so events can say which hero changed. */
public record UnitId(String value) {

    public UnitId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("unit id cannot be blank");
        }
    }

    /** Hands out u1, u2, u3 ... in order. Replaced by a seeded factory in Step 09. */
    public static final class Sequence {

        private int next = 1;

        public UnitId next() {
            return new UnitId("u" + next++);
        }
    }
}
