package com.forgottenheroes.backend.repo;

import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.session.BandSnapshot;
import java.util.List;

/** One battle as stored for replay (architecture §5): who, the events, the player's result. */
public record BattleLog(String runId, int turn, String opponentName, BandSnapshot opponentBand, List<GameEvent> events, Outcome result) {

    public BattleLog {
        events = List.copyOf(events);
    }
}
