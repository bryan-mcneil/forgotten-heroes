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
 * Everything an effect needs to know about the moment it runs in. In the tavern {@code state} is the
 * run and {@code band} the player's band; {@code self} is the unit whose ability fires (null for a
 * relic), {@code chosen} the hero the player picked for a relic, {@code triggering} the unit that
 * caused the trigger (the summoned ally, the fallen friend). {@code level} and {@code attack} resolve
 * the DSL amounts.
 */
public record EffectContext(RunState state, Band band, Side side, Band enemies, Unit self, Unit chosen, Unit triggering,
                            int level, int attack, GameRandom rng, EventLog log, List<PendingTrigger> triggers, String source) {

    /** The tavern moment for a relic the player just bought. */
    public static EffectContext forRelic(RunState state, Unit chosen, GameRandom rng, EventLog log, List<PendingTrigger> triggers,
                                         String relicId) {
        return new EffectContext(state, state.band(), Side.HOME, null, null, chosen, null, 1, 0, rng, log, triggers, relicId);
    }

    public boolean inTavern() {
        return state != null;
    }

    public Optional<Unit> selfUnit() {
        return Optional.ofNullable(self);
    }
}
