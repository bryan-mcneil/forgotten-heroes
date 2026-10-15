package com.forgottenheroes.engine.model;

import com.forgottenheroes.engine.content.HeroDefinition;
import com.forgottenheroes.engine.content.TokenDefinition;
import java.util.Objects;
import java.util.Optional;

/**
 * One hero or token on a band. Mutable: the mutators change it in place and emit events. Stats are
 * kept in three parts, base (from the content file), permanent bonus (tavern buffs, XP) and
 * temporary bonus (battle buffs, Battle Brew), plus the damage taken in the current battle; what the
 * player sees is derived: {@code attack()} and {@code health()}.
 */
public final class Unit {

    private final UnitId id;
    private final String heroId;
    private Stats base;
    private Stats permanentBonus = Stats.ZERO;
    private Stats temporaryBonus = Stats.ZERO;
    private Xp xp = Xp.ZERO;
    private Perk perk;
    private Counters counters = new Counters();
    private int damageTaken;

    public Unit(UnitId id, String heroId, Stats base) {
        this.id = Objects.requireNonNull(id, "id");
        this.heroId = Objects.requireNonNull(heroId, "heroId");
        this.base = Objects.requireNonNull(base, "base");
    }

    /** A freshly bought hero: level-1 stats from the roster, nothing else. */
    public static Unit fromDefinition(HeroDefinition definition, UnitId id) {
        return new Unit(id, definition.id(), definition.stats());
    }

    /** A summoned token with the stats the summoning ability resolved. */
    public static Unit fromToken(TokenDefinition definition, UnitId id, Stats stats) {
        return new Unit(id, definition.id(), stats);
    }

    public UnitId id() {
        return id;
    }

    public String heroId() {
        return heroId;
    }

    public Stats base() {
        return base;
    }

    public Stats permanentBonus() {
        return permanentBonus;
    }

    public Stats temporaryBonus() {
        return temporaryBonus;
    }

    public Xp xp() {
        return xp;
    }

    public int level() {
        return xp.level();
    }

    public Optional<Perk> perk() {
        return Optional.ofNullable(perk);
    }

    public boolean hasPerk(Perk candidate) {
        return perk == candidate;
    }

    public Counters counters() {
        return counters;
    }

    public int damageTaken() {
        return damageTaken;
    }

    /** Base plus both bonuses, capped at the stat cap. */
    public int attack() {
        return Math.min(base.attack() + permanentBonus.attack() + temporaryBonus.attack(), Rules.MAX_STAT);
    }

    /** Health before any damage this battle: base plus both bonuses, capped. */
    public int maxHealth() {
        return Math.min(base.health() + permanentBonus.health() + temporaryBonus.health(), Rules.MAX_STAT);
    }

    /** Current health; zero or below means the unit has fallen. */
    public int health() {
        return maxHealth() - damageTaken;
    }

    /** What a card shows: attack and current health (never below 0). */
    public Stats stats() {
        return new Stats(attack(), Math.max(0, health()));
    }

    /** The stats a merge compares and a ghost snapshot keeps: everything but the damage. */
    public Stats statsBeforeDamage() {
        return new Stats(attack(), maxHealth());
    }

    public boolean isAlive() {
        return health() > 0;
    }

    public void addPermanentBonus(Stats bonus) {
        permanentBonus = permanentBonus.plus(bonus);
    }

    public void addTemporaryBonus(Stats bonus) {
        temporaryBonus = temporaryBonus.plus(bonus);
    }

    /** After a battle, battle buffs and Battle Brew are gone. */
    public void clearTemporary() {
        temporaryBonus = Stats.ZERO;
    }

    public void takeDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("damage cannot be negative: " + amount);
        }
        damageTaken += amount;
    }

    /** Back to full health: a battle copy starts this way, and Phoenix Feather returns a unit this way. */
    public void heal() {
        damageTaken = 0;
    }

    /** Replace every stat with the given base and no bonuses (Phoenix Feather returns the unit as 1/1). */
    public void resetStats(Stats newBase) {
        base = Objects.requireNonNull(newBase, "newBase");
        permanentBonus = Stats.ZERO;
        temporaryBonus = Stats.ZERO;
        damageTaken = 0;
    }

    /** The merge rule: keep the higher of each stat, then the XP adds +1/+1 per point gained. */
    public void setPermanentStats(Stats total) {
        if (total.attack() < base.attack() || total.health() < base.health()) {
            throw new IllegalArgumentException("permanent stats " + total + " cannot be below base " + base);
        }
        permanentBonus = new Stats(total.attack() - base.attack(), total.health() - base.health());
    }

    public void setPerk(Perk newPerk) {
        perk = Objects.requireNonNull(newPerk, "perk");
    }

    public void removePerk() {
        perk = null;
    }

    /** Adds XP and returns the points actually gained (none past the maximum). */
    public int gainXp(int points) {
        Xp before = xp;
        xp = xp.plus(points);
        return xp.points() - before.points();
    }

    /** Same unit, fresh for a fight: no damage, battle counter at zero, temporary buffs kept (Battle Brew). */
    public Unit copyForBattle() {
        Unit copy = copy();
        copy.damageTaken = 0;
        copy.counters.resetBattle();
        return copy;
    }

    /** An exact copy, used to prove a rejected action changed nothing. */
    public Unit copy() {
        Unit copy = new Unit(id, heroId, base);
        copy.permanentBonus = permanentBonus;
        copy.temporaryBonus = temporaryBonus;
        copy.xp = xp;
        copy.perk = perk;
        copy.counters = counters.copy();
        copy.damageTaken = damageTaken;
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Unit that
            && id.equals(that.id)
            && heroId.equals(that.heroId)
            && base.equals(that.base)
            && permanentBonus.equals(that.permanentBonus)
            && temporaryBonus.equals(that.temporaryBonus)
            && xp.equals(that.xp)
            && perk == that.perk
            && counters.equals(that.counters)
            && damageTaken == that.damageTaken;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, heroId, base, permanentBonus, temporaryBonus, xp, perk, counters, damageTaken);
    }

    @Override
    public String toString() {
        return heroId + "#" + id.value() + " " + stats() + " L" + level() + (perk == null ? "" : " " + perk);
    }
}
