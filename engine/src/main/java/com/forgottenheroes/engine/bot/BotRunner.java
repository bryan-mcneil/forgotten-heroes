package com.forgottenheroes.engine.bot;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.event.GameEvent.AbilityTriggered;
import com.forgottenheroes.engine.event.GameEvent.GoldChanged;
import com.forgottenheroes.engine.event.GameEvent.GoldReason;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.resolver.RuleViolation;
import com.forgottenheroes.engine.rng.GameRandom;
import com.forgottenheroes.engine.rng.Hash64;
import com.forgottenheroes.engine.session.ActionResult;
import com.forgottenheroes.engine.session.GameSession;
import com.forgottenheroes.engine.session.TurnResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plays one whole run with a bot through the {@link GameSession}, the way the backend will play it
 * with a human: start, actions until the bot ends the turn, battle, repeat until the run is over.
 * Refused actions are counted and the bot is asked again; a bot that stalls is cut off after a few
 * refusals in a row or too many actions in one turn, so a run always finishes.
 */
public final class BotRunner {

    /** After this many refusals in a row the turn is ended for the bot. */
    public static final int MAX_VIOLATIONS_IN_A_ROW = 10;
    /** After this many accepted actions in one turn the turn is ended for the bot. */
    public static final int MAX_ACTIONS_PER_TURN = 60;
    /** A run that somehow never ends is stopped here (a real run ends by turn ~20). */
    public static final int MAX_TURNS = 60;

    private final GameSession session;

    public BotRunner(GameSession session) {
        this.session = session;
    }

    public RunSummary playRun(Bot bot, long seed, GhostProvider ghosts) {
        GameRandom rng = new GameRandom(Hash64.of(seed, Hash64.ofString(bot.name())));
        ActionResult started = session.start("bot-" + bot.name() + "-" + seed, seed);
        RunState state = started.state();
        int events = started.events().size();
        int actions = 0;
        int violations = 0;
        List<RunSummary.TurnRecord> battles = new ArrayList<>();
        Map<String, Integer> goldFromAbilities = new HashMap<>();
        countAbilityGold(started.events(), goldFromAbilities);

        while (!state.status().isOver() && state.turn() <= MAX_TURNS) {
            int actionsThisTurn = 0;
            int refusalsInARow = 0;
            boolean endTurn = false;
            while (!endTurn) {
                Action action = bot.next(state, session.content(), rng);
                if (action instanceof Action.EndTurn || actionsThisTurn >= MAX_ACTIONS_PER_TURN
                        || refusalsInARow >= MAX_VIOLATIONS_IN_A_ROW) {
                    endTurn = true;
                    continue;
                }
                try {
                    ActionResult result = session.apply(state, action);
                    events += result.events().size();
                    countAbilityGold(result.events(), goldFromAbilities);
                    actions++;
                    actionsThisTurn++;
                    refusalsInARow = 0;
                } catch (RuleViolation refused) {
                    violations++;
                    refusalsInARow++;
                }
            }
            int turn = state.turn();
            TurnResult result = session.endTurn(state, ghosts.ghostFor(turn, state, rng));
            events += result.events().size();
            countAbilityGold(result.events(), goldFromAbilities);
            battles.add(new RunSummary.TurnRecord(turn, result.outcome(), result.battle().clashes(), result.ghost()));
        }
        return new RunSummary(seed, bot.name(), state.status(), state.turn(), state.crowns(), state.hearts(), actions, violations,
            battles, goldFromAbilities, events);
    }

    /** Gold an ability earned is credited to the hero whose AbilityTriggered came just before it. */
    private static void countAbilityGold(List<GameEvent> events, Map<String, Integer> goldFromAbilities) {
        String lastHero = null;
        for (GameEvent event : events) {
            if (event instanceof AbilityTriggered fired) {
                lastHero = fired.heroId();
            } else if (event instanceof GoldChanged gold && gold.reason() == GoldReason.ABILITY && lastHero != null) {
                goldFromAbilities.merge(lastHero, gold.after() - gold.before(), Integer::sum);
            }
        }
    }
}
