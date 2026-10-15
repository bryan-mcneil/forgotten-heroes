package com.forgottenheroes.engine.content;

/**
 * A tweak applied while an effect resolves. {@code ifTargetRank} + {@code multiplier}: the amount is
 * multiplied when the target has that rank (Minotaur hits rank I twice as hard).
 */
public record Condition(Integer ifTargetRank, Integer multiplier) {

    public Condition {
        if (ifTargetRank != null && (ifTargetRank < 1 || ifTargetRank > 6)) {
            throw new IllegalArgumentException("ifTargetRank must be 1..6, got " + ifTargetRank);
        }
        if (multiplier != null && multiplier < 1) {
            throw new IllegalArgumentException("multiplier must be at least 1, got " + multiplier);
        }
        if ((ifTargetRank == null) != (multiplier == null)) {
            throw new IllegalArgumentException("ifTargetRank and multiplier go together");
        }
    }
}
