package com.forgottenheroes.engine.mutator;

import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.event.GameEvent.CrownsChanged;
import com.forgottenheroes.engine.event.GameEvent.HeartsChanged;
import com.forgottenheroes.engine.event.GameEvent.HeartsReason;
import com.forgottenheroes.engine.event.GameEvent.RunEnded;
import com.forgottenheroes.engine.event.GameEvent.RunStarted;
import com.forgottenheroes.engine.event.GameEvent.TurnEnded;
import com.forgottenheroes.engine.event.GameEvent.TurnStarted;
import com.forgottenheroes.engine.model.Rank;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.RunStatus;
import com.forgottenheroes.engine.model.Tavern;
import com.forgottenheroes.engine.model.Unit;

/** Changes to the run itself: turns, hearts, crowns, the end. */
public final class RunMutator {

    private RunMutator() { }

    public static void startRun(RunState state, EventLog log) {
        log.append(new RunStarted(0, state.runId()));
    }

    /** Advance to the next turn: the counter, the free roll, the per-turn ability counters, and the announcement. */
    public static void advanceTurn(RunState state, EventLog log, int gold) {
        int turn = state.turn() + 1;
        state.setTurn(turn);
        state.setRollsThisTurn(0);
        state.setFreeRollAvailable(true);
        for (Unit unit : state.band().units()) {
            unit.counters().resetTurn();
        }
        log.append(new TurnStarted(0, turn, gold, Rank.availableAt(turn), Tavern.heroSlotsFor(turn), Tavern.relicSlotsFor(turn)));
    }

    public static void endTurn(RunState state, EventLog log) {
        log.append(new TurnEnded(0, state.turn()));
    }

    public static void changeHearts(RunState state, EventLog log, int delta, HeartsReason reason) {
        int before = state.hearts();
        state.setHearts(before + delta);
        log.append(new HeartsChanged(0, before, state.hearts(), reason));
    }

    public static void changeCrowns(RunState state, EventLog log, int delta) {
        int before = state.crowns();
        state.setCrowns(before + delta);
        log.append(new CrownsChanged(0, before, state.crowns()));
    }

    public static void endRun(RunState state, EventLog log, RunStatus status) {
        if (!status.isOver()) {
            throw new IllegalArgumentException("a run ends WON, LOST or ABANDONED, not " + status);
        }
        state.setStatus(status);
        log.append(new RunEnded(0, status, state.turn(), state.crowns(), state.hearts()));
    }
}
