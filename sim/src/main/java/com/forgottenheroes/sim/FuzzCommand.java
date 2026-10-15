package com.forgottenheroes.sim;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.bot.GhostProvider;
import com.forgottenheroes.engine.content.ContentLoader;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.resolver.RuleViolation;
import com.forgottenheroes.engine.rng.GameRandom;
import com.forgottenheroes.engine.session.GameSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * {@code sim fuzz --actions N --seed S}: throws random actions at {@code GameSession}. A
 * {@code RuleViolation} is the engine saying no, which is fine; any other exception, or a broken
 * invariant, is a bug, and the seed plus the actions that led there are printed as a replay file.
 */
@Command(name = "fuzz", description = "Throw random legal and illegal actions at the engine; any exception but a RuleViolation is a bug.")
public final class FuzzCommand implements Callable<Integer> {

    @Option(names = "--actions", description = "how many actions to try (default ${DEFAULT-VALUE})", defaultValue = "100000")
    int actions;

    @Option(names = "--seed", description = "seed (default ${DEFAULT-VALUE})", defaultValue = "7")
    long seed;

    /** The outcome of a fuzz session, also used by the tests. */
    public record Outcome(int tried, int accepted, int refused, int runs, String failure) {
        public boolean clean() {
            return failure == null;
        }
    }

    @Override
    public Integer call() {
        Outcome outcome = fuzz(new GameSession(ContentLoader.load()), actions, seed);
        System.out.printf(Locale.ROOT, "%d actions tried over %d runs: %d accepted, %d refused%n", outcome.tried(), outcome.runs(),
            outcome.accepted(), outcome.refused());
        if (!outcome.clean()) {
            System.out.println(outcome.failure());
            return 1;
        }
        System.out.println("0 engine exceptions");
        return 0;
    }

    public static Outcome fuzz(GameSession session, int budget, long seed) {
        GameRandom rng = new GameRandom(seed);
        int tried = 0;
        int accepted = 0;
        int refused = 0;
        int runs = 0;
        while (tried < budget) {
            long runSeed = seed * 31L + runs;
            runs++;
            RunState state = session.start("fuzz-" + runSeed, runSeed).state();
            List<Action> history = new ArrayList<>();
            while (tried < budget && !state.status().isOver() && state.turn() <= 40) {
                Action action = ActionGenerator.next(rng);
                tried++;
                history.add(action);
                try {
                    if (action instanceof Action.EndTurn) {
                        session.endTurn(state, GhostProvider.mirror().ghostFor(state.turn(), state, rng));
                    } else {
                        session.apply(state, action);
                    }
                    accepted++;
                    state.assertInvariants();
                } catch (RuleViolation refusal) {
                    refused++;
                } catch (RuntimeException | AssertionError bug) {
                    return new Outcome(tried, accepted, refused, runs, replayFile(runSeed, history, bug));
                }
            }
        }
        return new Outcome(tried, accepted, refused, runs, null);
    }

    private static String replayFile(long runSeed, List<Action> history, Throwable bug) {
        Replay replay = new Replay(runSeed, history);
        return "ENGINE BUG: " + bug + "\nsave this as a replay file and run `sim replay --file <it>`:\n"
            + Json.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(replay);
    }

    /** A seed and the actions that followed it: the whole bug report. */
    public record Replay(long seed, List<Action> actions) { }
}
