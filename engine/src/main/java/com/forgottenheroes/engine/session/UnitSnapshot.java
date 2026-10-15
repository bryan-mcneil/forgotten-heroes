package com.forgottenheroes.engine.session;

import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.model.UnitId;
import com.forgottenheroes.engine.model.Xp;

/**
 * One unit as a ghost remembers it: hero, the stats it fought with (bonuses included), its
 * experience and its perk. No run internals, no damage, no counters.
 */
public record UnitSnapshot(String heroId, int attack, int health, int xp, Perk perk) {

    public UnitSnapshot {
        if (heroId == null || heroId.isBlank()) {
            throw new IllegalArgumentException("heroId is required");
        }
        new Stats(attack, health);
        new Xp(xp);
    }

    public static UnitSnapshot of(Unit unit) {
        return new UnitSnapshot(unit.heroId(), unit.attack(), unit.maxHealth(), unit.xp().points(), unit.perk().orElse(null));
    }

    /** A fresh unit that fights exactly as the snapshot describes. */
    public Unit toUnit(UnitId id) {
        Unit unit = new Unit(id, heroId, new Stats(attack, health));
        unit.gainXp(xp);
        if (perk != null) {
            unit.setPerk(perk);
        }
        return unit;
    }
}
