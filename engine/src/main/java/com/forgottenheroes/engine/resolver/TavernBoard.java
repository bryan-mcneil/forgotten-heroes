package com.forgottenheroes.engine.resolver;

import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.UnitId;

/** The tavern as a board: the player's band on the HOME side and nobody opposite. */
public record TavernBoard(RunState state) implements Board {

    @Override
    public Band band(Side side) {
        if (side != Side.HOME) {
            throw new IllegalArgumentException("the tavern has no " + side + " band");
        }
        return state.band();
    }

    @Override
    public boolean hasSide(Side side) {
        return side == Side.HOME;
    }

    @Override
    public UnitId nextUnitId() {
        return state.nextUnitId();
    }
}
