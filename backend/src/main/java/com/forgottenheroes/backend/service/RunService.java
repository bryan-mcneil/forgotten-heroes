package com.forgottenheroes.backend.service;

import com.forgottenheroes.backend.repo.BattleLog;
import com.forgottenheroes.backend.repo.BattleLogRepository;
import com.forgottenheroes.backend.repo.Ghost;
import com.forgottenheroes.backend.repo.GhostRepository;
import com.forgottenheroes.backend.repo.Profile;
import com.forgottenheroes.backend.repo.ProfileRepository;
import com.forgottenheroes.backend.repo.RunRepository;
import com.forgottenheroes.backend.repo.VersionConflictException;
import com.forgottenheroes.backend.web.dto.ActionRequest;
import com.forgottenheroes.backend.web.dto.ActionResponse;
import com.forgottenheroes.backend.web.dto.BattleDto;
import com.forgottenheroes.backend.web.dto.DtoMapper;
import com.forgottenheroes.backend.web.dto.EndTurnResponse;
import com.forgottenheroes.backend.web.dto.RunStateDto;
import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.session.ActionResult;
import com.forgottenheroes.engine.session.GameSession;
import com.forgottenheroes.engine.session.TurnResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * The use-cases of a run, each one "load → engine → save" (architecture §6). The service owns no
 * rule: it asks {@link GameSession} and stores what comes back. It knows the repositories only by
 * their interfaces, so the same code runs on a map today and on DynamoDB from Step 28.
 */
@Service
public class RunService {

    private final GameSession session;
    private final RunRepository runs;
    private final GhostRepository ghosts;
    private final BattleLogRepository battleLogs;
    private final ProfileRepository profiles;
    private final ProfileService profileService;
    private final MatchmakingService matchmaking;
    private final SeedSource seeds;
    private final LeaderboardService leaderboard;

    public RunService(GameSession session, RunRepository runs, GhostRepository ghosts, BattleLogRepository battleLogs,
                      ProfileRepository profiles, ProfileService profileService, MatchmakingService matchmaking, SeedSource seeds,
                      LeaderboardService leaderboard) {
        this.session = session;
        this.runs = runs;
        this.ghosts = ghosts;
        this.battleLogs = battleLogs;
        this.profiles = profiles;
        this.profileService = profileService;
        this.matchmaking = matchmaking;
        this.seeds = seeds;
        this.leaderboard = leaderboard;
    }

    /** A new run with turn 1 begun; 409 while another run of this player is still in progress. */
    public RunStateDto start(String userId) {
        Profile profile = profileService.getOrCreate(userId);
        if (profile.activeRunId() != null) {
            boolean stillRunning = runs.find(userId, profile.activeRunId()).map(run -> !run.status().isOver()).orElse(false);
            if (stillRunning) {
                throw new ActiveRunException(profile.activeRunId());
            }
        }
        String runId = UUID.randomUUID().toString();
        RunState state = session.start(runId, seeds.next()).state();
        runs.save(userId, state, RunRepository.NEW_RUN);
        profiles.save(profile.withActiveRun(runId));
        return DtoMapper.toDto(state);
    }

    public RunStateDto get(String userId, String runId) {
        return DtoMapper.toDto(load(userId, runId));
    }

    /**
     * One tavern action; a {@code RuleViolation} from the engine leaves the saved run untouched (422).
     * The save is conditional on the version the client sent (ADR-5): if another request wrote the run
     * in between, the repository refuses and the client gets 409, the same as from the quick check.
     */
    public ActionResponse apply(String userId, String runId, ActionRequest request) {
        RunState state = load(userId, runId);
        requireVersion(state, request.expectedVersion());
        ActionResult result = session.apply(state, DtoMapper.toAction(request));
        runs.save(userId, state, request.expectedVersion());
        return new ActionResponse(DtoMapper.toDto(state), result.events());
    }

    /** Architecture §6: pick the opponent, let the engine play the turn, store ghost, battle and run. */
    public EndTurnResponse endTurn(String userId, String runId, long expectedVersion) {
        RunState state = load(userId, runId);
        requireVersion(state, expectedVersion);
        int turn = state.turn();
        int crowns = state.crowns();
        int hearts = state.hearts();
        Opponent opponent = matchmaking.pick(userId, state);
        Profile profile = profileService.getOrCreate(userId);     // the display name travels with the ghost (Step 32)

        TurnResult result = session.endTurn(state, opponent.band());

        ghosts.save(new Ghost(userId, profile.displayName(), runId, turn, crowns, hearts, result.ghost(), false, Instant.now()));
        BattleLog log = new BattleLog(runId, turn, opponent.name(), opponent.band(), battleEventsOf(result.events()), result.outcome());
        battleLogs.save(log);
        runs.save(userId, state, expectedVersion);
        if (state.status().isOver()) {
            finish(profile, state, turn);
        }
        return new EndTurnResponse(toDto(log), result.events(), DtoMapper.toDto(state));
    }

    public BattleDto battle(String userId, String runId, int turn) {
        load(userId, runId);
        BattleLog log = battleLogs.find(runId, turn).orElseThrow(() -> new NotFoundException("Battle", runId + "/" + turn));
        return toDto(log);
    }

    public RunStateDto abandon(String userId, String runId) {
        RunState state = load(userId, runId);
        long loaded = state.version();                       // no expectedVersion in the request: the loaded one guards
        session.abandon(state);
        runs.save(userId, state, loaded);
        finish(profileService.getOrCreate(userId), state, state.turn());
        return DtoMapper.toDto(state);
    }

    private RunState load(String userId, String runId) {
        return runs.find(userId, runId).orElseThrow(() -> new NotFoundException("Run", runId));
    }

    /**
     * ADR-5, the quick check: the client says which state it acted on; a stale one is refused before
     * the engine runs. Not enough on its own (two requests can both pass it), so the repository checks
     * again inside the write.
     */
    private static void requireVersion(RunState state, long expectedVersion) {
        if (state.version() != expectedVersion) {
            throw new VersionConflictException(state.runId(), expectedVersion, state.version());
        }
    }

    /** A run is over: count it on the profile, keep the best, free the slot, and put a won or lost run on the board. */
    private void finish(Profile profile, RunState state, int turnsPlayed) {
        profiles.save(profile.runFinished(state.crowns()));
        leaderboard.record(profile.userId(), profile.displayName(), state.status(), state.crowns(), turnsPlayed, Instant.now());
    }

    /** The battle's own events, from {@code BattleStarted} to {@code BattleEnded}, with their run-wide numbers. */
    static List<GameEvent> battleEventsOf(List<GameEvent> turnEvents) {
        int start = -1;
        int end = -1;
        for (int i = 0; i < turnEvents.size(); i++) {
            if (turnEvents.get(i) instanceof GameEvent.BattleStarted) {
                start = i;
            } else if (turnEvents.get(i) instanceof GameEvent.BattleEnded) {
                end = i;
            }
        }
        if (start < 0 || end < start) {
            throw new IllegalStateException("a turn ended without a battle");
        }
        return turnEvents.subList(start, end + 1);
    }

    private static BattleDto toDto(BattleLog log) {
        return new BattleDto(log.opponentName(), log.events(), log.result());
    }
}
