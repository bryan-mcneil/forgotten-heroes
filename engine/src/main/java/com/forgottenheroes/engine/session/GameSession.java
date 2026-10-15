package com.forgottenheroes.engine.session;

import static com.forgottenheroes.engine.resolver.RuleViolation.require;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.event.GameEvent.HeartsReason;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.BattleSummary;
import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.RunStatus;
import com.forgottenheroes.engine.model.Rules;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.mutator.RunMutator;
import com.forgottenheroes.engine.resolver.RuleViolation.Code;
import com.forgottenheroes.engine.resolver.TavernResolver;
import com.forgottenheroes.engine.resolver.battle.BattleResolver;
import com.forgottenheroes.engine.resolver.battle.BattleResult;
import com.forgottenheroes.engine.rng.SeedDerivation;
import java.util.ArrayList;
import java.util.List;

/**
 * The facade (architecture §3, §6): start a run, apply a player action, end a turn against a ghost.
 * The backend reads "load state → session.apply → save"; the simulator does the same. Every call
 * that changes the state bumps its {@code version}, and every event of a run is numbered in one
 * contiguous sequence.
 */
public final class GameSession {

    private final ContentIndex content;
    private final TavernResolver tavern;
    private final BattleResolver battle;

    public GameSession(ContentIndex content) {
        this.content = content;
        this.tavern = new TavernResolver(content);
        this.battle = new BattleResolver(content);
    }

    public ContentIndex content() {
        return content;
    }

    /** A new run with its first turn begun: 10 gold, the shelves filled, 5 hearts, 0 crowns. */
    public ActionResult start(String runId, long runSeed) {
        RunState state = RunState.newRun(runId, runSeed);
        List<GameEvent> events = tavern.apply(state, new Action.StartTurn()).events().events();
        state.bumpVersion();
        return new ActionResult(state, events);
    }

    /** One tavern action from the player. Turns are started and ended by the engine, never by a client. */
    public ActionResult apply(RunState state, Action action) {
        require(!(action instanceof Action.StartTurn), Code.WRONG_PHASE, "the engine starts turns itself");
        require(!(action instanceof Action.EndTurn), Code.WRONG_PHASE, "end the turn through endTurn(state, ghost)");
        List<GameEvent> events = tavern.apply(state, action).events().events();
        state.bumpVersion();
        return new ActionResult(state, events);
    }

    /**
     * GDD §4.2 step 3 and architecture §6: end-of-turn abilities, the band saved as this player's
     * ghost, the battle against the given ghost (the player is home on odd turns), the result
     * applied, the run ended at ten crowns or zero hearts, otherwise the next turn started and the
     * temporary stats cleared.
     */
    public TurnResult endTurn(RunState state, BandSnapshot ghost) {
        require(!state.status().isOver(), Code.RUN_OVER, "the run is " + state.status());
        require(state.turn() >= 1, Code.WRONG_PHASE, "the first turn has not started");
        List<GameEvent> events = new ArrayList<>(tavern.fire(state, Trigger.END_OF_TURN).events());
        EventLog log = new EventLog(state.lastEventSeq());
        RunMutator.endTurn(state, log);
        BandSnapshot mine = state.toSnapshot();

        int turn = state.turn();
        Side playerSide = turn % 2 == 1 ? Side.HOME : Side.AWAY;
        Band ghostBand = ghost.toBand("g");
        Band home = playerSide == Side.HOME ? state.band() : ghostBand;
        Band away = playerSide == Side.HOME ? ghostBand : state.band();
        BattleResult result = battle.resolve(home, away, SeedDerivation.battleSeed(state.runSeed(), turn), turn, playerSide);
        log.appendAll(result.events().events());

        Outcome outcome = result.playerOutcome();
        switch (outcome) {
            case WIN -> RunMutator.changeCrowns(state, log, 1);
            case LOSS -> RunMutator.changeHearts(state, log, -1, HeartsReason.BATTLE_LOST);
            case DRAW -> { }
        }
        state.setLastBattle(new BattleSummary(turn, outcome, result.clashes()));
        for (Unit unit : state.band().units()) {
            unit.clearTemporary();
        }
        if (state.crowns() >= Rules.CROWNS_TO_WIN) {
            RunMutator.endRun(state, log, RunStatus.WON);
        } else if (state.hearts() <= 0) {
            RunMutator.endRun(state, log, RunStatus.LOST);
        }
        state.setLastEventSeq(log.lastSeq());
        events.addAll(log.events());
        if (!state.status().isOver()) {
            events.addAll(tavern.apply(state, new Action.StartTurn()).events().events());
        }
        state.bumpVersion();
        return new TurnResult(state, events, result, outcome, mine);
    }
}
