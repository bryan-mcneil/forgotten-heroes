package com.forgottenheroes.backend.web.dto;

import com.forgottenheroes.engine.event.GameEvent;
import java.util.List;

/** What one accepted action hands back: the state afterwards and the events that got it there. */
public record ActionResponse(RunStateDto state, List<GameEvent> events) {
}
