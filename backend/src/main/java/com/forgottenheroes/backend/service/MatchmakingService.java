package com.forgottenheroes.backend.service;

import com.forgottenheroes.engine.session.BandSnapshot;

/**
 * Picks the opponent for a turn (architecture §4.4): a ghost with similar crowns, else a bot, else a
 * mirror of the player's own band. Step 27 ships the mirror only; Step 30 adds the rest.
 */
public interface MatchmakingService {

    Opponent pick(String userId, int turn, int crowns, BandSnapshot mine);
}
