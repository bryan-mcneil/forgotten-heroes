package com.forgottenheroes.engine.event;

import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.model.UnitId;

/** A unit as the frontend needs to draw it at one moment: id, hero, visible stats, level and perk. */
public record UnitView(UnitId unitId, String heroId, int attack, int health, int level, Perk perk) {

    public static UnitView of(Unit unit) {
        return new UnitView(unit.id(), unit.heroId(), unit.attack(), Math.max(0, unit.health()), unit.level(),
            unit.perk().orElse(null));
    }
}
