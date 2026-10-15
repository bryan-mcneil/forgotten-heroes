package com.forgottenheroes.engine.content;

/** Who an effect hits (GDD §7). "Ally" and "enemy" are relative to the unit whose ability fires. */
public enum TargetSelect {
    /** The unit whose ability fires. */
    SELF,
    /** The band member the player picked when buying a relic. */
    CHOSEN,
    RANDOM_ALLY,
    RANDOM_ENEMY,
    ALL_ALLIES,
    ALL_ENEMIES,
    ALL_UNITS,
    /** The units directly ahead and behind, on this unit's side. */
    ADJACENT,
    NEAREST_ALLIES_BEHIND,
    NEAREST_ALLIES_AHEAD,
    FIRST_ENEMY,
    LAST_ENEMY,
    LOWEST_HEALTH_ENEMY,
    HIGHEST_HEALTH_ENEMY,
    /** The unit that caused the trigger: the summoned ally for ALLY_SUMMONED, the fallen one for ALLY_FALL. */
    TRIGGERING_UNIT,
    FRONT_ALLY,
    LEVEL_2_PLUS_ALLIES,
    /** Every hero currently for sale in the tavern. */
    TAVERN_HEROES
}
