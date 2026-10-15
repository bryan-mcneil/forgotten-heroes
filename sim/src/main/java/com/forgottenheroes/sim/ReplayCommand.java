package com.forgottenheroes.sim;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.bot.GhostProvider;
import com.forgottenheroes.engine.content.ContentLoader;
import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.resolver.RuleViolation;
import com.forgottenheroes.engine.rng.GameRandom;
import com.forgottenheroes.engine.session.GameSession;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * {@code sim replay --file bug.json}: play a seed and an action list again (the file the fuzzer
 * prints, or one copied from the frontend's inspector) and print every event, so a reported bug
 * can be watched line by line on any machine.
 */
@Command(name = "replay", description = "Replay a seed and an action list and print the event story.")
public final class ReplayCommand implements Callable<Integer> {

    @Option(names = "--file", description = "JSON with {\"seed\": …, \"actions\": […]}", required = true)
    Path file;

    @Override
    public Integer call() throws Exception {
        FuzzCommand.Replay replay = Json.read(file, FuzzCommand.Replay.class);
        return replay(new GameSession(ContentLoader.load()), replay.seed(), replay.actions(), System.out);
    }

    /** Plays the actions, printing events and refusals; returns 1 when the engine throws anything else. */
    public static int replay(GameSession session, long seed, List<Action> actions, PrintStream out) {
        GameRandom rng = new GameRandom(seed);
        RunState state = session.start("replay-" + seed, seed).state();
        int n = 0;
        for (Action action : actions) {
            out.println("--- action " + (++n) + ": " + action);
            try {
                List<GameEvent> events = action instanceof Action.EndTurn
                    ? session.endTurn(state, GhostProvider.mirror().ghostFor(state.turn(), state, rng)).events()
                    : session.apply(state, action).events();
                for (GameEvent event : events) {
                    out.println(event);
                }
            } catch (RuleViolation refusal) {
                out.println("refused: " + refusal.getMessage());
            } catch (RuntimeException bug) {
                out.println("ENGINE BUG: " + bug);
                return 1;
            }
        }
        out.println("--- final: " + state);
        return 0;
    }
}
