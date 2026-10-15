package com.forgottenheroes.engine.resolver;

import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.EffectDefinition;
import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.event.GameEvent.DamageKind;
import com.forgottenheroes.engine.event.GameEvent.GoldReason;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.mutator.GoldMutator;
import com.forgottenheroes.engine.mutator.TavernMutator;
import com.forgottenheroes.engine.mutator.UnitMutator;
import java.util.List;

/**
 * Runs one effect of the DSL against the board: resolves the amounts for the level, finds the
 * targets, calls the mutators. The tavern version: relics and, from Step 16, tavern abilities.
 * Battle-only effects (summons, health percentages) arrive with the battle resolver.
 */
public final class EffectApplier {

    private final ContentIndex content;

    public EffectApplier(ContentIndex content) {
        this.content = content;
    }

    public void apply(EffectDefinition effect, EffectContext context) {
        switch (effect) {
            case EffectDefinition.ModifyStats e -> modifyStats(e, context);
            case EffectDefinition.GivePerk e -> {
                for (Unit target : Targeting.select(e.target(), context)) {
                    UnitMutator.givePerk(context.band(), context.side(), target, context.log(), e.perk());
                }
            }
            case EffectDefinition.GiveXp e -> giveXp(e, context);
            case EffectDefinition.BuffTavern e -> {
                Stats bonus = new Stats(resolve(e.attack(), context), resolve(e.health(), context));
                TavernMutator.buff(context.state(), context.log(), bonus, e.permanent());
            }
            case EffectDefinition.Kill e -> {
                for (Unit target : Targeting.select(e.target(), context)) {
                    fall(target, context);
                }
            }
            case EffectDefinition.DealDamage e -> dealDamage(e, context);
            case EffectDefinition.GainGold e -> GoldMutator.gain(context.state(), context.log(),
                resolve(e.amount(), context), GoldReason.ABILITY);
            case EffectDefinition.SetHealthPercent e -> throw new UnsupportedOperationException("SET_HEALTH_PERCENT arrives in Step 18");
            case EffectDefinition.Summon e -> throw new UnsupportedOperationException("SUMMON arrives in Step 16");
            case EffectDefinition.StockRelic e -> throw new UnsupportedOperationException("STOCK_RELIC arrives in Step 16");
        }
    }

    private void modifyStats(EffectDefinition.ModifyStats e, EffectContext context) {
        Stats delta = new Stats(resolve(e.attack(), context), resolve(e.health(), context));
        boolean temporary = context.inTavern() ? e.untilEndOfBattle() : true;
        for (Unit target : Targeting.select(e.target(), context)) {
            UnitMutator.modifyStats(context.band(), context.side(), target, context.log(), delta, temporary, context.source());
        }
    }

    private void giveXp(EffectDefinition.GiveXp e, EffectContext context) {
        int points = resolve(e.amount(), context);
        for (Unit target : Targeting.select(e.target(), context)) {
            boolean leveled = UnitMutator.gainXp(context.band(), target, context.log(), points, true, context.source());
            if (leveled && context.inTavern()) {
                TavernMutator.openBonusSlot(context.state(), context.log(), content, context.rng().fork("bonus"));
                context.triggers().add(PendingTrigger.of(Trigger.LEVEL_UP, target.id()));
            }
        }
    }

    private void dealDamage(EffectDefinition.DealDamage e, EffectContext context) {
        int amount = resolve(e.amount(), context);
        for (Unit target : Targeting.select(e.target(), context)) {
            int dealt = amount;
            if (e.condition() != null && e.condition().ifTargetRank() != null
                    && content.hasHero(target.heroId())
                    && content.hero(target.heroId()).rank().number() == e.condition().ifTargetRank()) {
                dealt *= e.condition().multiplier();
            }
            Unit self = context.self();
            UnitMutator.damage(context.band(), context.side(), target, context.log(), dealt,
                self == null ? null : self.id(),
                self == null ? null : UnitMutator.positionOf(context.band(), context.side(), self),
                DamageKind.ABILITY);
            if (!target.isAlive() && context.inTavern()) {
                fall(target, context);
            }
        }
    }

    /** A unit falls in the tavern: announced, its Fall moment recorded, and it leaves the band. */
    private static void fall(Unit target, EffectContext context) {
        UnitMutator.fell(context.band(), context.side(), target, context.log());
        context.triggers().add(PendingTrigger.of(Trigger.FALL, target.id()));
        context.band().remove(target);
    }

    private static int resolve(com.forgottenheroes.engine.content.Amount amount, EffectContext context) {
        return amount == null ? 0 : amount.resolve(context.level(), context.attack());
    }

    /** The selectors this applier can serve today; battles add the rest. */
    public static List<String> supportedEffects() {
        return List.of("MODIFY_STATS", "GIVE_PERK", "GIVE_XP", "BUFF_TAVERN", "KILL", "DEAL_DAMAGE", "GAIN_GOLD");
    }
}
