package com.forgottenheroes.engine.content;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.forgottenheroes.engine.model.Perk;

/**
 * One thing an ability or relic does (GDD §7). The JSON {@code "type"} field picks the record:
 * Jackson reads it and builds the matching subtype; the sealed interface lets the interpreter
 * {@code switch} over every kind and the compiler checks that none is forgotten.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = EffectDefinition.ModifyStats.class, name = "MODIFY_STATS"),
    @JsonSubTypes.Type(value = EffectDefinition.DealDamage.class, name = "DEAL_DAMAGE"),
    @JsonSubTypes.Type(value = EffectDefinition.SetHealthPercent.class, name = "SET_HEALTH_PERCENT"),
    @JsonSubTypes.Type(value = EffectDefinition.Summon.class, name = "SUMMON"),
    @JsonSubTypes.Type(value = EffectDefinition.GainGold.class, name = "GAIN_GOLD"),
    @JsonSubTypes.Type(value = EffectDefinition.GivePerk.class, name = "GIVE_PERK"),
    @JsonSubTypes.Type(value = EffectDefinition.StockRelic.class, name = "STOCK_RELIC"),
    @JsonSubTypes.Type(value = EffectDefinition.GiveXp.class, name = "GIVE_XP"),
    @JsonSubTypes.Type(value = EffectDefinition.BuffTavern.class, name = "BUFF_TAVERN"),
    @JsonSubTypes.Type(value = EffectDefinition.Kill.class, name = "KILL")
})
public sealed interface EffectDefinition {

    /** Add attack and/or health to the targets; {@code untilEndOfBattle} makes a tavern buff temporary. */
    record ModifyStats(TargetSpec target, Amount attack, Amount health, boolean untilEndOfBattle)
            implements EffectDefinition {
        public ModifyStats {
            requireTarget(target);
            if (attack == null && health == null) {
                throw new IllegalArgumentException("MODIFY_STATS needs attack or health");
            }
        }
    }

    /** Deal damage to the targets; the optional condition can multiply it. */
    record DealDamage(TargetSpec target, Amount amount, Condition condition) implements EffectDefinition {
        public DealDamage {
            requireTarget(target);
            requireAmount(amount, "DEAL_DAMAGE");
        }
    }

    /** Remove a percentage of the targets' current health without it counting as damage (Witch). */
    record SetHealthPercent(TargetSpec target, Amount percent) implements EffectDefinition {
        public SetHealthPercent {
            requireTarget(target);
            requireAmount(percent, "SET_HEALTH_PERCENT");
        }
    }

    /** Put token units on the board; stats default to the token's own, side ALLY and position HERE. */
    record Summon(String tokenId, Amount count, Amount attack, Amount health, Perk perk, SummonSide side,
                  SummonPosition position) implements EffectDefinition {
        public Summon {
            if (tokenId == null || tokenId.isBlank()) {
                throw new IllegalArgumentException("SUMMON needs a tokenId");
            }
            if (count == null) {
                count = Amount.flat(1);
            }
            if (side == null) {
                side = SummonSide.ALLY;
            }
            if (position == null) {
                position = SummonPosition.HERE;
            }
        }
    }

    /** Give the player gold (tavern only). */
    record GainGold(Amount amount) implements EffectDefinition {
        public GainGold {
            requireAmount(amount, "GAIN_GOLD");
        }
    }

    /** Attach a perk to the targets, replacing any perk they hold. */
    record GivePerk(TargetSpec target, Perk perk) implements EffectDefinition {
        public GivePerk {
            requireTarget(target);
            if (perk == null) {
                throw new IllegalArgumentException("GIVE_PERK needs a perk");
            }
        }
    }

    /** Put a relic in the tavern at a price (Herbalist's Ration). */
    record StockRelic(String relicId, Amount price) implements EffectDefinition {
        public StockRelic {
            if (relicId == null || relicId.isBlank()) {
                throw new IllegalArgumentException("STOCK_RELIC needs a relicId");
            }
            requireAmount(price, "STOCK_RELIC");
        }
    }

    /** Give experience points to the targets. */
    record GiveXp(TargetSpec target, Amount amount) implements EffectDefinition {
        public GiveXp {
            requireTarget(target);
            requireAmount(amount, "GIVE_XP");
        }
    }

    /** Buff every hero in the tavern now and, when permanent, every hero that enters it later (Royal Decree). */
    record BuffTavern(Amount attack, Amount health, boolean permanent) implements EffectDefinition {
        public BuffTavern {
            if (attack == null && health == null) {
                throw new IllegalArgumentException("BUFF_TAVERN needs attack or health");
            }
        }
    }

    /** The targets fall at once, firing their Fall abilities (Poison Vial). */
    record Kill(TargetSpec target) implements EffectDefinition {
        public Kill {
            requireTarget(target);
        }
    }

    enum SummonSide { ALLY, ENEMY }

    enum SummonPosition { HERE, FRONT }

    /** The target of this effect, or null for effects that have none (gold, stock, tavern buff). */
    default TargetSpec target() {
        return switch (this) {
            case ModifyStats e -> e.target();
            case DealDamage e -> e.target();
            case SetHealthPercent e -> e.target();
            case GivePerk e -> e.target();
            case GiveXp e -> e.target();
            case Kill e -> e.target();
            case Summon e -> null;
            case GainGold e -> null;
            case StockRelic e -> null;
            case BuffTavern e -> null;
        };
    }

    private static void requireTarget(TargetSpec target) {
        if (target == null) {
            throw new IllegalArgumentException("effect needs a target");
        }
    }

    private static void requireAmount(Amount amount, String effect) {
        if (amount == null) {
            throw new IllegalArgumentException(effect + " needs an amount");
        }
    }
}
