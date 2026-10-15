package com.forgottenheroes.engine.resolver;

import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.rng.GameRandom;
import java.util.List;
import java.util.Optional;

/**
 * Everything an effect needs to know about the moment it runs in: the board and the side it acts
 * from, {@code self} (the unit whose ability fires; null for a relic), {@code chosen} (the hero the
 * player picked for a relic), {@code triggering} (the unit that caused the moment: the summoned ally,
 * the fallen friend), the level and attack that resolve the DSL amounts, the dice, the log, the
 * queue of moments to fire next, and where a "HERE" summon lands ({@code summonIndex}); during a
 * fall, summons are collected in {@code deferredSummons} and placed once the fallen unit is gone.
 */
public record EffectContext(Board board, Side side, Unit self, Unit chosen, Unit triggering, int level, int attack,
                            GameRandom rng, EventLog log, List<PendingTrigger> triggers, String source, int summonIndex,
                            List<Unit> deferredSummons) {

    /** The tavern moment for a relic the player just bought. */
    public static EffectContext forRelic(RunState state, Unit chosen, GameRandom rng, EventLog log, List<PendingTrigger> triggers,
                                         String relicId) {
        Band band = state.band();
        return new EffectContext(new TavernBoard(state), Side.HOME, null, chosen, null, 1, 0, rng, log, triggers, relicId,
            band.size(), null);
    }

    public RunState state() {
        return board.state();
    }

    public Band band() {
        return board.band(side);
    }

    public boolean hasEnemies() {
        return board.hasSide(side.opposite());
    }

    public Band enemies() {
        return board.band(side.opposite());
    }

    public boolean inTavern() {
        return board.inTavern();
    }

    public Optional<Unit> selfUnit() {
        return Optional.ofNullable(self);
    }
}
