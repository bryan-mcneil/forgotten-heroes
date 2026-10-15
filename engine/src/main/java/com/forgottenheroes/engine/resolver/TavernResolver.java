package com.forgottenheroes.engine.resolver;

import static com.forgottenheroes.engine.resolver.RuleViolation.require;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.event.GameEvent.GoldReason;
import com.forgottenheroes.engine.event.GameEvent.HeartsReason;
import com.forgottenheroes.engine.model.ItemKind;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Rules;
import com.forgottenheroes.engine.model.Tavern;
import com.forgottenheroes.engine.model.TavernSlot;
import com.forgottenheroes.engine.mutator.GoldMutator;
import com.forgottenheroes.engine.mutator.RunMutator;
import com.forgottenheroes.engine.mutator.TavernMutator;
import com.forgottenheroes.engine.resolver.RuleViolation.Code;
import com.forgottenheroes.engine.rng.GameRandom;
import com.forgottenheroes.engine.rng.SeedDerivation;
import java.util.ArrayList;
import java.util.List;

/**
 * The tavern rules (GDD §4.2–§4.4). {@link #apply} validates the action first, then mutates; a
 * {@link RuleViolation} always leaves the state exactly as it was. Randomness comes from the run
 * seed, the turn and the roll index, so the same state and action always produce the same shelves.
 */
public final class TavernResolver {

    private final ContentIndex content;

    public TavernResolver(ContentIndex content) {
        this.content = content;
    }

    public TavernResult apply(RunState state, Action action) {
        require(!state.status().isOver(), Code.RUN_OVER, "the run is " + state.status());
        EventLog log = new EventLog(state.lastEventSeq());
        List<PendingTrigger> triggers = new ArrayList<>();
        switch (action) {
            case Action.StartTurn s -> startTurn(state, log, triggers);
            case Action.Roll r -> roll(state, log);
            case Action.Freeze f -> freeze(state, log, f.kind(), f.slot());
            case Action.Unfreeze u -> unfreeze(state, log, u.kind(), u.slot());
            case Action.Buy b -> throw new UnsupportedOperationException("Buy arrives in Step 14");
            case Action.BuyRelic b -> throw new UnsupportedOperationException("BuyRelic arrives in Step 15");
            case Action.Sell s -> throw new UnsupportedOperationException("Sell arrives in Step 14");
            case Action.Move m -> throw new UnsupportedOperationException("Move arrives in Step 14");
            case Action.Merge m -> throw new UnsupportedOperationException("Merge arrives in Step 14");
            case Action.EndTurn e -> throw new IllegalArgumentException("EndTurn is resolved by GameSession.endTurn");
        }
        state.setLastEventSeq(log.lastSeq());
        return new TavernResult(log, triggers);
    }

    /** GDD §4.1–§4.3: next turn, 10 gold, the mercy heart, this turn's slot counts, the automatic refill. */
    private void startTurn(RunState state, EventLog log, List<PendingTrigger> triggers) {
        if (state.turn() == 0) {
            RunMutator.startRun(state, log);
        }
        RunMutator.advanceTurn(state, log, Rules.GOLD_PER_TURN);
        GoldMutator.set(state, log, Rules.GOLD_PER_TURN, GoldReason.TURN_START);
        if (state.turn() == Rules.MERCY_TURN && state.hearts() < Rules.START_HEARTS) {
            RunMutator.changeHearts(state, log, 1, HeartsReason.MERCY);
        }
        TavernMutator.resizeForTurn(state);
        TavernMutator.fill(state, log, content, rollRandom(state), true);
        triggers.add(PendingTrigger.of(Trigger.START_OF_TURN));
    }

    /** GDD §4.3: the first roll of a turn is free, every other one costs 1 gold; frozen items stay. */
    private void roll(RunState state, EventLog log) {
        requireTurnStarted(state);
        boolean free = state.freeRollAvailable();
        require(free || state.gold() >= Rules.ROLL_PRICE, Code.NOT_ENOUGH_GOLD,
            "a roll costs " + Rules.ROLL_PRICE + " gold, you have " + state.gold());
        if (free) {
            state.setFreeRollAvailable(false);
        } else {
            GoldMutator.spend(state, log, Rules.ROLL_PRICE, GoldReason.ROLL);
        }
        state.setRollsThisTurn(state.rollsThisTurn() + 1);
        TavernMutator.fill(state, log, content, rollRandom(state), free);
    }

    private void freeze(RunState state, EventLog log, ItemKind kind, int slot) {
        TavernSlot item = requireItem(state, kind, slot);
        require(!item.frozen(), Code.ALREADY_FROZEN, kind + " slot " + slot + " is already frozen");
        TavernMutator.freeze(state, log, kind, slot);
    }

    private void unfreeze(RunState state, EventLog log, ItemKind kind, int slot) {
        TavernSlot item = requireItem(state, kind, slot);
        require(item.frozen(), Code.NOT_FROZEN, kind + " slot " + slot + " is not frozen");
        TavernMutator.unfreeze(state, log, kind, slot);
    }

    private static TavernSlot requireItem(RunState state, ItemKind kind, int slot) {
        requireTurnStarted(state);
        Tavern tavern = state.tavern();
        int count = kind == ItemKind.HERO ? tavern.heroSlotCount() : tavern.relicSlots().size();
        require(slot < count, Code.INVALID_INDEX, kind + " slot " + slot + " does not exist (" + count + " slots)");
        TavernSlot item = TavernMutator.slot(tavern, kind, slot);
        require(!item.isEmpty(), Code.SLOT_EMPTY, kind + " slot " + slot + " is empty");
        return item;
    }

    private static void requireTurnStarted(RunState state) {
        require(state.turn() >= 1, Code.WRONG_PHASE, "the first turn has not started");
    }

    /** The dice for this roll: run seed → turn seed → roll seed (0 = the automatic refill), forked for the tavern. */
    private static GameRandom rollRandom(RunState state) {
        long turnSeed = SeedDerivation.turnSeed(state.runSeed(), state.turn());
        return new GameRandom(SeedDerivation.rollSeed(turnSeed, state.rollsThisTurn())).fork("tavern");
    }
}
