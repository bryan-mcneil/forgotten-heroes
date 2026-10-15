package com.forgottenheroes.engine.resolver;

import com.forgottenheroes.engine.content.Amount;
import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.EffectDefinition;
import com.forgottenheroes.engine.content.TokenDefinition;
import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.event.GameEvent.DamageKind;
import com.forgottenheroes.engine.event.GameEvent.GoldReason;
import com.forgottenheroes.engine.event.GameEvent.SummonFailed;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.mutator.BandMutator;
import com.forgottenheroes.engine.mutator.GoldMutator;
import com.forgottenheroes.engine.mutator.TavernMutator;
import com.forgottenheroes.engine.mutator.UnitMutator;
import com.forgottenheroes.engine.resolver.battle.Damage;

/**
 * Runs one effect of the DSL against the board: resolves the amounts for the level, finds the
 * targets, calls the mutators. It never removes a fallen unit itself: anything at zero health is
 * handled by the dispatcher's settle pass, so Fall abilities fire while the unit still has a place.
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
                    Side side = sideOf(target, context);
                    UnitMutator.givePerk(context.board().band(side), side, target, context.log(), e.perk());
                }
            }
            case EffectDefinition.GiveXp e -> giveXp(e, context);
            case EffectDefinition.BuffTavern e -> {
                Stats bonus = new Stats(resolve(e.attack(), context), resolve(e.health(), context));
                TavernMutator.buff(context.state(), context.log(), bonus, e.permanent());
            }
            case EffectDefinition.Kill e -> {
                for (Unit target : Targeting.select(e.target(), context)) {
                    UnitMutator.kill(target);
                }
            }
            case EffectDefinition.DealDamage e -> dealDamage(e, context);
            case EffectDefinition.GainGold e -> GoldMutator.gain(context.state(), context.log(),
                resolve(e.amount(), context), GoldReason.ABILITY);
            case EffectDefinition.StockRelic e -> TavernMutator.stockRelic(context.state(), context.log(), e.relicId(),
                resolve(e.price(), context));
            case EffectDefinition.Summon e -> summon(e, context);
            case EffectDefinition.SetHealthPercent e -> setHealthPercent(e, context);
        }
    }

    private void modifyStats(EffectDefinition.ModifyStats e, EffectContext context) {
        Stats delta = new Stats(resolve(e.attack(), context), resolve(e.health(), context));
        boolean temporary = context.inTavern() ? e.untilEndOfBattle() : true;
        for (Unit target : Targeting.select(e.target(), context)) {
            Side side = sideOf(target, context);
            UnitMutator.modifyStats(context.board().band(side), side, target, context.log(), delta, temporary, context.source());
        }
    }

    /** The side a target stands on: its own band may be the enemy's (Stone Golem buffs everybody). */
    private static Side sideOf(Unit target, EffectContext context) {
        return context.band().contains(target.id()) ? context.side() : context.side().opposite();
    }

    private void giveXp(EffectDefinition.GiveXp e, EffectContext context) {
        int points = resolve(e.amount(), context);
        for (Unit target : Targeting.select(e.target(), context)) {
            boolean leveled = UnitMutator.gainXp(context.band(), target, context.log(), points, true, context.source());
            if (leveled && context.inTavern()) {
                TavernMutator.openBonusSlot(context.state(), context.log(), content, context.rng().fork("bonus"));
                context.triggers().add(PendingTrigger.of(Trigger.LEVEL_UP, target));
            }
        }
    }

    private void dealDamage(EffectDefinition.DealDamage e, EffectContext context) {
        int amount = resolve(e.amount(), context);
        for (Unit target : Targeting.select(e.target(), context)) {
            int dealt = amount;
            if (e.condition() != null && content.hasHero(target.heroId())
                    && content.hero(target.heroId()).rank().number() == e.condition().ifTargetRank()) {
                dealt *= e.condition().multiplier();
            }
            Side targetSide = sideOf(target, context);
            Unit self = context.self();
            Band targetBand = context.board().band(targetSide);
            com.forgottenheroes.engine.model.UnitId sourceId = self == null ? null : self.id();
            com.forgottenheroes.engine.model.Position sourcePosition =
                self == null ? null : UnitMutator.positionOf(context.band(), context.side(), self);
            Damage.Hit hit = Damage.incoming(target, dealt);
            Damage.spend(hit, null, context.band(), context.side(), target, targetBand, targetSide, context.log());
            if (context.inTavern()) {
                UnitMutator.damagePermanently(targetBand, targetSide, target, context.log(), hit.amount(), sourceId, sourcePosition,
                    DamageKind.ABILITY);
            } else {
                UnitMutator.damage(targetBand, targetSide, target, context.log(), hit.amount(), sourceId, sourcePosition, DamageKind.ABILITY);
                if (hit.amount() > 0) {
                    context.triggers().add(PendingTrigger.of(Trigger.HURT, target, targetSide));
                    if (!target.isAlive() && self != null) {
                        context.log().append(new com.forgottenheroes.engine.event.GameEvent.Knockout(0, self.id(), self.heroId(),
                            sourcePosition, target.id()));
                        context.triggers().add(PendingTrigger.of(Trigger.KNOCKOUT, self, context.side()));
                    }
                }
            }
        }
    }

    /** Witch: the target loses a share of its current health; not damage, so nothing is "hurt". */
    private static void setHealthPercent(EffectDefinition.SetHealthPercent e, EffectContext context) {
        int percent = resolve(e.percent(), context);
        for (Unit target : Targeting.select(e.target(), context)) {
            Side targetSide = sideOf(target, context);
            int loss = target.health() * percent / 100;
            UnitMutator.loseHealth(context.board().band(targetSide), targetSide, target, context.log(), loss, context.source());
        }
    }

    /**
     * Put tokens on a band. Stats default to the token's own; ALLY/HERE lands at the context's summon
     * index (deferred during a fall), ENEMY/FRONT at the enemy front. A full band means {@code SummonFailed}.
     */
    private void summon(EffectDefinition.Summon e, EffectContext context) {
        TokenDefinition token = content.token(e.tokenId());
        int count = resolve(e.count(), context);
        Side side = e.side() == EffectDefinition.SummonSide.ALLY ? context.side() : context.side().opposite();
        Stats stats = new Stats(
            e.attack() == null ? token.attack() : resolve(e.attack(), context),
            e.health() == null ? token.health() : resolve(e.health(), context));
        for (int i = 0; i < count; i++) {
            if (!context.board().hasSide(side)) {
                context.log().append(new SummonFailed(0, token.id(), side, summonerId(context)));
                continue;
            }
            Band band = context.board().band(side);
            Unit unit = Unit.fromToken(token, context.board().nextUnitId(), stats);
            if (e.perk() != null) {
                unit.setPerk(e.perk());
            }
            boolean deferred = side == context.side() && e.position() == EffectDefinition.SummonPosition.HERE
                && context.deferredSummons() != null;
            if (deferred) {
                context.deferredSummons().add(unit);
                continue;
            }
            if (band.isFull()) {
                context.log().append(new SummonFailed(0, token.id(), side, summonerId(context)));
                continue;
            }
            int index = e.position() == EffectDefinition.SummonPosition.FRONT ? 0 : Math.min(context.summonIndex(), band.size());
            BandMutator.summon(band, side, index, unit, context.log(), summonerId(context));
            context.triggers().add(PendingTrigger.of(Trigger.ALLY_SUMMONED, unit));
            context.triggers().add(PendingTrigger.of(Trigger.SUMMONED, unit));
        }
    }

    private static com.forgottenheroes.engine.model.UnitId summonerId(EffectContext context) {
        return context.self() == null ? null : context.self().id();
    }

    private static int resolve(Amount amount, EffectContext context) {
        return amount == null ? 0 : amount.resolve(context.level(), context.attack());
    }
}
