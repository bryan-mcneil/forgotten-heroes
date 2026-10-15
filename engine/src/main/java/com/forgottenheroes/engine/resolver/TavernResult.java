package com.forgottenheroes.engine.resolver;

import com.forgottenheroes.engine.event.EventLog;
import java.util.List;

/** What one tavern action produced: its events, and the trigger moments abilities may react to. */
public record TavernResult(EventLog events, List<PendingTrigger> triggers) {

    public TavernResult {
        triggers = List.copyOf(triggers);
    }
}
