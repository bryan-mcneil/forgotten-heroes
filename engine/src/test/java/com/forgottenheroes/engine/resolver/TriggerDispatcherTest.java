package com.forgottenheroes.engine.resolver;

import static com.forgottenheroes.engine.testing.TestFixtures.content;
import static com.forgottenheroes.engine.testing.TestFixtures.resolver;
import static com.forgottenheroes.engine.testing.TestFixtures.runAtTurn;
import static com.forgottenheroes.engine.testing.TestFixtures.unit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.content.AbilityDefinition;
import com.forgottenheroes.engine.content.Amount;
import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.EffectDefinition;
import com.forgottenheroes.engine.content.HeroDefinition;
import com.forgottenheroes.engine.content.Limit;
import com.forgottenheroes.engine.content.RelicDefinition;
import com.forgottenheroes.engine.content.TargetSelect;
import com.forgottenheroes.engine.content.TargetSpec;
import com.forgottenheroes.engine.content.TokenDefinition;
import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.event.GameEvent.AbilityTriggered;
import com.forgottenheroes.engine.event.GameEvent.GoldChanged;
import com.forgottenheroes.engine.event.GameEvent.RelicStocked;
import com.forgottenheroes.engine.event.GameEvent.StatsChanged;
import com.forgottenheroes.engine.event.GameEvent.SummonFailed;
import com.forgottenheroes.engine.event.GameEvent.UnitFell;
import com.forgottenheroes.engine.event.GameEvent.UnitSummoned;
import com.forgottenheroes.engine.model.Rank;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.TavernSlot;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.resolver.RuleViolation.Code;
import java.util.List;
import org.junit.jupiter.api.Test;

class TriggerDispatcherTest {

    private static RunState shopWith(String heroId, Unit... band) {
        RunState state = runAtTurn(16, 3);
        state.tavern().setHeroSlot(0, TavernSlot.hero(heroId, Stats.ZERO));
        for (Unit unit : band) {
            state.band().add(unit);
        }
        return state;
    }

    private static TavernResult poison(RunState state, int bandIndex) {
        state.tavern().setRelicSlot(0, TavernSlot.relic("poison_vial", 1));
        return resolver().apply(state, new Action.BuyRelic(0, bandIndex));
    }

    @Test
    void merchantSellGivesGoldPerLevel() {
        RunState state = runAtTurn(16, 1);
        state.band().add(unit("merchant", 4, 1, 2));
        state.setGold(0);
        TavernResult result = resolver().apply(state, new Action.Sell(0));
        assertThat(state.gold()).isEqualTo(4);
        assertThat(result.events().events()).extracting(GameEvent::getClass)
            .containsExactly(GameEvent.HeroSold.class, GoldChanged.class, AbilityTriggered.class, GoldChanged.class);
        AbilityTriggered fired = result.events().of(AbilityTriggered.class).getFirst();
        assertThat(fired.trigger()).isEqualTo(Trigger.SELL);
        assertThat(fired.level()).isEqualTo(2);
        assertThat(fired.position()).isNull();
        assertThat(result.events().of(GoldChanged.class).getLast().reason()).isEqualTo(GameEvent.GoldReason.ABILITY);
    }

    @Test
    void hotaruBuyGivesARandomAllyHealth() {
        Unit aoi = unit("aoi", 2, 2);
        Unit bard = unit("bard", 2, 1);
        RunState state = shopWith("hotaru", aoi, bard);
        TavernResult result = resolver().apply(state, new Action.Buy(0, 2));
        List<StatsChanged> changes = result.events().of(StatsChanged.class);
        assertThat(result.events().of(AbilityTriggered.class)).extracting(AbilityTriggered::trigger)
            .containsExactly(Trigger.BUY, Trigger.ALLY_SUMMONED);
        assertThat(changes).hasSize(2);
        StatsChanged hotaruGift = changes.getFirst();
        assertThat(hotaruGift.healthAfter() - hotaruGift.healthBefore()).isEqualTo(1);
        assertThat(hotaruGift.attackAfter()).isEqualTo(hotaruGift.attackBefore());
        assertThat(hotaruGift.temporary()).isFalse();
        assertThat(hotaruGift.source()).isEqualTo("hotaru");
        assertThat(aoi.maxHealth() + bard.maxHealth()).isEqualTo(4);
    }

    @Test
    void bardBuffsTheBoughtAllyUntilTheEndOfBattle() {
        Unit bard = unit("bard", 2, 1);
        RunState state = shopWith("archer", bard);
        resolver().apply(state, new Action.Buy(0, 1));
        Unit archer = state.band().at(1);
        assertThat(archer.attack()).isEqualTo(3);
        assertThat(archer.temporaryBonus()).isEqualTo(new Stats(1, 0));
        archer.clearTemporary();
        assertThat(archer.attack()).isEqualTo(2);
        assertThat(bard.stats()).hasToString("2/1");
    }

    @Test
    void treasurerAndHerbalistFireAtTheStartOfTheTurn() {
        RunState state = runAtTurn(16, 2);
        state.band().add(unit("treasurer", 1, 2, 2));
        state.band().add(unit("herbalist", 1, 3, 3));
        state.band().add(unit("herbalist", 1, 3));
        TavernResult result = resolver().apply(state, new Action.StartTurn());
        assertThat(state.gold()).isEqualTo(12);
        List<RelicStocked> stocked = result.events().of(RelicStocked.class);
        assertThat(stocked).hasSize(2);
        assertThat(stocked.getFirst().price()).isEqualTo(0);
        assertThat(stocked.getLast().price()).isEqualTo(2);
        assertThat(state.tavern().relicSlots()).hasSize(4);
        assertThat(state.tavern().relicSlot(2).itemId()).isEqualTo("ration");
        assertThat(state.tavern().relicSlot(3).price()).isEqualTo(2);
        assertThat(result.events().of(AbilityTriggered.class)).extracting(AbilityTriggered::heroId)
            .containsExactly("herbalist", "herbalist", "treasurer");
        state.assertInvariants();
    }

    @Test
    void herbalistUsesAnEmptyRelicShelfFirst() {
        RunState state = runAtTurn(16, 3);
        state.band().add(unit("herbalist", 1, 3));
        state.tavern().setRelicSlot(0, TavernSlot.EMPTY);
        var events = resolver().fire(state, Trigger.START_OF_TURN);
        assertThat(state.tavern().relicSlots()).hasSize(2);
        assertThat(events.of(RelicStocked.class).getFirst().slot()).isEqualTo(0);
        assertThat(state.tavern().relicSlot(0).itemId()).isEqualTo("ration");
    }

    @Test
    void standardBearerPoisonedBuffsTheTwoAlliesBehind() {
        Unit front = unit("aoi", 2, 2);
        Unit bearer = unit("standard_bearer", 3, 2);
        Unit behind1 = unit("archer", 2, 2);
        Unit behind2 = unit("bard", 2, 1);
        Unit behind3 = unit("hotaru", 1, 3);
        RunState state = shopWith("aoi", front, bearer, behind1, behind2, behind3);
        TavernResult result = poison(state, 1);
        assertThat(state.band().units()).containsExactly(front, behind1, behind2, behind3);
        assertThat(behind1.stats()).hasToString("3/3");
        assertThat(behind2.stats()).hasToString("3/2");
        assertThat(behind3.stats()).hasToString("1/3");
        assertThat(front.stats()).hasToString("2/2");
        assertThat(result.events().events()).extracting(GameEvent::getClass).containsSubsequence(
            GameEvent.RelicBought.class, UnitFell.class, AbilityTriggered.class, StatsChanged.class, StatsChanged.class);
        assertThat(result.events().of(UnitFell.class).getFirst().position().index()).isEqualTo(1);
        state.assertInvariants();
    }

    @Test
    void farmhandPoisonedLeavesAScarecrowInItsSlotAndBardBuffsIt() {
        Unit bard = unit("bard", 2, 1);
        Unit farmhand = unit("farmhand", 1, 3, 2);
        Unit archer = unit("archer", 2, 2);
        RunState state = shopWith("aoi", bard, farmhand, archer);
        TavernResult result = poison(state, 1);
        assertThat(state.band().units()).extracting(Unit::heroId).containsExactly("bard", "scarecrow", "archer");
        Unit scarecrow = state.band().at(1);
        assertThat(scarecrow.base()).isEqualTo(new Stats(2, 2));
        assertThat(scarecrow.attack()).isEqualTo(3);
        assertThat(scarecrow.temporaryBonus()).isEqualTo(new Stats(1, 0));
        UnitSummoned summoned = result.events().of(UnitSummoned.class).getFirst();
        assertThat(summoned.position().index()).isEqualTo(1);
        assertThat(summoned.summonerUnitId()).isEqualTo(farmhand.id());
        assertThat(result.events().of(AbilityTriggered.class)).extracting(AbilityTriggered::trigger)
            .containsExactly(Trigger.FALL, Trigger.ALLY_SUMMONED);
        state.assertInvariants();
    }

    @Test
    void plagueRatPoisonedHasNoEnemyToCurse() {
        Unit rat = unit("plague_rat", 3, 6);
        RunState state = shopWith("aoi", rat);
        TavernResult result = poison(state, 0);
        assertThat(state.band().isEmpty()).isTrue();
        assertThat(result.events().of(SummonFailed.class).getFirst().tokenId()).isEqualTo("dirty_rat");
    }

    @Test
    void alchemistPoisonedCascadesThroughTheFallsItCauses() {
        Unit alchemist = unit("alchemist", 4, 2);
        Unit weakAoi = unit("aoi", 2, 1);
        Unit sturdy = unit("hotaru", 1, 3);
        RunState state = shopWith("aoi", alchemist, weakAoi, sturdy);
        TavernResult result = poison(state, 0);
        assertThat(state.band().units()).containsExactly(sturdy);
        assertThat(sturdy.stats()).hasToString("2/2");
        assertThat(sturdy.base()).isEqualTo(new Stats(1, 1));
        assertThat(sturdy.permanentBonus()).isEqualTo(new Stats(1, 1));
        assertThat(result.events().of(UnitFell.class)).extracting(UnitFell::heroId).containsExactly("alchemist", "aoi");
        assertThat(result.events().of(AbilityTriggered.class)).extracting(AbilityTriggered::heroId).containsExactly("alchemist", "aoi");
        state.assertInvariants();
    }

    @Test
    void fallsResolveHigherAttackFirstAndDeadUnitsDoNotAnswerLaterMoments() {
        Unit strong = unit("aoi", 5, 2);
        Unit weak = unit("aoi", 2, 2);
        Unit witness = unit("hotaru", 1, 3);
        RunState state = shopWith("aoi", weak, strong, witness);
        ContentIndex bomb = withRelic("bomb", new EffectDefinition.Kill(TargetSpec.of(TargetSelect.ALL_ALLIES)));
        TavernResolver resolver = new TavernResolver(bomb);
        state.tavern().setRelicSlot(0, TavernSlot.relic("bomb", 3));
        TavernResult result = resolver.apply(state, new Action.BuyRelic(0, null));
        assertThat(result.events().of(UnitFell.class)).extracting(UnitFell::unitId).containsExactly(strong.id(), weak.id(), witness.id());
        assertThat(result.events().of(AbilityTriggered.class)).extracting(AbilityTriggered::unitId).containsExactly(strong.id(), weak.id());
        assertThat(result.events().of(StatsChanged.class)).isEmpty();
        assertThat(state.band().isEmpty()).isTrue();
    }

    @Test
    void aPerTurnLimitStopsTheSecondActivation() {
        HeroDefinition greedy = new HeroDefinition("greedy", "Greedy", null, null, Rank.I, 1, 1, "s",
            new AbilityDefinition(Trigger.ALLY_SUMMONED, "Ally summoned → +1 gold, once per turn",
                List.of(new EffectDefinition.GainGold(Amount.flat(1))), new Limit(Amount.flat(1), null)));
        ContentIndex index = withHero(greedy);
        TavernResolver resolver = new TavernResolver(index);
        RunState state = runAtTurn(16, 1);
        state.band().add(new Unit(state.nextUnitId(), "greedy", new Stats(1, 1)));
        state.tavern().setHeroSlot(0, TavernSlot.hero("aoi", Stats.ZERO));
        state.tavern().setHeroSlot(1, TavernSlot.hero("aoi", Stats.ZERO));
        resolver.apply(state, new Action.Buy(0, 1));
        assertThat(state.gold()).isEqualTo(8);
        resolver.apply(state, new Action.Buy(1, 2));
        assertThat(state.gold()).isEqualTo(5);
        resolver.apply(state, new Action.StartTurn());
        state.tavern().setHeroSlot(0, TavernSlot.hero("aoi", Stats.ZERO));
        resolver.apply(state, new Action.Buy(0, 3));
        assertThat(state.gold()).isEqualTo(8);
    }

    @Test
    void anEndlessCascadeHitsTheEngineGuard() {
        TokenDefinition dud = new TokenDefinition("dud", "Dud", 0, 0, "s");
        HeroDefinition chain = new HeroDefinition("chain", "Chain", null, null, Rank.I, 1, 1, "s",
            new AbilityDefinition(Trigger.ALLY_FALL, "Ally falls → summon a Dud that falls at once",
                List.of(new EffectDefinition.Summon("dud", null, null, null, null, null, null)), null));
        ContentIndex index = new ContentIndex(List.of(chain, content().hero("aoi")), List.of(dud), List.of(content().relic("poison_vial")));
        TavernResolver resolver = new TavernResolver(index);
        RunState state = runAtTurn(16, 3);
        state.band().add(new Unit(state.nextUnitId(), "chain", new Stats(1, 1)));
        state.band().add(new Unit(state.nextUnitId(), "aoi", new Stats(2, 2)));
        state.tavern().setRelicSlot(0, TavernSlot.relic("poison_vial", 1));
        assertThatThrownBy(() -> resolver.apply(state, new Action.BuyRelic(0, 1)))
            .isInstanceOf(RuleViolation.class)
            .hasMessageContaining("ENGINE_GUARD");
    }

    @Test
    void endOfTurnCanBeFiredOutsideAnAction() {
        RunState state = runAtTurn(16, 1);
        state.band().add(unit("treasurer", 1, 2));
        int before = state.lastEventSeq();
        assertThat(resolver().fire(state, Trigger.END_OF_TURN).isEmpty()).isTrue();
        assertThat(state.lastEventSeq()).isEqualTo(before);
    }

    private static ContentIndex withRelic(String id, EffectDefinition effect) {
        RelicDefinition relic = new RelicDefinition(id, id, Rank.I, 3, "test", "s", List.of(effect));
        List<RelicDefinition> relics = new java.util.ArrayList<>(content().relics());
        relics.add(relic);
        return new ContentIndex(content().heroes(), content().tokens(), relics);
    }

    private static ContentIndex withHero(HeroDefinition hero) {
        List<HeroDefinition> heroes = new java.util.ArrayList<>(content().heroes());
        heroes.add(hero);
        return new ContentIndex(heroes, content().tokens(), content().relics());
    }
}
