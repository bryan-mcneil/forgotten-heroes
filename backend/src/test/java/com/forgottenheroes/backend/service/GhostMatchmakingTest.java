package com.forgottenheroes.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.backend.repo.Ghost;
import com.forgottenheroes.backend.repo.InMemoryGhostRepository;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.session.BandSnapshot;
import com.forgottenheroes.engine.session.UnitSnapshot;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The ladder of architecture §4.4 on an in-memory drawer: window, never self, players, bots, mirror, replayable. */
class GhostMatchmakingTest {

    private static final Instant NOON = Instant.parse("2026-10-16T12:00:00Z");
    private static final int TURN = 3;

    private final InMemoryGhostRepository ghosts = new InMemoryGhostRepository();
    private final GhostMatchmaking matchmaking = new GhostMatchmaking(ghosts);

    private static BandSnapshot band(String hero) {
        return new BandSnapshot(List.of(new UnitSnapshot(hero, 2, 3, 0, null)));
    }

    /** A run about to end this turn with these crowns; the setters are the test's shortcut past playing. */
    private static RunState runAt(int turn, int crowns, long seed) {
        RunState run = RunState.newRun("run-" + seed, seed);
        run.setTurn(turn);
        run.setCrowns(crowns);
        return run;
    }

    private Map<String, Ghost> tenPlayersWithCrownsZeroToFour() {
        Map<String, Ghost> byOwner = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            Ghost ghost = new Ghost("p" + i, "r" + i, TURN, i % 5, 5, band("hero" + i), false, NOON.plusSeconds(i));
            ghosts.save(ghost);
            byOwner.put(ghost.ownerUserId(), ghost);
        }
        return byOwner;
    }

    @Test
    void picksWithinOneCrownOfThePlayerAndNeverThePlayerItself() {
        Map<String, Ghost> byOwner = tenPlayersWithCrownsZeroToFour();
        for (int crowns = 0; crowns <= 4; crowns++) {
            ghosts.save(new Ghost("me", "mine-" + crowns, TURN, crowns, 5, band("mine"), false, NOON.plusSeconds(100)));
        }

        for (int crowns = 0; crowns <= 4; crowns++) {
            Opponent opponent = matchmaking.pick("me", runAt(TURN, crowns, 42L + crowns));

            assertThat(opponent.name()).startsWith("Ghost of p");
            Ghost chosen = byOwner.get(opponent.name().substring("Ghost of ".length()));
            assertThat(chosen.crowns()).as("crowns %d", crowns).isBetween(crowns - 1, crowns + 1);
            assertThat(opponent.band()).isEqualTo(chosen.band());
        }
    }

    @Test
    void theSameRunPicksTheSameGhostAgain() {
        tenPlayersWithCrownsZeroToFour();

        Opponent first = matchmaking.pick("me", runAt(TURN, 2, 7L));
        Opponent again = matchmaking.pick("me", runAt(TURN, 2, 7L));

        assertThat(again).isEqualTo(first);
    }

    @Test
    void anEmptyPoolGivesTheMirror() {
        RunState run = runAt(TURN, 2, 42L);

        Opponent opponent = matchmaking.pick("me", run);

        assertThat(opponent.name()).isEqualTo(GhostMatchmaking.MIRROR);
        assertThat(opponent.band()).isEqualTo(run.toSnapshot());
    }

    @Test
    void onlyMyOwnGhostsGiveTheMirrorToo() {
        ghosts.save(new Ghost("me", "mine", TURN, 2, 5, band("mine"), false, NOON));

        assertThat(matchmaking.pick("me", runAt(TURN, 2, 42L)).name()).isEqualTo(GhostMatchmaking.MIRROR);
    }

    @Test
    void botsAreUsedOnlyWhenNoPlayerIsInTheWindow() {
        ghosts.save(new Ghost("bot-1", "b1", TURN, 4, 5, band("bot"), true, NOON));
        ghosts.save(new Ghost("bot-2", "b2", TURN, 4, 5, band("bot"), true, NOON.plusSeconds(1)));
        ghosts.save(new Ghost("ann", "a1", TURN, 0, 5, band("ann"), false, NOON));

        assertThat(matchmaking.pick("me", runAt(TURN, 4, 42L)).name()).as("only bots near 4 crowns").isEqualTo(GhostMatchmaking.BOT);
        assertThat(matchmaking.pick("me", runAt(TURN, 1, 42L)).name()).as("a player near 1 crown").isEqualTo("Ghost of ann");
        assertThat(matchmaking.pick("me", runAt(TURN, 2, 42L)).name()).as("nobody near 2 crowns").isEqualTo(GhostMatchmaking.MIRROR);
    }

    @Test
    void otherTurnsAreOtherDrawers() {
        ghosts.save(new Ghost("ann", "a1", TURN + 1, 2, 5, band("ann"), false, NOON));

        assertThat(matchmaking.pick("me", runAt(TURN, 2, 42L)).name()).isEqualTo(GhostMatchmaking.MIRROR);
        assertThat(matchmaking.pick("me", runAt(TURN + 1, 2, 42L)).name()).isEqualTo("Ghost of ann");
    }
}
