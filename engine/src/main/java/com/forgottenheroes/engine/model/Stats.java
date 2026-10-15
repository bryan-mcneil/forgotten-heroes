package com.forgottenheroes.engine.model;

/**
 * A hero's attack and health. Immutable: every change returns a new Stats.
 * Negative values are impossible. Values above Rules.MAX_STAT are allowed until clamp() is called.
 */
public record Stats(int attack, int health) {

    public static final Stats ZERO = new Stats(0, 0);

    /** Compact constructor: runs before the values are stored. */
    public Stats {
        if (attack < 0 || health < 0) {
            throw new IllegalArgumentException("stats cannot be negative: " + attack + "/" + health);
        }
    }

    public Stats plus(Stats other) {
        return new Stats(attack + other.attack, health + other.health);
    }

    public Stats withAttack(int newAttack) {
        return new Stats(newAttack, health);
    }

    public Stats withHealth(int newHealth) {
        return new Stats(attack, newHealth);
    }

    public boolean isZero() {
        return attack == 0 && health == 0;
    }

    public Stats clamp() {
        return new Stats(Math.min(attack, Rules.MAX_STAT), Math.min(health, Rules.MAX_STAT));
    }

    @Override
    public String toString() {
        return attack + "/" + health;
    }
}
