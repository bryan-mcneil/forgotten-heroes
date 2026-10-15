package com.forgottenheroes.engine.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.model.ItemKind;
import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.Position;
import com.forgottenheroes.engine.model.Rank;
import com.forgottenheroes.engine.model.RunStatus;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.TavernSlot;
import com.forgottenheroes.engine.model.UnitId;
import java.util.List;

/**
 * A fact about what happened, in the past tense, numbered in order. Mutators emit them, the
 * backend stores and returns them, the frontend animates them. Every record carries before and
 * after values where they matter, so the client never recomputes a rule. The JSON {@code "type"}
 * field is the record's simple name.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(GameEvent.RunStarted.class),
    @JsonSubTypes.Type(GameEvent.TurnStarted.class),
    @JsonSubTypes.Type(GameEvent.TurnEnded.class),
    @JsonSubTypes.Type(GameEvent.GoldChanged.class),
    @JsonSubTypes.Type(GameEvent.TavernRolled.class),
    @JsonSubTypes.Type(GameEvent.TavernItemFrozen.class),
    @JsonSubTypes.Type(GameEvent.TavernItemUnfrozen.class),
    @JsonSubTypes.Type(GameEvent.TavernBuffed.class),
    @JsonSubTypes.Type(GameEvent.RelicStocked.class),
    @JsonSubTypes.Type(GameEvent.HeroBought.class),
    @JsonSubTypes.Type(GameEvent.RelicBought.class),
    @JsonSubTypes.Type(GameEvent.HeroSold.class),
    @JsonSubTypes.Type(GameEvent.HeroesReordered.class),
    @JsonSubTypes.Type(GameEvent.HeroesMerged.class),
    @JsonSubTypes.Type(GameEvent.XpGained.class),
    @JsonSubTypes.Type(GameEvent.LevelUp.class),
    @JsonSubTypes.Type(GameEvent.StatsChanged.class),
    @JsonSubTypes.Type(GameEvent.PerkGained.class),
    @JsonSubTypes.Type(GameEvent.PerkConsumed.class),
    @JsonSubTypes.Type(GameEvent.AbilityTriggered.class),
    @JsonSubTypes.Type(GameEvent.BattleStarted.class),
    @JsonSubTypes.Type(GameEvent.Clash.class),
    @JsonSubTypes.Type(GameEvent.DamageDealt.class),
    @JsonSubTypes.Type(GameEvent.Knockout.class),
    @JsonSubTypes.Type(GameEvent.UnitFell.class),
    @JsonSubTypes.Type(GameEvent.UnitRevived.class),
    @JsonSubTypes.Type(GameEvent.UnitSummoned.class),
    @JsonSubTypes.Type(GameEvent.SummonFailed.class),
    @JsonSubTypes.Type(GameEvent.LineShifted.class),
    @JsonSubTypes.Type(GameEvent.BattleEnded.class),
    @JsonSubTypes.Type(GameEvent.HeartsChanged.class),
    @JsonSubTypes.Type(GameEvent.CrownsChanged.class),
    @JsonSubTypes.Type(GameEvent.RunEnded.class)
})
public sealed interface GameEvent {

    /** The event's place in the run's log, from 1; 0 until the log assigns it. */
    int seq();

    /** The same fact with its sequence number, assigned by {@link EventLog#append}. */
    GameEvent withSeq(int seq);

    // ----- run and turn -------------------------------------------------------------------------

    /** A run began: the first fact of every log. */
    record RunStarted(int seq, String runId) implements GameEvent {
        public RunStarted withSeq(int s) { return new RunStarted(s, runId); }
    }

    /** A turn began: gold refilled, the tavern's rank and slot counts for this turn. */
    record TurnStarted(int seq, int turn, int gold, Rank rank, int heroSlots, int relicSlots) implements GameEvent {
        public TurnStarted withSeq(int s) { return new TurnStarted(s, turn, gold, rank, heroSlots, relicSlots); }
    }

    /** The player ended the tavern phase; the battle follows. */
    record TurnEnded(int seq, int turn) implements GameEvent {
        public TurnEnded withSeq(int s) { return new TurnEnded(s, turn); }
    }

    /** Gold went from before to after, and why. */
    record GoldChanged(int seq, int before, int after, GoldReason reason) implements GameEvent {
        public GoldChanged withSeq(int s) { return new GoldChanged(s, before, after, reason); }
    }

    // ----- tavern -------------------------------------------------------------------------------

    /** The tavern shelves were (re)filled; frozen items stayed, the rest are new. */
    record TavernRolled(int seq, boolean free, List<TavernSlot> heroes, List<TavernSlot> relics) implements GameEvent {
        public TavernRolled withSeq(int s) { return new TavernRolled(s, free, heroes, relics); }
    }

    /** A shelf item was frozen: it survives rolls and the next start of turn. */
    record TavernItemFrozen(int seq, ItemKind kind, int slot, String itemId) implements GameEvent {
        public TavernItemFrozen withSeq(int s) { return new TavernItemFrozen(s, kind, slot, itemId); }
    }

    /** A shelf item was unfrozen: the next roll replaces it. */
    record TavernItemUnfrozen(int seq, ItemKind kind, int slot, String itemId) implements GameEvent {
        public TavernItemUnfrozen withSeq(int s) { return new TavernItemUnfrozen(s, kind, slot, itemId); }
    }

    /** Every hero on the shelves gained stats; when permanent, future heroes spawn with them too (Royal Decree). */
    record TavernBuffed(int seq, Stats bonus, boolean permanent) implements GameEvent {
        public TavernBuffed withSeq(int s) { return new TavernBuffed(s, bonus, permanent); }
    }

    /** An ability put a relic on a shelf at a price (Herbalist). */
    record RelicStocked(int seq, String relicId, int slot, int price) implements GameEvent {
        public RelicStocked withSeq(int s) { return new RelicStocked(s, relicId, slot, price); }
    }

    /** A hero was bought from a shelf and placed on the band. */
    record HeroBought(int seq, UnitId unitId, String heroId, int slot, int bandIndex, Stats stats, int price)
            implements GameEvent {
        public HeroBought withSeq(int s) { return new HeroBought(s, unitId, heroId, slot, bandIndex, stats, price); }
    }

    /** A relic was bought and used, on the chosen unit when it needs one. */
    record RelicBought(int seq, String relicId, int slot, UnitId targetUnitId, int price) implements GameEvent {
        public RelicBought withSeq(int s) { return new RelicBought(s, relicId, slot, targetUnitId, price); }
    }

    /** A hero left the band for gold equal to its level. */
    record HeroSold(int seq, UnitId unitId, String heroId, int bandIndex, int level, int gold) implements GameEvent {
        public HeroSold withSeq(int s) { return new HeroSold(s, unitId, heroId, bandIndex, level, gold); }
    }

    /** A unit moved from one index to another; {@code order} is the band's unit ids afterwards. */
    record HeroesReordered(int seq, int from, int to, List<UnitId> order) implements GameEvent {
        public HeroesReordered withSeq(int s) { return new HeroesReordered(s, from, to, order); }
    }

    /** Two copies became one: the survivor keeps the higher stats and gains +1/+1 with the XP. */
    record HeroesMerged(int seq, UnitId unitId, String heroId, MergeSource source, UnitId removedUnitId,
                        Stats before, Stats after) implements GameEvent {
        public HeroesMerged withSeq(int s) { return new HeroesMerged(s, unitId, heroId, source, removedUnitId, before, after); }
    }

    /** A unit's experience went from before to after. */
    record XpGained(int seq, UnitId unitId, String heroId, int before, int after) implements GameEvent {
        public XpGained withSeq(int s) { return new XpGained(s, unitId, heroId, before, after); }
    }

    /** A unit reached level 2 or 3; its Level-up ability fires and the tavern opens a bonus slot. */
    record LevelUp(int seq, UnitId unitId, String heroId, int level) implements GameEvent {
        public LevelUp withSeq(int s) { return new LevelUp(s, unitId, heroId, level); }
    }

    // ----- units --------------------------------------------------------------------------------

    /** A unit's attack and/or health changed; {@code temporary} buffs vanish after the battle. */
    record StatsChanged(int seq, UnitId unitId, String heroId, Position position, int attackBefore, int attackAfter,
                        int healthBefore, int healthAfter, boolean temporary, String source) implements GameEvent {
        public StatsChanged withSeq(int s) {
            return new StatsChanged(s, unitId, heroId, position, attackBefore, attackAfter, healthBefore, healthAfter, temporary, source);
        }
    }

    /** A unit gained a perk, replacing the one it held if any. */
    record PerkGained(int seq, UnitId unitId, String heroId, Position position, Perk perk, Perk replaced) implements GameEvent {
        public PerkGained withSeq(int s) { return new PerkGained(s, unitId, heroId, position, perk, replaced); }
    }

    /** A once-only perk did its job and is gone. */
    record PerkConsumed(int seq, UnitId unitId, String heroId, Position position, Perk perk) implements GameEvent {
        public PerkConsumed withSeq(int s) { return new PerkConsumed(s, unitId, heroId, position, perk); }
    }

    /** A unit's ability fired; the effects' events follow. */
    record AbilityTriggered(int seq, UnitId unitId, String heroId, Position position, Trigger trigger, int level)
            implements GameEvent {
        public AbilityTriggered withSeq(int s) { return new AbilityTriggered(s, unitId, heroId, position, trigger, level); }
    }

    // ----- battle -------------------------------------------------------------------------------

    /** A battle began between the two bands as they stood; {@code playerSide} says which one is the player's. */
    record BattleStarted(int seq, int turn, Side playerSide, List<UnitView> home, List<UnitView> away) implements GameEvent {
        public BattleStarted withSeq(int s) { return new BattleStarted(s, turn, playerSide, home, away); }
    }

    /** The two front units are about to hit each other. */
    record Clash(int seq, int number, UnitId homeUnitId, UnitId awayUnitId) implements GameEvent {
        public Clash withSeq(int s) { return new Clash(s, number, homeUnitId, awayUnitId); }
    }

    /** A unit took damage from an attack, a splash or an ability. */
    record DamageDealt(int seq, UnitId sourceUnitId, Position sourcePosition, UnitId targetUnitId, Position targetPosition,
                       int amount, int healthBefore, int healthAfter, DamageKind kind) implements GameEvent {
        public DamageDealt withSeq(int s) {
            return new DamageDealt(s, sourceUnitId, sourcePosition, targetUnitId, targetPosition, amount, healthBefore, healthAfter, kind);
        }
    }

    /** A unit's damage brought another to zero. */
    record Knockout(int seq, UnitId unitId, String heroId, Position position, UnitId targetUnitId) implements GameEvent {
        public Knockout withSeq(int s) { return new Knockout(s, unitId, heroId, position, targetUnitId); }
    }

    /** A unit reached zero health; its Fall ability fires, then it leaves the band. */
    record UnitFell(int seq, UnitId unitId, String heroId, Position position) implements GameEvent {
        public UnitFell withSeq(int s) { return new UnitFell(s, unitId, heroId, position); }
    }

    /** A fallen unit came back in its slot with new stats (Phoenix Feather). */
    record UnitRevived(int seq, UnitId unitId, String heroId, Position position, Stats stats) implements GameEvent {
        public UnitRevived withSeq(int s) { return new UnitRevived(s, unitId, heroId, position, stats); }
    }

    /** A new unit appeared on a band, bought or created by an ability. */
    record UnitSummoned(int seq, UnitId unitId, String heroId, Position position, Stats stats, int level, Perk perk,
                        UnitId summonerUnitId) implements GameEvent {
        public UnitSummoned withSeq(int s) { return new UnitSummoned(s, unitId, heroId, position, stats, level, perk, summonerUnitId); }
    }

    /** A summon found no room: the band already had five units. */
    record SummonFailed(int seq, String tokenId, Side side, UnitId summonerUnitId) implements GameEvent {
        public SummonFailed withSeq(int s) { return new SummonFailed(s, tokenId, side, summonerUnitId); }
    }

    /** Fallen units were removed and the line closed up; {@code order} is the side's unit ids afterwards. */
    record LineShifted(int seq, Side side, List<UnitId> order) implements GameEvent {
        public LineShifted withSeq(int s) { return new LineShifted(s, side, order); }
    }

    /** The battle is over: the outcome for the home side and for the player, and why it stopped. */
    record BattleEnded(int seq, Outcome homeOutcome, Outcome playerOutcome, BattleEndReason reason, int clashes)
            implements GameEvent {
        public BattleEnded withSeq(int s) { return new BattleEnded(s, homeOutcome, playerOutcome, reason, clashes); }
    }

    // ----- run progress -------------------------------------------------------------------------

    /** Hearts went from before to after: a lost battle, or the turn-3 mercy heart. */
    record HeartsChanged(int seq, int before, int after, HeartsReason reason) implements GameEvent {
        public HeartsChanged withSeq(int s) { return new HeartsChanged(s, before, after, reason); }
    }

    /** Crowns went from before to after: a won battle. */
    record CrownsChanged(int seq, int before, int after) implements GameEvent {
        public CrownsChanged withSeq(int s) { return new CrownsChanged(s, before, after); }
    }

    /** The run is over: won at ten crowns, lost at zero hearts, or abandoned. */
    record RunEnded(int seq, RunStatus status, int turn, int crowns, int hearts) implements GameEvent {
        public RunEnded withSeq(int s) { return new RunEnded(s, status, turn, crowns, hearts); }
    }

    // ----- small vocabularies -------------------------------------------------------------------

    enum GoldReason { TURN_START, ROLL, BUY_HERO, BUY_RELIC, SELL, ABILITY }

    enum MergeSource { TAVERN, BAND }

    enum DamageKind { ATTACK, SPLASH, ABILITY }

    enum BattleEndReason { ELIMINATION, CLASH_CAP }

    enum HeartsReason { BATTLE_LOST, MERCY }
}
