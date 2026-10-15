package com.forgottenheroes.engine.bot;

import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.rng.GameRandom;
import com.forgottenheroes.engine.session.BandSnapshot;

/** Where the opponent for a turn comes from: a pool, a mirror of the player's own band, or nobody. */
@FunctionalInterface
public interface GhostProvider {

    BandSnapshot ghostFor(int turn, RunState state, GameRandom rng);

    /** The player fights a copy of their own band. */
    static GhostProvider mirror() {
        return (turn, state, rng) -> state.toSnapshot();
    }

    /** The player fights nobody and wins every turn; useful to test economies. */
    static GhostProvider nobody() {
        return (turn, state, rng) -> BandSnapshot.EMPTY;
    }
}
