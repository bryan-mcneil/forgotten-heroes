package com.forgottenheroes.engine.session;

import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.resolver.battle.BattleResult;
import java.util.List;

/**
 * What ending a turn produced: the end-of-turn abilities, the battle, the hearts or crowns, the run's
 * end or the next turn's start, all as one contiguous event list; plus the battle on its own, the
 * player's outcome and the snapshot the backend should store as this player's ghost.
 */
public record TurnResult(RunState state, List<GameEvent> events, BattleResult battle, Outcome outcome, BandSnapshot ghost) {

    public TurnResult {
        events = List.copyOf(events);
    }
}
