package com.forgottenheroes.engine.mutator;

import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.event.GameEvent.DamageDealt;
import com.forgottenheroes.engine.event.GameEvent.DamageKind;
import com.forgottenheroes.engine.event.GameEvent.LevelUp;
import com.forgottenheroes.engine.event.GameEvent.PerkConsumed;
import com.forgottenheroes.engine.event.GameEvent.PerkGained;
import com.forgottenheroes.engine.event.GameEvent.StatsChanged;
import com.forgottenheroes.engine.event.GameEvent.UnitFell;
import com.forgottenheroes.engine.event.GameEvent.UnitRevived;
import com.forgottenheroes.engine.event.GameEvent.XpGained;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.Position;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.model.UnitId;

/** Changes to one unit: stats, experience, perks, damage. Each one appends the event that describes it. */
public final class UnitMutator {

    private UnitMutator() { }

    /** Where a unit stands on its band, for the event; null once it has left the band (a sold hero). */
    public static Position positionOf(Band band, Side side, Unit unit) {
        int index = band.indexOf(unit);
        return index < 0 ? null : new Position(side, index);
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

    /** GDD §4.8: a hero holds one perk; a new one replaces it. */
    public static void givePerk(Band band, Side side, Unit unit, EventLog log, Perk perk) {
        Perk replaced = unit.perk().orElse(null);
        unit.setPerk(perk);
        log.append(new PerkGained(0, unit.id(), unit.heroId(), positionOf(band, side, unit), perk, replaced));
    }

    /** A once-only perk did its job. */
    public static void consumePerk(Band band, Side side, Unit unit, EventLog log) {
        Perk perk = unit.perk().orElseThrow(() -> new IllegalStateException(unit + " has no perk to consume"));
        unit.removePerk();
        log.append(new PerkConsumed(0, unit.id(), unit.heroId(), positionOf(band, side, unit), perk));
    }

    /** Take damage and report it with the health before and after; whether the unit falls is the caller's business. */
    public static void damage(Band band, Side side, Unit target, EventLog log, int amount, UnitId sourceId, Position sourcePosition,
                              DamageKind kind) {
        int before = target.health();
        target.takeDamage(amount);
        log.append(new DamageDealt(0, sourceId, sourcePosition, target.id(), positionOf(band, side, target), amount, before,
            target.health(), kind));
    }

    /** Ability damage in the tavern: the same event, but the health is lost for good (there is no battle to heal from). */
    public static void damagePermanently(Band band, Side side, Unit target, EventLog log, int amount, UnitId sourceId,
                                         Position sourcePosition, DamageKind kind) {
        int before = target.health();
        target.reduceMaxHealth(amount);
        log.append(new DamageDealt(0, sourceId, sourcePosition, target.id(), positionOf(band, side, target), amount, before,
            target.health(), kind));
    }

    /** Poison Vial: the unit drops to zero health at once, without damage; the settle pass does the rest. */
    public static void kill(Unit unit) {
        if (unit.isAlive()) {
            unit.takeDamage(unit.health());
        }
    }

    /** Witch: lose part of the current health; reported as a stat change, never as damage. */
    public static void loseHealth(Band band, Side side, Unit unit, EventLog log, int amount, String source) {
        if (amount <= 0) {
            return;
        }
        int attack = unit.attack();
        int before = unit.stats().health();
        unit.takeDamage(amount);
        log.append(new StatsChanged(0, unit.id(), unit.heroId(), positionOf(band, side, unit), attack, attack, before,
            unit.stats().health(), true, source));
    }

    /** Phoenix Feather: back in the same slot with new stats and full health. */
    public static void revive(Band band, Side side, Unit unit, EventLog log, Stats stats) {
        unit.resetStats(stats);
        log.append(new UnitRevived(0, unit.id(), unit.heroId(), positionOf(band, side, unit), unit.stats()));
    }

    /** Announce that a unit reached zero health (or was poisoned); removal and summons follow elsewhere. */
    public static void fell(Band band, Side side, Unit unit, EventLog log) {
        log.append(new UnitFell(0, unit.id(), unit.heroId(), positionOf(band, side, unit)));
    }
}
