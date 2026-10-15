package com.forgottenheroes.engine.content;

/** The moment an ability fires (GDD §7). Tavern triggers first, then battle triggers. */
public enum Trigger {
    START_OF_TURN,
    END_OF_TURN,
    BUY,
    SELL,
    LEVEL_UP,
    ALLY_SUMMONED,
    EAT_RELIC,
    START_OF_BATTLE,
    BEFORE_ATTACK,
    AFTER_ATTACK,
    HURT,
    FALL,
    ALLY_FALL,
    ALLY_AHEAD_FALLS,
    ALLY_AHEAD_ATTACKS,
    KNOCKOUT,
    SUMMONED
}
