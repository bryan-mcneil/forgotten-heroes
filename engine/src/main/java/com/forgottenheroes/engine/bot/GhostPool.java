package com.forgottenheroes.engine.bot;

import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.rng.GameRandom;
import com.forgottenheroes.engine.session.BandSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bands saved per turn by earlier runs, the way production's ghost table works (GDD §8): a run on
 * turn 5 fights a band someone else ended turn 5 with. Falls back to the nearest earlier turn, then
 * to a mirror of the player's own band, so there is always an opponent.
 */
public final class GhostPool implements GhostProvider {

    private final Map<Integer, List<BandSnapshot>> byTurn = new HashMap<>();

    public void add(int turn, BandSnapshot band) {
        byTurn.computeIfAbsent(turn, key -> new ArrayList<>()).add(band);
    }

    /** Every battle of a finished run, filed under its turn. */
    public void addAll(RunSummary summary) {
        for (RunSummary.TurnRecord record : summary.battles()) {
            add(record.turn(), record.band());
        }
    }

    public int size() {
        return byTurn.values().stream().mapToInt(List::size).sum();
    }

    @Override
    public BandSnapshot ghostFor(int turn, RunState state, GameRandom rng) {
        for (int t = turn; t >= 1; t--) {
            List<BandSnapshot> candidates = byTurn.get(t);
            if (candidates != null && !candidates.isEmpty()) {
                return rng.pick(candidates);
            }
        }
        return state.toSnapshot();
    }
}
