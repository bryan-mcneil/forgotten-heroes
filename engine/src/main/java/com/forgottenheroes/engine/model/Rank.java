package com.forgottenheroes.engine.model;

/** Hero tier I to VI. Each rank unlocks in the tavern on a fixed turn (GDD §4.3). */
public enum Rank {
    I(1), II(3), III(5), IV(7), V(9), VI(11);

    private final int turnUnlocked;

    Rank(int turnUnlocked) {
        this.turnUnlocked = turnUnlocked;
    }

    public int turnUnlocked() {
        return turnUnlocked;
    }

    /** The highest rank the tavern offers on this turn. */
    public static Rank availableAt(int turn) {
        if (turn < 1) {
            throw new IllegalArgumentException("turn starts at 1, got " + turn);
        }
        Rank best = I;
        for (Rank rank : values()) {
            if (rank.turnUnlocked <= turn) {
                best = rank;
            }
        }
        return best;
    }

    /** The next rank up; VI stays VI. */
    public Rank next() {
        return this == VI ? VI : values()[ordinal() + 1];
    }
}
