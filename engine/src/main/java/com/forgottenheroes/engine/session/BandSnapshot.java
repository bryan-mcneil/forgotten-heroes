package com.forgottenheroes.engine.session;

import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.model.UnitId;
import java.util.List;

/**
 * What a ghost is (GDD §3, architecture §4.4): the band another player ended a turn with, front
 * first. The backend stores it; {@code GameSession.endTurn} fights it. An empty snapshot is a
 * legal opponent that loses at once.
 */
public record BandSnapshot(List<UnitSnapshot> units) {

    public static final BandSnapshot EMPTY = new BandSnapshot(List.of());

    public BandSnapshot {
        units = List.copyOf(units);
    }

    public static BandSnapshot of(Band band) {
        return new BandSnapshot(band.units().stream().map(UnitSnapshot::of).toList());
    }

    /** The snapshot as a band ready to fight; ids are prefix + 1, 2, 3 … so they never clash with the player's. */
    public Band toBand(String idPrefix) {
        Band band = new Band();
        int n = 1;
        for (UnitSnapshot unit : units) {
            band.add(unit.toUnit(new UnitId(idPrefix + n++)));
        }
        return band;
    }

    public boolean isEmpty() {
        return units.isEmpty();
    }

    public int size() {
        return units.size();
    }

    /** The crowns-like strength hint matchmaking can sort on: total attack plus health. */
    public int power() {
        return units.stream().mapToInt(unit -> unit.attack() + unit.health()).sum();
    }

    public static BandSnapshot ofUnits(List<Unit> units) {
        return new BandSnapshot(units.stream().map(UnitSnapshot::of).toList());
    }
}
