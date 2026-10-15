package com.forgottenheroes.engine.resolver;

import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.model.UnitId;

/**
 * A moment that abilities may react to, noted by the tavern resolver and fired by the trigger
 * dispatcher (Step 16). {@code subject} is the unit the moment is about (the bought, sold or fallen
 * unit), or null for moments that concern the whole band (start and end of turn).
 */
public record PendingTrigger(Trigger trigger, UnitId subject) {

    public PendingTrigger {
        if (trigger == null) {
            throw new IllegalArgumentException("trigger is required");
        }
    }

    public static PendingTrigger of(Trigger trigger) {
        return new PendingTrigger(trigger, null);
    }

    public static PendingTrigger of(Trigger trigger, UnitId subject) {
        return new PendingTrigger(trigger, subject);
    }
}
