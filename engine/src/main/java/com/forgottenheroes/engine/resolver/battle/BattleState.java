package com.forgottenheroes.engine.resolver.battle;

import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.UnitId;
import com.forgottenheroes.engine.resolver.Board;

/**
 * The board of one battle: fresh copies of both bands (no damage, battle counters at zero,
 * temporary buffs kept), the clash counter, and ids for units summoned during the fight (s1, s2 …).
 * The run itself is never touched; the winner is reported, not applied.
 */
public final class BattleState implements Board {

    private final Band home;
    private final Band away;
    private int clashes;
    private int nextSummonId = 1;

    public BattleState(Band home, Band away) {
        this.home = home.copyForBattle();
        this.away = away.copyForBattle();
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
        return new UnitId("s" + nextSummonId++);
    }

    public int clashes() {
        return clashes;
    }

    public int nextClash() {
        return ++clashes;
    }

    public boolean bothAlive() {
        return home.anyAlive() && away.anyAlive();
    }
}
