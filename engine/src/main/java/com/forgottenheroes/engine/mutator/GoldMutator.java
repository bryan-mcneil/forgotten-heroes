package com.forgottenheroes.engine.mutator;

import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.event.GameEvent.GoldChanged;
import com.forgottenheroes.engine.event.GameEvent.GoldReason;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Rules;

/** Every change to the player's gold goes through here and leaves a {@code GoldChanged} behind. */
public final class GoldMutator {

    private GoldMutator() { }

    /** Set gold to an exact amount (the start-of-turn refill). */
    public static void set(RunState state, EventLog log, int amount, GoldReason reason) {
        int before = state.gold();
        state.setGold(amount);
        log.append(new GoldChanged(0, before, amount, reason));
    }

    /** Take gold away; the resolver has already checked there is enough. */
    public static void spend(RunState state, EventLog log, int amount, GoldReason reason) {
        if (amount < 0) {
            throw new IllegalArgumentException("cannot spend a negative amount: " + amount);
        }
        set(state, log, state.gold() - amount, reason);
    }

    /** Add gold, never beyond the cap. */
    public static void gain(RunState state, EventLog log, int amount, GoldReason reason) {
        if (amount < 0) {
            throw new IllegalArgumentException("cannot gain a negative amount: " + amount);
        }
        set(state, log, Math.min(state.gold() + amount, Rules.MAX_GOLD), reason);
    }
}
