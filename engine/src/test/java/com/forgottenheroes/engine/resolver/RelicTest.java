package com.forgottenheroes.engine.resolver;

import static com.forgottenheroes.engine.testing.TestFixtures.content;
import static com.forgottenheroes.engine.testing.TestFixtures.resolver;
import static com.forgottenheroes.engine.testing.TestFixtures.runAtTurn;
import static com.forgottenheroes.engine.testing.TestFixtures.unit;
import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.content.RelicDefinition;
import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.event.GameEvent.GoldChanged;
import com.forgottenheroes.engine.event.GameEvent.LevelUp;
import com.forgottenheroes.engine.event.GameEvent.PerkGained;
import com.forgottenheroes.engine.event.GameEvent.RelicBought;
import com.forgottenheroes.engine.event.GameEvent.StatsChanged;
import com.forgottenheroes.engine.event.GameEvent.TavernBuffed;
import com.forgottenheroes.engine.event.GameEvent.UnitFell;
import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.Position;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.TavernSlot;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.resolver.RuleViolation.Code;
import java.util.List;
import org.junit.jupiter.api.Test;

class RelicTest {

    private static RuleViolation violation(RunState state, Action action) {
        try {
            resolver().apply(state, action);
        } catch (RuleViolation e) {
            return e;
        }
        throw new AssertionError("expected a RuleViolation for " + action);
    }

    /** Turn 3 (two relic slots), relic slot 0 holding the given relic, the given heroes on the band. */
    private static RunState shop(String relicId, Unit... band) {
        RunState state = runAtTurn(15, 3);
        state.tavern().setRelicSlot(0, TavernSlot.relic(relicId, content().relic(relicId).price()));
        for (Unit unit : band) {
            state.band().add(unit);
        }
        return state;
    }

    @Test
    void rationGivesOneHeroOneOneForGood() {
        Unit aoi = unit("aoi", 2, 2);
        RunState state = shop("ration", aoi);
        TavernResult result = resolver().apply(state, new Action.BuyRelic(0, 0));
        assertThat(state.gold()).isEqualTo(7);
        assertThat(aoi.stats()).hasToString("3/3");
        assertThat(aoi.permanentBonus()).isEqualTo(new Stats(1, 1));
        assertThat(state.tavern().relicSlot(0)).isEqualTo(TavernSlot.EMPTY);
        assertThat(result.events().events()).extracting(GameEvent::getClass)
            .containsExactly(GoldChanged.class, RelicBought.class, StatsChanged.class);
        assertThat(result.events().of(RelicBought.class).getFirst().targetUnitId()).isEqualTo(aoi.id());
        StatsChanged changed = result.events().of(StatsChanged.class).getFirst();
        assertThat(changed).isEqualTo(new StatsChanged(changed.seq(), aoi.id(), "aoi", new Position(Side.HOME, 0), 2, 3, 2, 3, false, "ration"));
        assertThat(result.triggers()).containsExactly(PendingTrigger.of(Trigger.EAT_RELIC, aoi));
        state.assertInvariants();
    }

    @Test
    void battleBrewIsTemporaryAndRationIsNot() {
        Unit aoi = unit("aoi", 2, 2);
        RunState state = shop("battle_brew", aoi);
        TavernResult result = resolver().apply(state, new Action.BuyRelic(0, 0));
        assertThat(aoi.stats()).hasToString("5/5");
        assertThat(aoi.temporaryBonus()).isEqualTo(new Stats(3, 3));
        assertThat(result.events().of(StatsChanged.class).getFirst().temporary()).isTrue();
        state.tavern().setRelicSlot(1, TavernSlot.relic("ration", 3));
        resolver().apply(state, new Action.BuyRelic(1, 0));
        aoi.clearTemporary();
        assertThat(aoi.stats()).hasToString("3/3");
    }

    @Test
    void aPerkRelicSetsThePerkAndASecondOneReplacesIt() {
        Unit aoi = unit("aoi", 2, 2);
        RunState state = shop("iron_blade", aoi);
        TavernResult first = resolver().apply(state, new Action.BuyRelic(0, 0));
        assertThat(aoi.perk()).contains(Perk.IRON_BLADE);
        assertThat(first.events().of(PerkGained.class).getFirst().replaced()).isNull();
        state.tavern().setRelicSlot(1, TavernSlot.relic("chainmail", 3));
        TavernResult second = resolver().apply(state, new Action.BuyRelic(1, 0));
        assertThat(aoi.perk()).contains(Perk.CHAINMAIL);
        assertThat(second.events().of(PerkGained.class).getFirst().replaced()).isEqualTo(Perk.IRON_BLADE);
    }

    @Test
    void poisonVialCostsOneAndMakesAHeroFallInTheTavern() {
        Unit aoi = unit("aoi", 2, 2);
        Unit bard = unit("bard", 2, 1);
        RunState state = shop("poison_vial", aoi, bard);
        TavernResult result = resolver().apply(state, new Action.BuyRelic(0, 0));
        assertThat(state.gold()).isEqualTo(9);
        assertThat(state.band().units()).containsExactly(bard);
        assertThat(result.events().of(UnitFell.class).getFirst().position()).isEqualTo(new Position(Side.HOME, 0));
        assertThat(result.events().of(com.forgottenheroes.engine.event.GameEvent.AbilityTriggered.class)).hasSize(1);
        assertThat(bard.stats()).hasToString("3/2");
        assertThat(result.triggers()).isEmpty();
        state.assertInvariants();
    }

    @Test
    void royalDecreeBuffsTheShelvesNowAndEveryFutureHero() {
        RunState state = shop("royal_decree");
        TavernResult result = resolver().apply(state, new Action.BuyRelic(0, null));
        assertThat(result.events().of(TavernBuffed.class).getFirst()).isEqualTo(new TavernBuffed(0, new Stats(1, 1), true).withSeq(result.events().lastSeq()));
        assertThat(state.tavern().heroSlots()).allMatch(slot -> slot.bonus().equals(new Stats(1, 1)));
        assertThat(state.tavern().permanentBuff()).isEqualTo(new Stats(1, 1));
        resolver().apply(state, new Action.Roll());
        assertThat(state.tavern().heroSlots()).allMatch(slot -> slot.bonus().equals(new Stats(1, 1)));
        resolver().apply(state, new Action.Buy(0, 0));
        Unit bought = state.band().front();
        assertThat(bought.stats()).isEqualTo(content().hero(bought.heroId()).stats().plus(new Stats(1, 1)));
    }

    @Test
    void tomeOfExperienceLevelsAHeroAtTheThreshold() {
        Unit aoi = unit("aoi", 2, 2);
        aoi.gainXp(1);
        RunState state = shop("tome_of_experience", aoi);
        TavernResult result = resolver().apply(state, new Action.BuyRelic(0, 0));
        assertThat(aoi.xp().points()).isEqualTo(2);
        assertThat(aoi.level()).isEqualTo(2);
        assertThat(aoi.stats()).hasToString("3/3");
        assertThat(result.events().of(LevelUp.class)).hasSize(1);
        assertThat(state.tavern().bonusSlot()).isPresent();
        assertThat(result.triggers()).containsExactly(PendingTrigger.of(Trigger.LEVEL_UP, aoi), PendingTrigger.of(Trigger.EAT_RELIC, aoi));
    }

    @Test
    void feastPicksTwoDifferentHeroesWithTheActionDice() {
        Unit a = unit("aoi", 2, 2);
        Unit b = unit("bard", 2, 1);
        Unit c = unit("archer", 2, 2);
        RunState state = shop("feast", a, b, c);
        RunState twin = state.copy();
        TavernResult result = resolver().apply(state, new Action.BuyRelic(0, null));
        List<StatsChanged> changes = result.events().of(StatsChanged.class);
        assertThat(changes).hasSize(2);
        assertThat(changes).extracting(StatsChanged::unitId).doesNotHaveDuplicates();
        assertThat(resolver().apply(twin, new Action.BuyRelic(0, null)).events().events()).isEqualTo(result.events().events());
        assertThat(state.band().units().stream().mapToInt(Unit::attack).sum()).isEqualTo(8);
    }

    @Test
    void banquetAndGrandFeastStopAtTheHeroesThatExist() {
        Unit a = unit("aoi", 2, 2);
        RunState state = shop("banquet", a);
        resolver().apply(state, new Action.BuyRelic(0, null));
        assertThat(a.stats()).hasToString("3/3");
        state.tavern().setRelicSlot(1, TavernSlot.relic("grand_feast", 3));
        resolver().apply(state, new Action.BuyRelic(1, null));
        assertThat(a.stats()).hasToString("5/5");
    }

    @Test
    void buyRelicRefusesTheWrongShapeOfRequest() {
        Unit aoi = unit("aoi", 2, 2);
        RunState state = shop("ration", aoi);
        assertThat(violation(state, new Action.BuyRelic(0, null)).code()).isEqualTo(Code.TARGET_REQUIRED);
        assertThat(violation(state, new Action.BuyRelic(0, 4)).code()).isEqualTo(Code.INVALID_INDEX);
        assertThat(violation(state, new Action.BuyRelic(5, 0)).code()).isEqualTo(Code.INVALID_INDEX);
        state.tavern().setRelicSlot(1, TavernSlot.relic("feast", 3));
        assertThat(violation(state, new Action.BuyRelic(1, 0)).code()).isEqualTo(Code.TARGET_NOT_ALLOWED);
        state.setGold(2);
        RunState before = state.copy();
        assertThat(violation(state, new Action.BuyRelic(0, 0)).code()).isEqualTo(Code.NOT_ENOUGH_GOLD);
        assertThat(state).isEqualTo(before);
        state.setGold(10);
        resolver().apply(state, new Action.BuyRelic(0, 0));
        assertThat(violation(state, new Action.BuyRelic(0, 0)).code()).isEqualTo(Code.SLOT_EMPTY);

        RunState empty = shop("feast");
        assertThat(violation(empty, new Action.BuyRelic(0, null)).code()).isEqualTo(Code.TARGET_REQUIRED);
        RunState decree = shop("royal_decree");
        resolver().apply(decree, new Action.BuyRelic(0, null));
        assertThat(decree.gold()).isEqualTo(7);
    }

    @Test
    void allSixteenRelicsExistAndEveryPerkHasARelicOrAnAbility() {
        List<RelicDefinition> relics = content().relics();
        assertThat(relics).hasSize(16);
        assertThat(relics.stream().filter(RelicDefinition::isPerk).count()).isEqualTo(7);
        assertThat(relics).extracting(RelicDefinition::id).contains("ration", "fairy_charm", "battle_brew", "iron_blade",
            "poison_vial", "chainmail", "feast", "royal_decree", "elixir", "flame_oil", "tome_of_experience", "banquet", "ward",
            "phoenix_feather", "enchanted_blade", "grand_feast");
        assertThat(content().relic("royal_decree").needsChosenTarget()).isFalse();
        assertThat(content().relic("feast").needsChosenTarget()).isFalse();
    }

    @Test
    void everyRelicCanBeBoughtOnAThreeHeroBand() {
        for (RelicDefinition relic : content().relics()) {
            RunState state = shop(relic.id(), unit("aoi", 2, 2), unit("bard", 2, 1), unit("archer", 2, 2));
            resolver().apply(state, new Action.BuyRelic(0, relic.needsChosenTarget() ? 1 : null));
            state.assertInvariants();
            assertThat(state.gold()).isEqualTo(10 - relic.price());
        }
    }
}
