package com.forgottenheroes.engine.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Everything about one run that must stay consistent: the aggregate root. Mutators are the only
 * code meant to call the setters; {@link #assertInvariants()} lets tests and the resolver prove that
 * no rule was broken.
 */
public final class RunState {

    private final String runId;
    private final long runSeed;
    private int turn;
    private int gold;
    private int hearts;
    private int crowns;
    private final Band band;
    private final Tavern tavern;
    private boolean freeRollAvailable;
    private int rollsThisTurn;
    private RunStatus status;
    private BattleSummary lastBattle;
    private long version;
    private int lastEventSeq;
    private int nextUnitNumber;

    private RunState(String runId, long runSeed, Band band, Tavern tavern) {
        this.runId = runId;
        this.runSeed = runSeed;
        this.band = band;
        this.tavern = tavern;
    }

    /** A run before its first turn: 5 hearts, 0 crowns, no gold, empty band and tavern. */
    public static RunState newRun(String runId, long runSeed) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("run id cannot be blank");
        }
        RunState state = new RunState(runId, runSeed, new Band(), new Tavern());
        state.turn = 0;
        state.gold = 0;
        state.hearts = Rules.START_HEARTS;
        state.crowns = 0;
        state.status = RunStatus.IN_PROGRESS;
        state.version = 0;
        state.nextUnitNumber = 1;
        return state;
    }

    public String runId() {
        return runId;
    }

    public long runSeed() {
        return runSeed;
    }

    public int turn() {
        return turn;
    }

    public int gold() {
        return gold;
    }

    public int hearts() {
        return hearts;
    }

    public int crowns() {
        return crowns;
    }

    public Band band() {
        return band;
    }

    public Tavern tavern() {
        return tavern;
    }

    public boolean freeRollAvailable() {
        return freeRollAvailable;
    }

    public int rollsThisTurn() {
        return rollsThisTurn;
    }

    public RunStatus status() {
        return status;
    }

    public BattleSummary lastBattle() {
        return lastBattle;
    }

    public long version() {
        return version;
    }

    public int lastEventSeq() {
        return lastEventSeq;
    }

    public void setTurn(int newTurn) {
        if (newTurn < 0) {
            throw new IllegalArgumentException("turn cannot be negative: " + newTurn);
        }
        turn = newTurn;
    }

    public void setGold(int newGold) {
        if (newGold < 0 || newGold > Rules.MAX_GOLD) {
            throw new IllegalArgumentException("gold must be 0.." + Rules.MAX_GOLD + ", got " + newGold);
        }
        gold = newGold;
    }

    public void setHearts(int newHearts) {
        if (newHearts < 0 || newHearts > Rules.START_HEARTS) {
            throw new IllegalArgumentException("hearts must be 0.." + Rules.START_HEARTS + ", got " + newHearts);
        }
        hearts = newHearts;
    }

    public void setCrowns(int newCrowns) {
        if (newCrowns < 0 || newCrowns > Rules.CROWNS_TO_WIN) {
            throw new IllegalArgumentException("crowns must be 0.." + Rules.CROWNS_TO_WIN + ", got " + newCrowns);
        }
        crowns = newCrowns;
    }

    public void setFreeRollAvailable(boolean available) {
        freeRollAvailable = available;
    }

    public void setRollsThisTurn(int rolls) {
        if (rolls < 0) {
            throw new IllegalArgumentException("rolls cannot be negative: " + rolls);
        }
        rollsThisTurn = rolls;
    }

    public void setStatus(RunStatus newStatus) {
        status = Objects.requireNonNull(newStatus, "status");
    }

    public void setLastBattle(BattleSummary summary) {
        lastBattle = summary;
    }

    /** One more accepted action; the backend uses this for optimistic locking (Step 29). */
    public void bumpVersion() {
        version++;
    }

    public void setLastEventSeq(int seq) {
        if (seq < lastEventSeq) {
            throw new IllegalArgumentException("event sequence cannot go backwards: " + seq + " < " + lastEventSeq);
        }
        lastEventSeq = seq;
    }

    /** The band as a ghost for other players: hero ids, stats, levels, perks and nothing else. */
    public com.forgottenheroes.engine.session.BandSnapshot toSnapshot() {
        return com.forgottenheroes.engine.session.BandSnapshot.of(band);
    }

    /** The next unit id for this run: u1, u2, u3 ... a reloaded run keeps counting. */
    public UnitId nextUnitId() {
        return new UnitId("u" + nextUnitNumber++);
    }

    /**
     * Throws when a rule-level fact is broken: gold or hearts out of range, more than five units, a
     * fallen unit still on the band, a frozen empty slot, duplicate unit ids, or slot counts that do
     * not match the turn (once the first turn has started).
     */
    public void assertInvariants() {
        check(gold >= 0 && gold <= Rules.MAX_GOLD, "gold out of range: " + gold);
        check(hearts >= 0 && hearts <= Rules.START_HEARTS, "hearts out of range: " + hearts);
        check(crowns >= 0 && crowns <= Rules.CROWNS_TO_WIN, "crowns out of range: " + crowns);
        check(band.size() <= Rules.BAND_SIZE, "band has " + band.size() + " units");
        Set<UnitId> ids = new HashSet<>();
        for (Unit unit : band.units()) {
            check(unit.isAlive(), "fallen unit on the band: " + unit);
            check(unit.damageTaken() == 0, "battle damage outside a battle: " + unit);
            check(ids.add(unit.id()), "duplicate unit id " + unit.id().value());
        }
        for (TavernSlot slot : tavern.heroSlots()) {
            check(!(slot.isEmpty() && slot.frozen()), "frozen empty hero slot");
        }
        if (turn >= 1) {
            check(tavern.heroSlots().size() == Tavern.heroSlotsFor(turn),
                "expected " + Tavern.heroSlotsFor(turn) + " hero slots on turn " + turn + ", found " + tavern.heroSlots().size());
            check(tavern.relicSlots().size() >= Tavern.relicSlotsFor(turn),
                "expected at least " + Tavern.relicSlotsFor(turn) + " relic slots on turn " + turn);
        }
        check(status == RunStatus.IN_PROGRESS || status.isOver(), "status missing");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("invariant broken: " + message);
        }
    }

    /** A deep copy: changing the copy never touches the original. */
    public RunState copy() {
        RunState copy = new RunState(runId, runSeed, band.copy(), tavern.copy());
        copy.turn = turn;
        copy.gold = gold;
        copy.hearts = hearts;
        copy.crowns = crowns;
        copy.freeRollAvailable = freeRollAvailable;
        copy.rollsThisTurn = rollsThisTurn;
        copy.status = status;
        copy.lastBattle = lastBattle;
        copy.version = version;
        copy.lastEventSeq = lastEventSeq;
        copy.nextUnitNumber = nextUnitNumber;
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RunState that
            && runId.equals(that.runId)
            && runSeed == that.runSeed
            && turn == that.turn
            && gold == that.gold
            && hearts == that.hearts
            && crowns == that.crowns
            && band.equals(that.band)
            && tavern.equals(that.tavern)
            && freeRollAvailable == that.freeRollAvailable
            && rollsThisTurn == that.rollsThisTurn
            && status == that.status
            && Objects.equals(lastBattle, that.lastBattle)
            && version == that.version
            && lastEventSeq == that.lastEventSeq
            && nextUnitNumber == that.nextUnitNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(runId, runSeed, turn, gold, hearts, crowns, band, tavern, freeRollAvailable, rollsThisTurn,
            status, lastBattle, version, lastEventSeq, nextUnitNumber);
    }

    @Override
    public String toString() {
        return "run " + runId + " turn " + turn + " gold " + gold + " hearts " + hearts + " crowns " + crowns
            + " " + status + " band " + band + " tavern " + tavern;
    }
}
