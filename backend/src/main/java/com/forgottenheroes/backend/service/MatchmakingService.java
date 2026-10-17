package com.forgottenheroes.backend.service;

import com.forgottenheroes.engine.model.RunState;

/**
 * Picks the opponent for the turn the run is about to end (architecture §4.4): a ghost with similar
 * crowns, else a bot, else a mirror of the player's own band. The run is passed whole because the
 * pick is made with the run's own dice, so a replay chooses the same ghost from the same candidates.
 */
public interface MatchmakingService {

    Opponent pick(String userId, RunState run);
}
