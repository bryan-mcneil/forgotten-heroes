package com.forgottenheroes.engine.mutator;

import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.event.GameEvent.HeroBought;
import com.forgottenheroes.engine.event.GameEvent.HeroSold;
import com.forgottenheroes.engine.event.GameEvent.HeroesMerged;
import com.forgottenheroes.engine.event.GameEvent.HeroesReordered;
import com.forgottenheroes.engine.event.GameEvent.MergeSource;
import com.forgottenheroes.engine.event.GameEvent.UnitSummoned;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.Position;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.model.UnitId;
import com.forgottenheroes.engine.model.Xp;

/** Changes to the player's band: who is on it and in which order. */
public final class BandMutator {

    private BandMutator() { }

    /** Put a freshly bought hero on the band. */
    public static void buy(RunState state, EventLog log, Unit unit, int bandIndex, int slot, int price) {
        state.band().insert(bandIndex, unit);
        log.append(new HeroBought(0, unit.id(), unit.heroId(), slot, bandIndex, unit.stats(), price));
    }

    /** Take a hero off the band; the gold is paid by the caller so the events read sell → gold. */
    public static Unit sell(RunState state, EventLog log, int bandIndex) {
        Unit unit = state.band().remove(bandIndex);
        log.append(new HeroSold(0, unit.id(), unit.heroId(), bandIndex, unit.level(), unit.level()));
        return unit;
    }

    /** A token appears on a band at an index; the caller has checked there is room. */
    public static void summon(Band band, Side side, int index, Unit unit, EventLog log, UnitId summonerId) {
        band.insert(index, unit);
        log.append(new UnitSummoned(0, unit.id(), unit.heroId(), new Position(side, index), unit.stats(), unit.level(),
            unit.perk().orElse(null), summonerId));
    }

    public static void move(Band band, EventLog log, int from, int to) {
        band.move(from, to);
        log.append(new HeroesReordered(0, from, to, band.units().stream().map(Unit::id).toList()));
    }

    /**
     * GDD §4.4, the merge rule: the survivor keeps the higher attack and the higher health of the two,
     * gains the copy's experience plus one, and +1/+1 for every point gained. A copy's perk is kept only
     * when the survivor has none; the higher temporary bonus of each stat survives too. Returns true on a
     * level-up.
     */
    public static boolean stack(Band band, EventLog log, Unit survivor, Unit copy, MergeSource source, UnitId removedId) {
        Stats before = survivor.stats();
        Stats mine = survivor.permanentStats();
        Stats theirs = copy.permanentStats();
        survivor.setPermanentStats(new Stats(Math.max(mine.attack(), theirs.attack()), Math.max(mine.health(), theirs.health())));
        Stats temporary = new Stats(
            Math.max(survivor.temporaryBonus().attack(), copy.temporaryBonus().attack()),
            Math.max(survivor.temporaryBonus().health(), copy.temporaryBonus().health()));
        survivor.clearTemporary();
        survivor.addTemporaryBonus(temporary);
        if (survivor.perk().isEmpty()) {
            copy.perk().ifPresent(survivor::setPerk);
        }
        boolean leveled = UnitMutator.gainXp(band, survivor, log, copy.xp().points() + 1, false, "merge");
        log.append(new HeroesMerged(0, survivor.id(), survivor.heroId(), source, removedId, before, survivor.stats()));
        return leveled;
    }

    /** True when one more copy can still be merged into the unit. */
    public static boolean canStack(Unit unit) {
        return unit.xp().points() < Xp.MAX;
    }
}
