package com.forgottenheroes.engine.testing;

import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.UnitId;
import com.forgottenheroes.engine.resolver.Board;

/** A board with two bands and no run, the shape a battle has; lets targeting and the dispatcher be tested alone. */
public final class TwoSidedBoard implements Board {

    private final Band home;
    private final Band away;
    private int next = 1;

    public TwoSidedBoard(Band home, Band away) {
        this.home = home;
        this.away = away;
    }

    @Override
    public Band band(Side side) {
        return side == Side.HOME ? home : away;
    }

    @Override
    public boolean hasSide(Side side) {
        return true;
    }

    @Override
    public RunState state() {
        return null;
    }

    @Override
    public UnitId nextUnitId() {
        return new UnitId("b" + next++);
    }
}
