package com.forgottenheroes.engine.resolver;

import static com.forgottenheroes.engine.resolver.RuleViolation.require;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.EffectDefinition;
import com.forgottenheroes.engine.content.HeroDefinition;
import com.forgottenheroes.engine.content.RelicDefinition;
import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.event.GameEvent.GoldReason;
import com.forgottenheroes.engine.event.GameEvent.HeartsReason;
import com.forgottenheroes.engine.event.GameEvent.HeroBought;
import com.forgottenheroes.engine.event.GameEvent.MergeSource;
import com.forgottenheroes.engine.event.GameEvent.RelicBought;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.ItemKind;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Rules;
import com.forgottenheroes.engine.model.Tavern;
import com.forgottenheroes.engine.model.TavernSlot;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.mutator.BandMutator;
import com.forgottenheroes.engine.mutator.GoldMutator;
import com.forgottenheroes.engine.mutator.RunMutator;
import com.forgottenheroes.engine.mutator.TavernMutator;
import com.forgottenheroes.engine.mutator.UnitMutator;
import com.forgottenheroes.engine.resolver.RuleViolation.Code;
import com.forgottenheroes.engine.rng.GameRandom;
import com.forgottenheroes.engine.rng.SeedDerivation;
import java.util.ArrayList;
import java.util.List;

/**
 * The tavern rules (GDD §4.2–§4.4). {@link #apply} validates the action first, then mutates; a
 * {@link RuleViolation} always leaves the state exactly as it was. Randomness comes from the run
 * seed, the turn and the roll or action index, so the same state and action always produce the
 * same result. Every action ends with a dispatch: the moments it noted (a buy, a sell, a fall) fire
 * the abilities that answer them, in the GDD §4.6 order, cascades included.
 */
public final class TavernResolver {

    private final ContentIndex content;
    private final EffectApplier effects;
    private final TriggerDispatcher dispatcher;

    public TavernResolver(ContentIndex content) {
        this.content = content;
        this.effects = new EffectApplier(content);
        this.dispatcher = new TriggerDispatcher(content);
    }

    public TavernResult apply(RunState state, Action action) {
        require(!state.status().isOver(), Code.RUN_OVER, "the run is " + state.status());
        EventLog log = new EventLog(state.lastEventSeq());
        List<PendingTrigger> triggers = new ArrayList<>();
        GameRandom dice = actionRandom(state);
        switch (action) {
            case Action.StartTurn s -> startTurn(state, log, triggers);
            case Action.Roll r -> roll(state, log);
            case Action.Freeze f -> freeze(state, log, f.kind(), f.slot());
            case Action.Unfreeze u -> unfreeze(state, log, u.kind(), u.slot());
            case Action.Buy b -> buy(state, log, triggers, dice, b.slot(), b.bandIndex());
            case Action.BuyRelic b -> buyRelic(state, log, triggers, dice, b.slot(), b.bandIndex());
            case Action.Sell s -> sell(state, log, triggers, s.bandIndex());
            case Action.Move m -> move(state, log, m.from(), m.to());
            case Action.Merge m -> merge(state, log, triggers, dice, m.from(), m.to());
            case Action.EndTurn e -> throw new IllegalArgumentException("EndTurn is resolved by GameSession.endTurn");
        }
        dispatcher.dispatch(new TavernBoard(state), dice.fork("abilities"), log, triggers);
        state.setLastEventSeq(log.lastSeq());
        return new TavernResult(log, triggers);
    }

    /** Fire a moment on the band outside any action: the session's END_OF_TURN. */
    public EventLog fire(RunState state, Trigger trigger) {
        EventLog log = new EventLog(state.lastEventSeq());
        dispatcher.dispatch(new TavernBoard(state), actionRandom(state).fork("abilities"), log, List.of(PendingTrigger.of(trigger)));
        state.setLastEventSeq(log.lastSeq());
        return log;
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

    /**
     * GDD §4.3–§4.4: 3 gold for the hero on a shelf; it joins the band at the index (the band's size
     * appends), or merges into the same hero already standing there.
     */
    private void buy(RunState state, EventLog log, List<PendingTrigger> triggers, GameRandom dice, int slot, int bandIndex) {
        TavernSlot item = requireItem(state, ItemKind.HERO, slot);
        Band band = state.band();
        require(state.gold() >= item.price(), Code.NOT_ENOUGH_GOLD,
            "the hero costs " + item.price() + " gold, you have " + state.gold());
        require(bandIndex <= band.size(), Code.INVALID_INDEX, "band index " + bandIndex + " is beyond the band (" + band.size() + ")");
        HeroDefinition definition = content.hero(item.itemId());
        boolean merging = bandIndex < band.size() && band.at(bandIndex).heroId().equals(definition.id());
        if (merging) {
            require(BandMutator.canStack(band.at(bandIndex)), Code.MAX_LEVEL, definition.name() + " is already level 3");
        } else {
            require(!band.isFull(), Code.BAND_FULL, "the band already has " + Rules.BAND_SIZE + " heroes");
        }

        // Validated: from here on the state changes. A merged copy never joins the band, so it borrows the survivor's id.
        Unit copy = Unit.fromDefinition(definition, merging ? band.at(bandIndex).id() : state.nextUnitId());
        copy.addPermanentBonus(item.bonus());
        GoldMutator.spend(state, log, item.price(), GoldReason.BUY_HERO);
        TavernMutator.takeHero(state, slot);
        if (merging) {
            Unit survivor = band.at(bandIndex);
            log.append(new HeroBought(0, survivor.id(), survivor.heroId(), slot, bandIndex, copy.stats(), item.price()));
            levelUpIf(BandMutator.stack(band, log, survivor, copy, MergeSource.TAVERN, null), state, log, triggers, dice, survivor);
            triggers.add(PendingTrigger.of(Trigger.BUY, survivor));
        } else {
            BandMutator.buy(state, log, copy, bandIndex, slot, item.price());
            triggers.add(PendingTrigger.of(Trigger.BUY, copy));
            triggers.add(PendingTrigger.of(Trigger.ALLY_SUMMONED, copy));
        }
    }

    /**
     * GDD §6: a relic is paid, taken off the shelf and used at once. Relics that act on "one hero" need
     * the band index of that hero; the others pick their own targets and refuse an index.
     */
    private void buyRelic(RunState state, EventLog log, List<PendingTrigger> triggers, GameRandom dice, int slot, Integer bandIndex) {
        TavernSlot item = requireItem(state, ItemKind.RELIC, slot);
        RelicDefinition relic = content.relic(item.itemId());
        Band band = state.band();
        require(state.gold() >= item.price(), Code.NOT_ENOUGH_GOLD,
            relic.name() + " costs " + item.price() + " gold, you have " + state.gold());
        if (relic.needsChosenTarget()) {
            require(bandIndex != null, Code.TARGET_REQUIRED, relic.name() + " needs a hero to use it on");
            require(bandIndex < band.size(), Code.INVALID_INDEX,
                "band index " + bandIndex + " does not exist (" + band.size() + " heroes)");
        } else {
            require(bandIndex == null, Code.TARGET_NOT_ALLOWED, relic.name() + " picks its own targets");
            require(!band.isEmpty() || !targetsUnits(relic), Code.TARGET_REQUIRED, relic.name() + " needs a hero on the band");
        }
        Unit chosen = bandIndex == null ? null : band.at(bandIndex);

        GoldMutator.spend(state, log, item.price(), GoldReason.BUY_RELIC);
        TavernMutator.takeRelic(state, slot);
        log.append(new RelicBought(0, relic.id(), slot, chosen == null ? null : chosen.id(), item.price()));
        EffectContext context = EffectContext.forRelic(state, chosen, dice.fork("relic"), log, triggers, relic.id());
        for (EffectDefinition effect : relic.effects()) {
            effects.apply(effect, context);
        }
        if (chosen != null && chosen.isAlive()) {
            triggers.add(PendingTrigger.of(Trigger.EAT_RELIC, chosen));
        }
    }

    private static boolean targetsUnits(RelicDefinition relic) {
        return relic.effects().stream().anyMatch(effect -> effect.target() != null);
    }

    /** GDD §4.3: a hero sells for gold equal to its level; selling is not a Fall. */
    private void sell(RunState state, EventLog log, List<PendingTrigger> triggers, int bandIndex) {
        requireBandIndex(state, bandIndex);
        Unit unit = state.band().at(bandIndex);
        triggers.add(PendingTrigger.of(Trigger.SELL, unit));
        BandMutator.sell(state, log, bandIndex);
        GoldMutator.gain(state, log, unit.level(), GoldReason.SELL);
    }

    private void move(RunState state, EventLog log, int from, int to) {
        requireBandIndex(state, from);
        requireBandIndex(state, to);
        require(from != to, Code.INVALID_INDEX, "a hero cannot move onto itself");
        BandMutator.move(state.band(), log, from, to);
    }

    /** GDD §4.4: dropping a hero onto its twin on the band merges them for free. */
    private void merge(RunState state, EventLog log, List<PendingTrigger> triggers, GameRandom dice, int from, int to) {
        requireBandIndex(state, from);
        requireBandIndex(state, to);
        Band band = state.band();
        Unit copy = band.at(from);
        Unit survivor = band.at(to);
        require(copy.heroId().equals(survivor.heroId()), Code.NOT_SAME_HERO, copy.heroId() + " cannot merge with " + survivor.heroId());
        require(BandMutator.canStack(survivor), Code.MAX_LEVEL, survivor.heroId() + " is already level 3");
        band.remove(copy);
        levelUpIf(BandMutator.stack(band, log, survivor, copy, MergeSource.BAND, copy.id()), state, log, triggers, dice, survivor);
    }

    private void levelUpIf(boolean leveled, RunState state, EventLog log, List<PendingTrigger> triggers, GameRandom dice, Unit unit) {
        if (leveled) {
            TavernMutator.openBonusSlot(state, log, content, dice.fork("bonus"));
            triggers.add(PendingTrigger.of(Trigger.LEVEL_UP, unit));
        }
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

    private static void requireBandIndex(RunState state, int index) {
        requireTurnStarted(state);
        require(index < state.band().size(), Code.INVALID_INDEX,
            "band index " + index + " does not exist (" + state.band().size() + " heroes)");
    }

    private static void requireTurnStarted(RunState state) {
        require(state.turn() >= 1, Code.WRONG_PHASE, "the first turn has not started");
    }

    /** The dice for this roll: run seed → turn seed → roll seed (0 = the automatic refill), forked for the tavern. */
    private static GameRandom rollRandom(RunState state) {
        long turnSeed = SeedDerivation.turnSeed(state.runSeed(), state.turn());
        return new GameRandom(SeedDerivation.rollSeed(turnSeed, state.rollsThisTurn())).fork("tavern");
    }

    /** The dice for everything else one action may need (bonus slot, abilities, relics), unique per action. */
    private static GameRandom actionRandom(RunState state) {
        long turnSeed = SeedDerivation.turnSeed(state.runSeed(), Math.max(1, state.turn()));
        return new GameRandom(SeedDerivation.actionSeed(turnSeed, state.lastEventSeq()));
    }
}
