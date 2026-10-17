package com.forgottenheroes.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgottenheroes.backend.repo.InMemoryBattleLogRepository;
import com.forgottenheroes.backend.repo.InMemoryGhostRepository;
import com.forgottenheroes.backend.repo.InMemoryProfileRepository;
import com.forgottenheroes.backend.repo.InMemoryRunRepository;
import com.forgottenheroes.backend.repo.VersionConflictException;
import com.forgottenheroes.backend.web.dto.ActionRequest;
import com.forgottenheroes.backend.web.dto.ActionResponse;
import com.forgottenheroes.backend.web.dto.EndTurnResponse;
import com.forgottenheroes.backend.web.dto.ProfileDto;
import com.forgottenheroes.backend.web.dto.RunStateDto;
import com.forgottenheroes.engine.content.ContentLoader;
import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.model.RunStatus;
import com.forgottenheroes.engine.resolver.RuleViolation;
import com.forgottenheroes.engine.session.GameSession;
import org.junit.jupiter.api.Test;

/**
 * The use-cases against in-memory repositories: no Spring, no mocks, the real engine. These tests
 * stay this fast forever; the repositories get their own tests against DynamoDB Local in Step 28.
 */
class RunServiceTest {

    private static final GameSession SESSION = new GameSession(ContentLoader.load());

    private final InMemoryRunRepository runs = new InMemoryRunRepository();
    private final InMemoryGhostRepository ghosts = new InMemoryGhostRepository();
    private final InMemoryBattleLogRepository battleLogs = new InMemoryBattleLogRepository();
    private final InMemoryProfileRepository profiles = new InMemoryProfileRepository();
    private final ProfileService profileService = new ProfileService(profiles);
    private final RunService service = new RunService(SESSION, runs, ghosts, battleLogs, profiles, profileService,
        new GhostMatchmaking(ghosts), () -> 42L);

    @Test
    void startBeginsTurnOneAndRecordsTheActiveRunOnTheProfile() {
        RunStateDto run = service.start("bryan");

        assertThat(run.turn()).isEqualTo(1);
        assertThat(run.gold()).isEqualTo(10);
        assertThat(run.hearts()).isEqualTo(5);
        assertThat(run.band().units()).isEmpty();
        assertThat(run.tavern().heroes()).isNotEmpty();
        ProfileDto me = profileService.me("bryan");
        assertThat(me.activeRunId()).isEqualTo(run.runId());
        assertThat(me.runsPlayed()).isZero();
        assertThat(service.get("bryan", run.runId())).isEqualTo(run);
    }

    @Test
    void aSecondStartIsRefusedWhileTheFirstRunIsInProgress() {
        RunStateDto first = service.start("bryan");

        assertThatThrownBy(() -> service.start("bryan"))
            .isInstanceOf(ActiveRunException.class)
            .extracting("activeRunId").isEqualTo(first.runId());
    }

    @Test
    void threeTurnsAgainstTheMirrorPlayOutAndAreStoredForReplay() {
        RunStateDto run = service.start("bryan");
        String runId = run.runId();

        ActionResponse bought = service.apply("bryan", runId, new ActionRequest.Buy(0, 0, run.version()));
        assertThat(bought.state().band().units()).hasSize(1);
        assertThat(bought.state().gold()).isEqualTo(7);
        assertThat(bought.events()).anyMatch(event -> event instanceof GameEvent.HeroBought);

        int wins = 0;
        int losses = 0;
        long version = bought.state().version();
        for (int turn = 1; turn <= 3; turn++) {
            EndTurnResponse ended = service.endTurn("bryan", runId, version);

            assertThat(ended.battle().opponentName()).isEqualTo(GhostMatchmaking.MIRROR);
            assertThat(ended.battle().events().getFirst()).isInstanceOf(GameEvent.BattleStarted.class);
            assertThat(ended.battle().events().getLast()).isInstanceOf(GameEvent.BattleEnded.class);
            assertThat(ended.events()).containsAll(ended.battle().events());
            assertThat(ended.events().getLast()).isInstanceOf(GameEvent.TavernRolled.class);
            assertThat(ended.state().turn()).isEqualTo(turn + 1);
            assertThat(ended.state().status()).isEqualTo(RunStatus.IN_PROGRESS);
            if (ended.battle().result() == Outcome.WIN) {
                wins++;
            } else if (ended.battle().result() == Outcome.LOSS) {
                losses++;
            }
            assertThat(ended.state().crowns()).isEqualTo(wins);
            assertThat(ended.state().hearts()).isEqualTo(5 - losses);
            assertThat(ghosts.findByTurn(turn)).singleElement().satisfies(ghost -> {
                assertThat(ghost.ownerUserId()).isEqualTo("bryan");
                assertThat(ghost.band().size()).isEqualTo(ended.state().band().units().size());
            });
            assertThat(service.battle("bryan", runId, turn).events()).isEqualTo(ended.battle().events());
            version = ended.state().version();
        }
        assertThat(service.get("bryan", runId).lastBattle().turn()).isEqualTo(3);
        assertThatThrownBy(() -> service.battle("bryan", runId, 9)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void aStaleVersionIsRefusedBeforeTheEngineRuns() {
        RunStateDto run = service.start("bryan");

        assertThatThrownBy(() -> service.apply("bryan", run.runId(), new ActionRequest.Roll(run.version() + 5)))
            .isInstanceOf(VersionConflictException.class)
            .extracting("actual").isEqualTo(run.version());
        assertThat(service.get("bryan", run.runId())).isEqualTo(run);
    }

    @Test
    void aRuleViolationLeavesTheSavedRunUntouched() {
        RunStateDto run = service.start("bryan");

        assertThatThrownBy(() -> service.apply("bryan", run.runId(), new ActionRequest.Sell(0, run.version())))
            .isInstanceOf(RuleViolation.class);
        assertThat(service.get("bryan", run.runId())).isEqualTo(run);
    }

    @Test
    void runsAreOnlyVisibleToTheirOwner() {
        RunStateDto run = service.start("bryan");

        assertThatThrownBy(() -> service.get("someone-else", run.runId())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.get("bryan", "no-such-run")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void abandonEndsTheRunCountsItAndFreesTheProfileForANewOne() {
        RunStateDto run = service.start("bryan");

        RunStateDto abandoned = service.abandon("bryan", run.runId());

        assertThat(abandoned.status()).isEqualTo(RunStatus.ABANDONED);
        assertThat(abandoned.version()).isEqualTo(run.version() + 1);
        ProfileDto me = profileService.me("bryan");
        assertThat(me.activeRunId()).isNull();
        assertThat(me.runsPlayed()).isEqualTo(1);
        assertThatThrownBy(() -> service.abandon("bryan", run.runId())).isInstanceOf(RuleViolation.class);
        assertThat(service.start("bryan").runId()).isNotEqualTo(run.runId());
    }

    @Test
    void theSameSeedReplaysTheSameShop() {
        RunStateDto first = service.start("bryan");
        RunStateDto second = service.start("someone-else");

        assertThat(second.tavern()).isEqualTo(first.tavern());
    }
}
