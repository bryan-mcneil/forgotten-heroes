package com.forgottenheroes.engine.testing;

import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.ContentLoader;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.model.UnitId;
import com.forgottenheroes.engine.model.Xp;

/** Shortcuts every engine test uses: a unit in one line, a band in one line, the real content loaded once. */
public final class TestFixtures {

    private static final ContentIndex CONTENT = ContentLoader.load();
    private static int nextId = 1;

    private TestFixtures() { }

    /** The real heroes.json / tokens.json / relics.json, loaded once per test run. */
    public static ContentIndex content() {
        return CONTENT;
    }

    /** A level-1 unit of any hero id with the given stats as its base; ids are t1, t2, t3 ... */
    public static Unit unit(String heroId, int attack, int health) {
        return new Unit(new UnitId("t" + nextId++), heroId, new Stats(attack, health));
    }

    public static Unit unit(String heroId, int attack, int health, int level) {
        Unit unit = unit(heroId, attack, health);
        unit.gainXp(level == 1 ? 0 : level == 2 ? Xp.LEVEL2_AT : Xp.LEVEL3_AT);
        return unit;
    }

    public static Unit unit(String heroId, int attack, int health, Perk perk) {
        Unit unit = unit(heroId, attack, health);
        unit.setPerk(perk);
        return unit;
    }

    public static Band band(Unit... units) {
        return Band.of(units);
    }

    /** A run that has not started its first turn yet. */
    public static RunState newRun(long seed) {
        return RunState.newRun("run-" + seed, seed);
    }
}
