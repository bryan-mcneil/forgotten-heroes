package com.forgottenheroes.engine.session;

import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.model.RunState;
import java.util.List;

/** What one accepted action produced: the state afterwards (the same object, mutated) and its events. */
public record ActionResult(RunState state, List<GameEvent> events) {

    public ActionResult {
        events = List.copyOf(events);
    }
}
