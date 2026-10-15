package com.forgottenheroes.engine.model;

import java.util.Objects;

/** How often a unit's ability has fired this turn and this battle, for abilities with a limit. */
public final class Counters {

    private int usesThisTurn;
    private int usesThisBattle;

    public int usesThisTurn() {
        return usesThisTurn;
    }

    public int usesThisBattle() {
        return usesThisBattle;
    }

    /** The ability fired once more: both counters go up. */
    public void recordUse() {
        usesThisTurn++;
        usesThisBattle++;
    }

    public void resetTurn() {
        usesThisTurn = 0;
    }

    public void resetBattle() {
        usesThisBattle = 0;
    }

    public Counters copy() {
        Counters copy = new Counters();
        copy.usesThisTurn = usesThisTurn;
        copy.usesThisBattle = usesThisBattle;
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Counters that && usesThisTurn == that.usesThisTurn && usesThisBattle == that.usesThisBattle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(usesThisTurn, usesThisBattle);
    }

    @Override
    public String toString() {
        return "turn " + usesThisTurn + ", battle " + usesThisBattle;
    }
}
