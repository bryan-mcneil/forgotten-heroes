package com.forgottenheroes.engine.resolver.battle;

import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.mutator.UnitMutator;

/**
 * GDD §4.8: {@code damage = attack + bonus − reduction}, perks included. The attacker's Iron Blade
 * adds 3, Enchanted Blade adds 20 once; the defender's Aegis ignores a hit once, Ward takes 20 off
 * once, Chainmail takes 2 off every hit (never below 1); Venom makes any damaging hit lethal. A hit
 * knows which once-only perks it spent, so the caller can report them before the damage lands.
 */
public final class Damage {

    /** Flame Oil's splash hits the unit behind the target for this much. */
    public static final int SPLASH = 5;

    /** What one hit does: the amount, and whether it spent the attacker's or the defender's once-only perk. */
    public record Hit(int amount, boolean spendsAttackerPerk, boolean spendsDefenderPerk) { }

    private Damage() { }

    /** The damage an attack from {@code attacker} does to {@code defender}, from their stats before the hit. */
    public static Hit attack(Unit attacker, Unit defender) {
        int amount = attacker.attack();
        boolean spendsAttackerPerk = false;
        if (attacker.hasPerk(Perk.IRON_BLADE)) {
            amount += 3;
        }
        if (attacker.hasPerk(Perk.ENCHANTED_BLADE)) {
            amount += 20;
            spendsAttackerPerk = true;
        }
        Hit hit = defend(defender, amount, spendsAttackerPerk);
        if (hit.amount() > 0 && attacker.hasPerk(Perk.VENOM)) {
            hit = new Hit(Math.max(hit.amount(), defender.health()), hit.spendsAttackerPerk(), hit.spendsDefenderPerk());
        }
        return hit;
    }

    /** Damage from an ability or a splash: the defender's perks apply, the attacker's do not. */
    public static Hit incoming(Unit defender, int amount) {
        return defend(defender, amount, false);
    }

    /** Report the once-only perks a hit spends, so the log reads "the ward shatters" before the damage. */
    public static void spend(Hit hit, Unit attacker, Band attackerBand, Side attackerSide, Unit defender, Band defenderBand,
                             Side defenderSide, EventLog log) {
        if (hit.spendsAttackerPerk() && attacker != null) {
            UnitMutator.consumePerk(attackerBand, attackerSide, attacker, log);
        }
        if (hit.spendsDefenderPerk()) {
            UnitMutator.consumePerk(defenderBand, defenderSide, defender, log);
        }
    }

    private static Hit defend(Unit defender, int amount, boolean spendsAttackerPerk) {
        boolean spendsDefenderPerk = false;
        if (amount > 0 && defender.hasPerk(Perk.AEGIS)) {
            amount = 0;
            spendsDefenderPerk = true;
        } else if (amount > 0 && defender.hasPerk(Perk.WARD)) {
            amount = Math.max(0, amount - 20);
            spendsDefenderPerk = true;
        }
        if (amount > 0 && defender.hasPerk(Perk.CHAINMAIL)) {
            amount = Math.max(1, amount - 2);
        }
        return new Hit(amount, spendsAttackerPerk, spendsDefenderPerk);
    }
}
