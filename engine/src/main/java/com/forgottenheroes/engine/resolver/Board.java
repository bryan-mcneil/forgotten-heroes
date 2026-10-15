package com.forgottenheroes.engine.resolver;

import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.UnitId;

/**
 * What the trigger system sees: one or two bands. In the tavern the HOME band is the player's and
 * there is no AWAY side; in a battle both sides exist and there is no run state.
 */
public interface Board {

    Band band(Side side);

    boolean hasSide(Side side);

    /** The run, in the tavern; null in a battle. */
    RunState state();

    /** A fresh id for a summoned unit. */
    UnitId nextUnitId();

    default boolean inTavern() {
        return state() != null;
    }
}
