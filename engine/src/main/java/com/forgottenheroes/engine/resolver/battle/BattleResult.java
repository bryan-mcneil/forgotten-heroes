package com.forgottenheroes.engine.resolver.battle;

import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.event.GameEvent.BattleEndReason;
import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.model.Side;

/** How a battle went: its events, the outcome for the home side and for the player, why it stopped, how long it took. */
public record BattleResult(EventLog events, Outcome homeOutcome, Side playerSide, BattleEndReason reason, int clashes) {

    /** The outcome as the player experiences it: flipped when the player fought as the away side. */
    public Outcome playerOutcome() {
        return playerSide == Side.HOME ? homeOutcome : homeOutcome.flipped();
    }
}
