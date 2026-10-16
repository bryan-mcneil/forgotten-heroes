package com.forgottenheroes.backend.web.dto;

import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.model.Outcome;
import java.util.List;

/** One battle, ready to play back: who, every event, and the player's result. */
public record BattleDto(String opponentName, List<GameEvent> events, Outcome result) {
}
