package com.forgottenheroes.engine.resolver;

import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Unit;

/**
 * A moment that abilities may react to: the trigger, the unit it is about (the bought, sold, fallen
 * or summoned unit; null for moments that concern the whole band, such as the start of a turn) and
 * which side that unit stands on. Noted by the resolver and the effects, fired by the
 * {@link TriggerDispatcher} in the order they were noted.
 */
public record PendingTrigger(Trigger trigger, Unit subject, Side side) {

    public PendingTrigger {
        if (trigger == null) {
            throw new IllegalArgumentException("trigger is required");
        }
        if (side == null) {
            side = Side.HOME;
        }
    }

    public static PendingTrigger of(Trigger trigger) {
        return new PendingTrigger(trigger, null, Side.HOME);
    }

    /** A moment about a unit on the player's band (the tavern). */
    public static PendingTrigger of(Trigger trigger, Unit subject) {
        return new PendingTrigger(trigger, subject, Side.HOME);
    }

    public static PendingTrigger of(Trigger trigger, Unit subject, Side side) {
        return new PendingTrigger(trigger, subject, side);
    }
}
