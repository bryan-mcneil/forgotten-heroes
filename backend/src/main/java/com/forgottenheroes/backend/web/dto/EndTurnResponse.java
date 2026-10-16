package com.forgottenheroes.backend.web.dto;

import com.forgottenheroes.engine.event.GameEvent;
import java.util.List;

/**
 * What ending a turn hands back: the battle on its own (the same shape a replay returns), every
 * event of the turn in order (end-of-turn abilities, the battle, hearts or crowns, the next turn's
 * tavern) for the playback engine, and the state with the next turn already begun.
 */
public record EndTurnResponse(BattleDto battle, List<GameEvent> events, RunStateDto state) {
}
