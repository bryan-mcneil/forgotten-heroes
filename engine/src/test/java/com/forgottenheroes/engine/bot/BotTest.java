package com.forgottenheroes.engine.bot;

import static com.forgottenheroes.engine.testing.TestFixtures.content;
import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.event.EventJson;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.RunStatus;
import com.forgottenheroes.engine.rng.GameRandom;
import com.forgottenheroes.engine.session.GameSession;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BotTest {

    private static final GameSession SESSION = new GameSession(content());
    private static final BotRunner RUNNER = new BotRunner(SESSION);

    /** A pool that starts with a few random bands and grows with every run played, so bots fight bots. */
    private static GhostPool seededPool() {
        GhostPool pool = new GhostPool();
        for (RunSummary run : play(new RandomBot(), 10, GhostProvider.nobody())) {
            pool.addAll(run);
        }
        return pool;
    }

    @Test
    void randomBotFinishesFiftySeededRuns() {
        GhostPool pool = seededPool();
        List<RunSummary> runs = new ArrayList<>();
        for (int seed = 1; seed <= 50; seed++) {
            RunSummary run = RUNNER.playRun(new RandomBot(), seed, pool);
            pool.addAll(run);
            runs.add(run);
        }
        assertThat(runs).allMatch(run -> run.status().isOver());
        assertThat(runs).allMatch(run -> run.turns() >= 1 && run.events() > 10);
        assertThat(runs.stream().mapToInt(RunSummary::violations).sum()).isGreaterThan(0);
    }

    @Test
    void greedyBotFinishesFiftySeededRunsWithFewRefusals() {
        GhostPool pool = seededPool();
        List<RunSummary> runs = new ArrayList<>();
        for (int seed = 1; seed <= 50; seed++) {
            RunSummary run = RUNNER.playRun(new GreedyBot(), seed, pool);
            pool.addAll(run);
            runs.add(run);
        }
        assertThat(runs).allMatch(run -> run.status().isOver());
        assertThat(runs.stream().mapToInt(RunSummary::violations).sum()).isLessThan(50);
        assertThat(runs.stream().mapToInt(RunSummary::actions).average().orElse(0)).isGreaterThan(10);
    }

    @Test
    void aMirrorGhostMostlyDrawsSoTheRunnerStopsAtTheTurnCap() {
        RunSummary run = RUNNER.playRun(new GreedyBot(), 1, GhostProvider.mirror());
        assertThat(run.turns()).isLessThanOrEqualTo(BotRunner.MAX_TURNS + 1);
        assertThat(run.draws()).isGreaterThan(run.wins() + run.losses());
    }

    @Test
    void greedyBeatsRandomMostOfTheTime() {
        GhostPool randomBands = new GhostPool();
        for (RunSummary run : play(new RandomBot(), 60, GhostProvider.nobody())) {
            randomBands.addAll(run);
        }
        assertThat(randomBands.size()).isGreaterThan(100);
        List<RunSummary> greedyRuns = play(new GreedyBot(), 200, randomBands);
        long wins = greedyRuns.stream().mapToLong(RunSummary::wins).sum();
        long battles = greedyRuns.stream().mapToLong(run -> run.battles().size()).sum();
        double winRate = (double) wins / battles;
        System.out.printf("greedy vs random ghosts: %d battles, %.0f%% wins, %d runs won%n", battles, winRate * 100,
            greedyRuns.stream().filter(run -> run.status() == RunStatus.WON).count());
        assertThat(winRate).isGreaterThanOrEqualTo(0.70);
        assertThat(greedyRuns.stream().filter(run -> run.status() == RunStatus.WON).count()).isGreaterThan(greedyRuns.size() / 2);
    }

    @Test
    void aBotRunIsDeterministic() {
        RunSummary first = RUNNER.playRun(new GreedyBot(), 77, GhostProvider.mirror());
        RunSummary second = RUNNER.playRun(new GreedyBot(), 77, GhostProvider.mirror());
        assertThat(first).isEqualTo(second);
        assertThat(EventJson.mapper().writeValueAsString(first.battles())).isEqualTo(EventJson.mapper().writeValueAsString(second.battles()));
        assertThat(RUNNER.playRun(new GreedyBot(), 78, GhostProvider.mirror())).isNotEqualTo(first);
    }

    @Test
    void theRunnerCutsOffABotThatOnlyProposesIllegalMoves() {
        Bot stubborn = new Bot() {
            @Override
            public String name() {
                return "stubborn";
            }

            @Override
            public Action next(RunState state, com.forgottenheroes.engine.content.ContentIndex content, GameRandom rng) {
                return new Action.Sell(4);
            }
        };
        RunSummary run = RUNNER.playRun(stubborn, 1, GhostProvider.nobody());
        assertThat(run.status()).isEqualTo(RunStatus.IN_PROGRESS);
        assertThat(run.actions()).isZero();
        assertThat(run.turns()).isEqualTo(BotRunner.MAX_TURNS + 1);
        assertThat(run.violations()).isEqualTo(BotRunner.MAX_VIOLATIONS_IN_A_ROW * BotRunner.MAX_TURNS);
        assertThat(run.draws()).isEqualTo(BotRunner.MAX_TURNS);
    }

    @Test
    void economyHeroesAreCreditedForTheirGold() {
        List<RunSummary> runs = play(new GreedyBot(), 30, GhostProvider.nobody());
        int merchantGold = runs.stream().mapToInt(run -> run.goldFromAbilities().getOrDefault("merchant", 0)).sum();
        int treasurerGold = runs.stream().mapToInt(run -> run.goldFromAbilities().getOrDefault("treasurer", 0)).sum();
        assertThat(merchantGold + treasurerGold).isGreaterThan(0);
        assertThat(runs).allMatch(run -> run.wins() + run.losses() + run.draws() == run.battles().size());
    }

    @Test
    void theGhostPoolFallsBackToEarlierTurnsThenToAMirror() {
        GhostPool pool = new GhostPool();
        RunState state = SESSION.start("x", 1).state();
        GameRandom rng = new GameRandom(1);
        assertThat(pool.ghostFor(3, state, rng)).isEqualTo(state.toSnapshot());
        RunSummary run = RUNNER.playRun(new GreedyBot(), 5, GhostProvider.nobody());
        pool.addAll(run);
        assertThat(pool.ghostFor(2, state, rng)).isEqualTo(run.battles().get(1).band());
        assertThat(pool.ghostFor(99, state, rng).isEmpty()).isFalse();
    }

    private static List<RunSummary> play(Bot bot, int count, GhostProvider ghosts) {
        List<RunSummary> runs = new ArrayList<>();
        for (int seed = 1; seed <= count; seed++) {
            runs.add(RUNNER.playRun(bot, seed, ghosts));
        }
        return runs;
    }
}
