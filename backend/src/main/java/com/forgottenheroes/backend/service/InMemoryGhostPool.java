package com.forgottenheroes.backend.service;

import com.forgottenheroes.engine.session.BandSnapshot;
import org.springframework.stereotype.Service;

/** The last fallback of architecture §4.4 and nothing else: you fight a copy of your own band. */
@Service
public class InMemoryGhostPool implements MatchmakingService {

    public static final String MIRROR = "Mirror";

    @Override
    public Opponent pick(String userId, int turn, int crowns, BandSnapshot mine) {
        return new Opponent(MIRROR, mine);
    }
}
