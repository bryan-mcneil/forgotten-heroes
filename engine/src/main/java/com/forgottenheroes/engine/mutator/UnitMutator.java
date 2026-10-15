package com.forgottenheroes.engine.mutator;

import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.event.GameEvent.LevelUp;
import com.forgottenheroes.engine.event.GameEvent.StatsChanged;
import com.forgottenheroes.engine.event.GameEvent.XpGained;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.Position;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.Unit;

/** Changes to one unit: stats, experience, perks, damage. Each one appends the event that describes it. */
public final class UnitMutator {

    private UnitMutator() { }

    /** Where a unit stands on its band, for the event. */
    public static Position positionOf(Band band, Side side, Unit unit) {
        return new Position(side, band.indexOf(unit));
    }

    /** Add stats, permanently or until the end of the battle, and say so. */
    public static void modifyStats(Band band, Side side, Unit unit, EventLog log, Stats delta, boolean temporary, String source) {
        int attackBefore = unit.attack();
        int healthBefore = unit.stats().health();
        if (temporary) {
            unit.addTemporaryBonus(delta);
        } else {
            unit.addPermanentBonus(delta);
        }
        log.append(new StatsChanged(0, unit.id(), unit.heroId(), positionOf(band, side, unit), attackBefore, unit.attack(),
            healthBefore, unit.stats().health(), temporary, source));
    }

    /**
     * GDD §4.4: every experience point is +1/+1 for good; level 2 at 2 XP, level 3 at 5. Returns true when the
     * unit crossed a level. {@code announceStats} adds a {@code StatsChanged}; a merge reports its stats itself.
     */
    public static boolean gainXp(Band band, Unit unit, EventLog log, int points, boolean announceStats, String source) {
        int levelBefore = unit.level();
        int xpBefore = unit.xp().points();
        int gained = unit.gainXp(points);
        if (gained == 0) {
            return false;
        }
        if (announceStats) {
            modifyStats(band, Side.HOME, unit, log, new Stats(gained, gained), false, source);
        } else {
            unit.addPermanentBonus(new Stats(gained, gained));
        }
        log.append(new XpGained(0, unit.id(), unit.heroId(), xpBefore, unit.xp().points()));
        boolean leveled = unit.level() > levelBefore;
        if (leveled) {
            log.append(new LevelUp(0, unit.id(), unit.heroId(), unit.level()));
        }
        return leveled;
    }
}
