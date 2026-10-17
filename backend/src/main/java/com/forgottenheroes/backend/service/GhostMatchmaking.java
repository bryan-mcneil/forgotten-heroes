package com.forgottenheroes.backend.service;

import com.forgottenheroes.backend.repo.Ghost;
import com.forgottenheroes.backend.repo.GhostRepository;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Rules;
import com.forgottenheroes.engine.rng.GameRandom;
import com.forgottenheroes.engine.rng.SeedDerivation;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Architecture §4.4 as code. One Query brings up to 25 ghosts of this turn with crowns one either side
 * of the player's; the ladder then goes: never the player's own ghosts, a real player's band if any,
 * else a bot's, else the mirror. The choice is rolled with the run's own dice (the battle seed of this
 * turn, forked so the battle's rolls are untouched), so a replay with the same candidates picks the
 * same ghost. The chosen band is copied onto the battle log by the service, so replays never need the
 * ghost drawer again.
 */
@Service
public class GhostMatchmaking implements MatchmakingService {

    public static final String MIRROR = "Mirror";
    public static final String BOT = "Bot";

    /** Reserved for Step 35: one count per fallback, with {@code kind} (bot or mirror) as the dimension. */
    public static final String FALLBACK_METRIC = "MatchmakingFallback";

    static final int CROWNS_WINDOW = 1;
    static final int CANDIDATES = 25;

    private static final Logger log = LoggerFactory.getLogger(GhostMatchmaking.class);

    private final GhostRepository ghosts;

    public GhostMatchmaking(GhostRepository ghosts) {
        this.ghosts = ghosts;
    }

    @Override
    public Opponent pick(String userId, RunState run) {
        int turn = run.turn();
        int crowns = run.crowns();
        List<Ghost> candidates = ghosts.findCandidates(turn,
                Math.max(0, crowns - CROWNS_WINDOW), Math.min(Rules.CROWNS_TO_WIN, crowns + CROWNS_WINDOW), CANDIDATES)
            .stream()
            .filter(ghost -> !ghost.ownerUserId().equals(userId))                       // 1. never yourself
            .toList();
        GameRandom die = new GameRandom(SeedDerivation.battleSeed(run.runSeed(), turn)).fork("matchmaking");   // 2. replayable

        List<Ghost> players = candidates.stream().filter(ghost -> !ghost.bot()).toList();
        if (!players.isEmpty()) {
            Ghost ghost = die.pick(players);                                               // 3. a real player's band
            return new Opponent("Ghost of " + ghost.ownerUserId(), ghost.band());          //    (display name: Step 32)
        }
        List<Ghost> bots = candidates.stream().filter(Ghost::bot).toList();
        if (!bots.isEmpty()) {
            log.info("{} kind=bot turn={} crowns={}", FALLBACK_METRIC, turn, crowns);
            return new Opponent(BOT, die.pick(bots).band());                               // 4. a seeded bot (Step 72)
        }
        log.info("{} kind=mirror turn={} crowns={}", FALLBACK_METRIC, turn, crowns);
        return new Opponent(MIRROR, run.toSnapshot());                                     // 5. yourself, as a last resort
    }
}
