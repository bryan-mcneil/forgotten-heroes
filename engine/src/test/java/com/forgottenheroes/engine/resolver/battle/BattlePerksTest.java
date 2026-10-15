package com.forgottenheroes.engine.resolver.battle;

import static com.forgottenheroes.engine.testing.TestFixtures.band;
import static com.forgottenheroes.engine.testing.TestFixtures.content;
import static com.forgottenheroes.engine.testing.TestFixtures.unit;
import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.event.GameEvent.AbilityTriggered;
import com.forgottenheroes.engine.event.GameEvent.DamageDealt;
import com.forgottenheroes.engine.event.GameEvent.DamageKind;
import com.forgottenheroes.engine.event.GameEvent.Knockout;
import com.forgottenheroes.engine.event.GameEvent.PerkConsumed;
import com.forgottenheroes.engine.event.GameEvent.UnitFell;
import com.forgottenheroes.engine.event.GameEvent.UnitRevived;
import com.forgottenheroes.engine.event.GameEvent.UnitSummoned;
import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.Position;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.Unit;
import java.util.List;
import org.junit.jupiter.api.Test;

class BattlePerksTest {

    private static final BattleResolver RESOLVER = new BattleResolver(content());

    private static List<DamageDealt> hitsOn(BattleResult result, Unit target) {
        return result.events().of(DamageDealt.class).stream().filter(hit -> hit.targetUnitId().equals(target.id())).toList();
    }

    @Test
    void wardTakesTwentyOffOnceThenShatters() {
        Unit warded = unit("w", 1, 30, Perk.WARD);
        Unit brute = unit("b", 25, 50);
        BattleResult result = RESOLVER.resolve(band(warded), band(brute), 1);
        List<DamageDealt> hits = hitsOn(result, warded);
        assertThat(hits.get(0).amount()).isEqualTo(5);
        assertThat(hits.get(1).amount()).isEqualTo(25);
        assertThat(result.events().of(PerkConsumed.class)).hasSize(1);
        assertThat(result.events().events().indexOf(result.events().of(PerkConsumed.class).getFirst()))
            .isLessThan(result.events().events().indexOf(hits.get(0)));
        assertThat(result.homeOutcome()).isEqualTo(Outcome.LOSS);
    }

    @Test
    void aegisIgnoresOneHitEntirelyAndNobodyIsHurt() {
        Unit shielded = unit("shield_maiden", 2, 5, Perk.AEGIS);
        Unit brute = unit("b", 10, 3);
        BattleResult result = RESOLVER.resolve(band(shielded), band(brute), 1);
        List<DamageDealt> hits = hitsOn(result, shielded);
        assertThat(hits.get(0).amount()).isZero();
        assertThat(hits.get(1).amount()).isEqualTo(10);
        assertThat(result.events().of(AbilityTriggered.class)).extracting(AbilityTriggered::trigger).containsExactly(Trigger.HURT);
        assertThat(result.events().of(PerkConsumed.class).getFirst().perk()).isEqualTo(Perk.AEGIS);
    }

    @Test
    void enchantedBladeAddsTwentyToTheFirstAttackOnly() {
        Unit blade = unit("e", 1, 10, Perk.ENCHANTED_BLADE);
        Unit wall = unit("x", 0, 30);
        BattleResult result = RESOLVER.resolve(band(blade), band(wall), 1);
        List<DamageDealt> hits = hitsOn(result, wall);
        assertThat(hits.get(0).amount()).isEqualTo(21);
        assertThat(hits.get(1).amount()).isEqualTo(1);
        assertThat(result.events().of(PerkConsumed.class).getFirst().unitId()).isEqualTo(blade.id());
    }

    @Test
    void flameOilSplashesTheUnitBehindThroughItsOwnPerks() {
        Unit torch = unit("t", 2, 20, Perk.FLAME_OIL);
        Unit front = unit("f", 1, 20);
        Unit behind = unit("shield_maiden", 2, 9, Perk.CHAINMAIL);
        BattleResult result = RESOLVER.resolve(band(torch), band(front, behind), 1);
        DamageDealt splash = result.events().of(DamageDealt.class).stream().filter(hit -> hit.kind() == DamageKind.SPLASH).findFirst().orElseThrow();
        assertThat(splash.targetUnitId()).isEqualTo(behind.id());
        assertThat(splash.amount()).isEqualTo(3);
        assertThat(splash.sourceUnitId()).isEqualTo(torch.id());
        assertThat(result.events().of(AbilityTriggered.class)).anyMatch(fired -> fired.unitId().equals(behind.id()) && fired.trigger() == Trigger.HURT);
    }

    @Test
    void flameOilCanKnockOutTheUnitBehind() {
        Unit torch = unit("t", 1, 20, Perk.FLAME_OIL);
        Unit front = unit("f", 1, 20);
        Unit frail = unit("x", 1, 4);
        BattleResult result = RESOLVER.resolve(band(torch), band(front, frail), 1);
        assertThat(result.events().of(Knockout.class).getFirst().targetUnitId()).isEqualTo(frail.id());
        assertThat(result.events().of(UnitFell.class).getFirst().unitId()).isEqualTo(frail.id());
    }

    @Test
    void venomMakesAnyDamagingHitLethal() {
        Unit viper = unit("v", 1, 50);
        viper.setPerk(Perk.VENOM);
        Unit giant = unit("g", 1, 50, Perk.CHAINMAIL);
        BattleResult result = RESOLVER.resolve(band(viper), band(giant), 1);
        assertThat(hitsOn(result, giant).getFirst().amount()).isEqualTo(50);
        assertThat(result.clashes()).isEqualTo(1);
        assertThat(result.homeOutcome()).isEqualTo(Outcome.WIN);
        Unit harmless = unit("h", 0, 5);
        harmless.setPerk(Perk.VENOM);
        BattleResult nothing = RESOLVER.resolve(band(harmless), band(unit("x", 0, 5)), 1);
        assertThat(nothing.events().of(DamageDealt.class)).allMatch(hit -> hit.amount() == 0);
        assertThat(nothing.homeOutcome()).isEqualTo(Outcome.DRAW);
    }

    @Test
    void fairyCharmLeavesASpriteInTheSlot() {
        Unit charmed = unit("c", 1, 1, Perk.FAIRY_CHARM);
        Unit back = unit("k", 3, 3);
        Unit enemy = unit("e", 1, 8);
        BattleResult result = RESOLVER.resolve(band(charmed, back), band(enemy), 1);
        UnitSummoned sprite = result.events().of(UnitSummoned.class).getFirst();
        assertThat(sprite.heroId()).isEqualTo("sprite");
        assertThat(sprite.position()).isEqualTo(new Position(Side.HOME, 0));
        assertThat(sprite.stats()).isEqualTo(new Stats(1, 1));
        assertThat(sprite.summonerUnitId()).isEqualTo(charmed.id());
        assertThat(result.homeOutcome()).isEqualTo(Outcome.WIN);
    }

    @Test
    void phoenixFeatherReturnsTheUnitAsAOneOne() {
        Unit phoenix = unit("p", 5, 1, Perk.PHOENIX_FEATHER);
        Unit enemy = unit("e", 1, 7);
        BattleResult result = RESOLVER.resolve(band(phoenix), band(enemy), 1);
        assertThat(result.events().of(UnitRevived.class).getFirst()).satisfies(revived -> {
            assertThat(revived.unitId()).isEqualTo(phoenix.id());
            assertThat(revived.position()).isEqualTo(new Position(Side.HOME, 0));
            assertThat(revived.stats()).isEqualTo(new Stats(1, 1));
        });
        assertThat(result.events().of(PerkConsumed.class).getFirst().perk()).isEqualTo(Perk.PHOENIX_FEATHER);
        assertThat(result.events().of(UnitFell.class)).extracting(UnitFell::unitId).containsExactly(phoenix.id(), phoenix.id());
        assertThat(result.homeOutcome()).isEqualTo(Outcome.LOSS);
        assertThat(result.clashes()).isEqualTo(2);
        assertThat(enemy.health()).isEqualTo(7);
    }

    @Test
    void abilityDamageGoesThroughTheDefendersPerks() {
        Unit archer = unit("archer", 2, 2);
        Unit shielded = unit("x", 1, 1, Perk.AEGIS);
        BattleResult result = RESOLVER.resolve(band(archer), band(shielded), 1);
        DamageDealt opener = result.events().of(DamageDealt.class).getFirst();
        assertThat(opener.kind()).isEqualTo(DamageKind.ABILITY);
        assertThat(opener.amount()).isZero();
        assertThat(result.events().of(PerkConsumed.class).getFirst().perk()).isEqualTo(Perk.AEGIS);
        assertThat(result.homeOutcome()).isEqualTo(Outcome.WIN);
    }
}
