package com.forgottenheroes.engine.resolver.battle;

import static com.forgottenheroes.engine.testing.TestFixtures.band;
import static com.forgottenheroes.engine.testing.TestFixtures.content;
import static com.forgottenheroes.engine.testing.TestFixtures.unit;
import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.engine.content.AbilityDefinition;
import com.forgottenheroes.engine.content.Amount;
import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.EffectDefinition;
import com.forgottenheroes.engine.content.HeroDefinition;
import com.forgottenheroes.engine.content.TargetSelect;
import com.forgottenheroes.engine.content.TargetSpec;
import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.event.GameEvent.AbilityTriggered;
import com.forgottenheroes.engine.event.GameEvent.DamageDealt;
import com.forgottenheroes.engine.event.GameEvent.StatsChanged;
import com.forgottenheroes.engine.event.GameEvent.SummonFailed;
import com.forgottenheroes.engine.event.GameEvent.UnitSummoned;
import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.model.Position;
import com.forgottenheroes.engine.model.Rank;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.Unit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BattleSummonsTest {

    private static final BattleResolver RESOLVER = new BattleResolver(content());

    @Test
    void farmhandLeavesAScarecrowInItsPlace() {
        Unit farmhand = unit("farmhand", 1, 3, 2);
        Unit back = unit("k", 1, 10);
        Unit enemy = unit("e", 3, 20);
        BattleResult result = RESOLVER.resolve(band(farmhand, back), band(enemy), 1);
        UnitSummoned scarecrow = result.events().of(UnitSummoned.class).getFirst();
        assertThat(scarecrow.heroId()).isEqualTo("scarecrow");
        assertThat(scarecrow.stats()).isEqualTo(new Stats(2, 2));
        assertThat(scarecrow.position()).isEqualTo(new Position(Side.HOME, 0));
        assertThat(result.events().of(AbilityTriggered.class)).extracting(AbilityTriggered::trigger).contains(Trigger.FALL);
        assertThat(result.events().of(DamageDealt.class)).anyMatch(hit -> hit.sourceUnitId().equals(scarecrow.unitId()));
    }

    @Test
    void aFullBandCannotTakeASummon() {
        Unit farmhand = unit("farmhand", 1, 3);
        ContentIndex index = withHero(new HeroDefinition("spawner", "Spawner", null, null, Rank.I, 1, 1, "s",
            new AbilityDefinition(Trigger.START_OF_BATTLE, "Start of battle → summon a Scarecrow",
                List.of(new EffectDefinition.Summon("scarecrow", null, null, null, null, null, null)), null)));
        BattleResolver resolver = new BattleResolver(index);
        Unit spawner = new Unit(new com.forgottenheroes.engine.model.UnitId("sp"), "spawner", new Stats(1, 1));
        BattleResult result = resolver.resolve(band(spawner, farmhand, unit("a", 1, 1), unit("b", 1, 1), unit("c", 1, 1)),
            band(unit("e", 1, 50)), 1);
        SummonFailed failed = result.events().of(SummonFailed.class).getFirst();
        assertThat(failed.tokenId()).isEqualTo("scarecrow");
        assertThat(failed.side()).isEqualTo(Side.HOME);
        assertThat(failed.summonerUnitId()).isEqualTo(spawner.id());
    }

    @Test
    void plagueRatCursesTheEnemyFront() {
        Unit rat = unit("plague_rat", 3, 6);
        Unit killer = unit("k", 6, 2);
        Unit second = unit("s", 1, 10);
        BattleResult result = RESOLVER.resolve(band(rat), band(killer, second), 1);
        UnitSummoned dirtyRat = result.events().of(UnitSummoned.class).getFirst();
        assertThat(dirtyRat.heroId()).isEqualTo("dirty_rat");
        assertThat(dirtyRat.position()).isEqualTo(new Position(Side.AWAY, 0));
        assertThat(result.events().of(AbilityTriggered.class)).extracting(AbilityTriggered::trigger).contains(Trigger.FALL);
        assertThat(result.homeOutcome()).isEqualTo(Outcome.LOSS);
    }

    @Test
    void bardAndBeastmasterBuffBattleSummons() {
        Unit bard = unit("bard", 2, 1);
        Unit beastmaster = unit("beastmaster", 3, 2);
        Unit farmhand = unit("farmhand", 1, 3);
        Unit enemy = unit("e", 3, 30);
        BattleResult result = RESOLVER.resolve(band(farmhand, bard, beastmaster), band(enemy), 1);
        UnitSummoned scarecrow = result.events().of(UnitSummoned.class).getFirst();
        List<StatsChanged> buffs = result.events().of(StatsChanged.class).stream()
            .filter(change -> change.unitId().equals(scarecrow.unitId())).toList();
        assertThat(buffs).extracting(StatsChanged::source).containsExactly("beastmaster", "bard");
        assertThat(buffs.getLast().attackAfter()).isEqualTo(1 + 2 + 1);
        assertThat(buffs.getLast().healthAfter()).isEqualTo(2);
        assertThat(buffs).allMatch(StatsChanged::temporary);
    }

    @Test
    void druidSummonsTwoWolvesButOnlyOneWhenOneSlotIsFree() {
        Unit druid = unit("druid", 2, 2);
        Unit enemy = unit("e", 5, 40);
        BattleResult roomy = RESOLVER.resolve(band(druid), band(enemy), 1);
        List<UnitSummoned> wolves = roomy.events().of(UnitSummoned.class);
        assertThat(wolves).hasSize(2);
        assertThat(wolves).extracting(summon -> summon.position().index()).containsExactly(0, 1);
        assertThat(wolves).allMatch(summon -> summon.stats().equals(new Stats(2, 2)));

        Unit druid2 = unit("druid", 2, 2);
        BattleResult tight = RESOLVER.resolve(band(druid2, unit("a", 1, 10), unit("b", 1, 10), unit("c", 1, 10), unit("d", 1, 10)),
            band(unit("x", 5, 40)), 1);
        assertThat(tight.events().of(UnitSummoned.class)).hasSize(1);
        assertThat(tight.events().of(SummonFailed.class)).hasSize(1);
    }

    @Test
    void rankThreeAbilitiesDoWhatTheirTextSays() {
        Unit assassin = unit("assassin", 4, 3, 2);
        Unit weak = unit("w", 1, 5);
        Unit strong = unit("s", 1, 12);
        BattleResult opener = RESOLVER.resolve(band(assassin), band(strong, weak), 1);
        DamageDealt stab = opener.events().of(DamageDealt.class).getFirst();
        assertThat(stab.targetUnitId()).isEqualTo(weak.id());
        assertThat(stab.amount()).isEqualTo(8);

        Unit golem = unit("clay_golem", 3, 7);
        Unit mate = unit("m", 1, 10);
        BattleResult golemFight = RESOLVER.resolve(band(golem, mate), band(unit("e", 1, 3)), 1);
        assertThat(golemFight.events().of(DamageDealt.class)).anyMatch(hit -> hit.targetUnitId().equals(mate.id()) && hit.amount() == 1);

        Unit berserker = unit("berserker", 6, 3, 3);
        Unit ahead = unit("a", 1, 20);
        Unit behind = unit("b", 1, 20);
        BattleResult rage = RESOLVER.resolve(band(ahead, berserker, behind), band(unit("assassin", 4, 3)), 1);
        List<DamageDealt> rageHits = rage.events().of(DamageDealt.class).stream().filter(hit -> hit.sourceUnitId().equals(berserker.id())).toList();
        assertThat(rageHits).extracting(DamageDealt::amount).containsExactly(9, 9);
        assertThat(rageHits).extracting(DamageDealt::targetUnitId).containsExactly(ahead.id(), behind.id());

        Unit iwao = unit("iwao", 3, 4);
        Unit pupil = unit("p", 1, 1);
        BattleResult stone = RESOLVER.resolve(band(iwao, pupil), band(unit("e", 1, 20)), 1);
        StatsChanged lesson = stone.events().of(StatsChanged.class).getFirst();
        assertThat(lesson.unitId()).isEqualTo(pupil.id());
        assertThat(lesson.attackAfter() - lesson.attackBefore()).isEqualTo(1);
        assertThat(lesson.healthAfter() - lesson.healthBefore()).isEqualTo(2);
    }

    @Test
    void setHealthPercentIsNotDamage() {
        ContentIndex index = withHero(new HeroDefinition("witch_test", "Witch", null, null, Rank.I, 1, 1, "s",
            new AbilityDefinition(Trigger.START_OF_BATTLE, "Start of battle → the healthiest enemy loses a third of its health",
                List.of(new EffectDefinition.SetHealthPercent(TargetSpec.of(TargetSelect.HIGHEST_HEALTH_ENEMY), Amount.perLevel(33))), null)));
        BattleResolver resolver = new BattleResolver(index);
        Unit witch = new Unit(new com.forgottenheroes.engine.model.UnitId("w"), "witch_test", new Stats(1, 1));
        Unit maiden = unit("shield_maiden", 2, 30);
        BattleResult result = resolver.resolve(band(witch), band(maiden), 1);
        StatsChanged curse = result.events().of(StatsChanged.class).getFirst();
        assertThat(curse.unitId()).isEqualTo(maiden.id());
        assertThat(curse.healthBefore()).isEqualTo(30);
        assertThat(curse.healthAfter()).isEqualTo(21);
        assertThat(result.events().of(AbilityTriggered.class)).extracting(AbilityTriggered::trigger).startsWith(Trigger.START_OF_BATTLE);
        assertThat(result.events().of(AbilityTriggered.class).stream().filter(fired -> fired.trigger() == Trigger.HURT).count())
            .isEqualTo(1L);
    }

    private static ContentIndex withHero(HeroDefinition hero) {
        List<HeroDefinition> heroes = new ArrayList<>(content().heroes());
        heroes.add(hero);
        return new ContentIndex(heroes, content().tokens(), content().relics());
    }
}
