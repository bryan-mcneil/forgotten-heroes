package com.forgottenheroes.engine.model;

/** What the run remembers of its last battle: which turn, how it went for the player, how long it took. */
public record BattleSummary(int turn, Outcome outcome, int clashes) {

    public BattleSummary {
        if (turn < 1) {
            throw new IllegalArgumentException("turns start at 1: " + turn);
        }
        if (outcome == null) {
            throw new IllegalArgumentException("outcome is required");
        }
        if (clashes < 0) {
            throw new IllegalArgumentException("clashes cannot be negative: " + clashes);
        }
    }
}
