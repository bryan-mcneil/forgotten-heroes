package com.forgottenheroes.engine.model;

/** Every number from the rulebook (GDD §4), in one place. Nothing else may hard-code them. */
public final class Rules {

    public static final int START_HEARTS = 5;
    public static final int CROWNS_TO_WIN = 10;
    public static final int MERCY_TURN = 3;

    public static final int GOLD_PER_TURN = 10;
    public static final int HERO_PRICE = 3;
    public static final int RELIC_PRICE = 3;
    public static final int ROLL_PRICE = 1;

    public static final int BAND_SIZE = 5;
    public static final int MAX_STAT = 50;
    public static final int CLASH_CAP = 300;

    private Rules() {
    }
}
