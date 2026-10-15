package com.forgottenheroes.engine.model;

/** Experience points of a hero, 0 to 5. The level is derived from them (GDD §4.4). */
public record Xp(int points) {

    public static final int LEVEL2_AT = 2;
    public static final int LEVEL3_AT = 5;
    public static final int MAX = LEVEL3_AT;

    public static final Xp ZERO = new Xp(0);

    public Xp {
        if (points < 0 || points > MAX) {
            throw new IllegalArgumentException("xp must be 0.." + MAX + ", got " + points);
        }
    }

    public int level() {
        if (points >= LEVEL3_AT) {
            return 3;
        }
        if (points >= LEVEL2_AT) {
            return 2;
        }
        return 1;
    }

    public boolean isMax() {
        return points == MAX;
    }

    /** Adds points, never beyond MAX. */
    public Xp plus(int more) {
        if (more < 0) {
            throw new IllegalArgumentException("cannot add negative xp: " + more);
        }
        return new Xp(Math.min(points + more, MAX));
    }
}
