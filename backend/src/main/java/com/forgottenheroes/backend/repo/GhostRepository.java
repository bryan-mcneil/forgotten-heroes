package com.forgottenheroes.backend.repo;

import java.util.List;

/** Ghost bands by turn: the whole drawer, or the slice with similar crowns that matchmaking reads (Step 30). */
public interface GhostRepository {

    void save(Ghost ghost);

    /** Every ghost of that turn, highest crowns first and the newest first among equal crowns. */
    List<Ghost> findByTurn(int turn);

    /**
     * The ghosts of that turn with {@code crownsMin <= crowns <= crownsMax}, in the same order as
     * {@link #findByTurn}, at most {@code limit} of them: the candidates architecture §4.4 picks from.
     * The limit caps what is read, not only what is returned, so a huge drawer costs the same as a small one.
     */
    List<Ghost> findCandidates(int turn, int crownsMin, int crownsMax, int limit);
}
