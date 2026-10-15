package com.forgottenheroes.engine.resolver.battle;

import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.Unit;

/**
 * GDD §4.8: {@code damage = attack + bonus − reduction}, perks included. Iron Blade adds 3 to an
 * attack; Chainmail takes 2 off any hit, never below 1. The once-only perks and the splash arrive in
 * Step 18; the record already says which perks a hit used up.
 */
public final class Damage {

    /** What one hit does: the amount, and whether it spent the attacker's or the defender's once-only perk. */
    public record Hit(int amount, boolean spendsAttackerPerk, boolean spendsDefenderPerk) { }

    private Damage() { }

    /** The damage an attack from {@code attacker} does to {@code defender}, from their stats before the hit. */
    public static Hit attack(Unit attacker, Unit defender) {
        int amount = attacker.attack();
        if (attacker.hasPerk(Perk.IRON_BLADE)) {
            amount += 3;
        }
        return defend(defender, amount, false);
    }

    /** Damage from an ability or a splash: the defender's perks apply, the attacker's do not. */
    public static Hit incoming(Unit defender, int amount) {
        return defend(defender, amount, false);
    }

    private static Hit defend(Unit defender, int amount, boolean spendsAttackerPerk) {
        if (amount > 0 && defender.hasPerk(Perk.CHAINMAIL)) {
            amount = Math.max(1, amount - 2);
        }
        return new Hit(amount, spendsAttackerPerk, false);
    }
}
