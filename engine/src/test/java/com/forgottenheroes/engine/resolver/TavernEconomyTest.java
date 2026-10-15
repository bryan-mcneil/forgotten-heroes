package com.forgottenheroes.engine.resolver;

import static com.forgottenheroes.engine.testing.TestFixtures.content;
import static com.forgottenheroes.engine.testing.TestFixtures.resolver;
import static com.forgottenheroes.engine.testing.TestFixtures.runAtTurn;
import static com.forgottenheroes.engine.testing.TestFixtures.unit;
import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.event.GameEvent.BonusSlotOpened;
import com.forgottenheroes.engine.event.GameEvent.GoldChanged;
import com.forgottenheroes.engine.event.GameEvent.HeroBought;
import com.forgottenheroes.engine.event.GameEvent.HeroSold;
import com.forgottenheroes.engine.event.GameEvent.HeroesMerged;
import com.forgottenheroes.engine.event.GameEvent.HeroesReordered;
import com.forgottenheroes.engine.event.GameEvent.LevelUp;
import com.forgottenheroes.engine.event.GameEvent.MergeSource;
import com.forgottenheroes.engine.event.GameEvent.XpGained;
import com.forgottenheroes.engine.model.ItemKind;
import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.Rank;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.TavernSlot;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.resolver.RuleViolation.Code;
import com.forgottenheroes.engine.rng.GameRandom;
import java.util.List;
import org.junit.jupiter.api.Test;

class TavernEconomyTest {

    private static RuleViolation violation(RunState state, Action action) {
        try {
            resolver().apply(state, action);
        } catch (RuleViolation e) {
            return e;
        }
        throw new AssertionError("expected a RuleViolation for " + action);
    }

    /** Turn 1 with the first shelf holding the given hero, so a test controls what it buys. */
    private static RunState shop(String heroId) {
        RunState state = runAtTurn(14, 1);
        state.tavern().setHeroSlot(0, TavernSlot.hero(heroId, Stats.ZERO));
        return state;
    }

    private static void buy(RunState state, String heroId, int bandIndex) {
        state.tavern().setHeroSlot(0, TavernSlot.hero(heroId, Stats.ZERO));
        state.setGold(10);
        resolver().apply(state, new Action.Buy(0, bandIndex));
    }

    @Test
    void buyingIntoAnEmptyBandCostsThreeGoldAndTellsItsStory() {
        RunState state = shop("merchant");
        TavernResult result = resolver().apply(state, new Action.Buy(0, 0));
        assertThat(state.gold()).isEqualTo(7);
        assertThat(state.band().size()).isEqualTo(1);
        Unit merchant = state.band().front();
        assertThat(merchant.heroId()).isEqualTo("merchant");
        assertThat(merchant.stats()).hasToString("4/1");
        assertThat(state.tavern().heroSlot(0)).isEqualTo(TavernSlot.EMPTY);
        assertThat(result.events().events()).extracting(GameEvent::getClass).containsExactly(GoldChanged.class, HeroBought.class);
        assertThat(result.events().of(HeroBought.class).getFirst())
            .isEqualTo(new HeroBought(0, merchant.id(), "merchant", 0, 0, new Stats(4, 1), 3).withSeq(result.events().lastSeq()));
        assertThat(result.triggers()).containsExactly(
            PendingTrigger.of(Trigger.BUY, merchant.id()), PendingTrigger.of(Trigger.ALLY_SUMMONED, merchant.id()));
        state.assertInvariants();
    }

    @Test
    void theBandIndexPlacesTheNewHero() {
        RunState state = shop("merchant");
        resolver().apply(state, new Action.Buy(0, 0));
        buy(state, "archer", 0);
        buy(state, "bard", 2);
        assertThat(state.band().units()).extracting(Unit::heroId).containsExactly("archer", "merchant", "bard");
        assertThat(state.band().units()).extracting(unit -> unit.id().value()).containsExactly("u2", "u1", "u3");
    }

    @Test
    void aBuyWithTheShelfBuffCarriesItOntoTheBand() {
        RunState state = runAtTurn(14, 1);
        state.tavern().setHeroSlot(0, TavernSlot.hero("aoi", new Stats(1, 1)));
        resolver().apply(state, new Action.Buy(0, 0));
        assertThat(state.band().front().stats()).hasToString("3/3");
        assertThat(state.band().front().permanentBonus()).isEqualTo(new Stats(1, 1));
    }

    @Test
    void buyingIsRefusedWhenPoorFullOrOffTheBand() {
        RunState state = shop("merchant");
        state.setGold(2);
        RunState before = state.copy();
        assertThat(violation(state, new Action.Buy(0, 0)).code()).isEqualTo(Code.NOT_ENOUGH_GOLD);
        assertThat(state).isEqualTo(before);

        state.setGold(10);
        assertThat(violation(state, new Action.Buy(0, 1)).code()).isEqualTo(Code.INVALID_INDEX);
        state.tavern().setHeroSlot(2, TavernSlot.EMPTY);
        assertThat(violation(state, new Action.Buy(2, 0)).code()).isEqualTo(Code.SLOT_EMPTY);
        for (String hero : List.of("aoi", "bard", "archer", "hotaru", "farmhand")) {
            buy(state, hero, state.band().size());
        }
        assertThat(state.band().isFull()).isTrue();
        state.tavern().setHeroSlot(0, TavernSlot.hero("merchant", Stats.ZERO));
        state.setGold(10);
        assertThat(violation(state, new Action.Buy(0, 5)).code()).isEqualTo(Code.BAND_FULL);
        assertThat(violation(state, new Action.Buy(0, 9)).code()).isEqualTo(Code.INVALID_INDEX);
    }

    @Test
    void buyingOntoTheSameHeroMerges() {
        RunState state = shop("aoi");
        resolver().apply(state, new Action.Buy(0, 0));
        Unit aoi = state.band().front();
        state.tavern().setHeroSlot(1, TavernSlot.hero("aoi", Stats.ZERO));
        TavernResult result = resolver().apply(state, new Action.Buy(1, 0));
        assertThat(state.band().size()).isEqualTo(1);
        assertThat(state.gold()).isEqualTo(4);
        assertThat(aoi.stats()).hasToString("3/3");
        assertThat(aoi.xp().points()).isEqualTo(1);
        assertThat(aoi.level()).isEqualTo(1);
        assertThat(result.events().of(HeroesMerged.class).getFirst().source()).isEqualTo(MergeSource.TAVERN);
        assertThat(result.events().of(HeroesMerged.class).getFirst().removedUnitId()).isNull();
        assertThat(result.events().of(HeroesMerged.class).getFirst().before()).isEqualTo(new Stats(2, 2));
        assertThat(result.events().of(HeroesMerged.class).getFirst().after()).isEqualTo(new Stats(3, 3));
        assertThat(result.events().of(XpGained.class).getFirst().after()).isEqualTo(1);
        assertThat(result.events().of(LevelUp.class)).isEmpty();
        assertThat(result.triggers()).containsExactly(PendingTrigger.of(Trigger.BUY, aoi.id()));
    }

    @Test
    void threeCopiesReachLevelTwoAndOpenTheBonusSlot() {
        RunState state = shop("aoi");
        resolver().apply(state, new Action.Buy(0, 0));
        buy(state, "aoi", 0);
        TavernResult result = resolver().apply(stateWithAoiOnShelf(state), new Action.Buy(0, 0));
        Unit aoi = state.band().front();
        assertThat(aoi.level()).isEqualTo(2);
        assertThat(aoi.xp().points()).isEqualTo(2);
        assertThat(aoi.stats()).hasToString("4/4");
        assertThat(result.events().of(LevelUp.class).getFirst().level()).isEqualTo(2);
        BonusSlotOpened opened = result.events().of(BonusSlotOpened.class).getFirst();
        assertThat(opened.slot()).isEqualTo(3);
        assertThat(state.tavern().bonusSlot()).contains(opened.item());
        assertThat(content().hero(opened.item().itemId()).rank()).isLessThanOrEqualTo(Rank.II);
        assertThat(result.triggers()).containsExactly(
            PendingTrigger.of(Trigger.LEVEL_UP, aoi.id()), PendingTrigger.of(Trigger.BUY, aoi.id()));
        state.assertInvariants();
    }

    private static RunState stateWithAoiOnShelf(RunState state) {
        state.tavern().setHeroSlot(0, TavernSlot.hero("aoi", Stats.ZERO));
        state.setGold(10);
        return state;
    }

    @Test
    void sixCopiesReachLevelThreeAndASeventhIsRefused() {
        RunState state = shop("aoi");
        resolver().apply(state, new Action.Buy(0, 0));
        for (int copies = 2; copies <= 6; copies++) {
            buy(state, "aoi", 0);
        }
        Unit aoi = state.band().front();
        assertThat(aoi.level()).isEqualTo(3);
        assertThat(aoi.xp().points()).isEqualTo(5);
        assertThat(aoi.stats()).hasToString("7/7");
        state.tavern().setHeroSlot(0, TavernSlot.hero("aoi", Stats.ZERO));
        state.setGold(10);
        assertThat(violation(state, new Action.Buy(0, 0)).code()).isEqualTo(Code.MAX_LEVEL);
    }

    @Test
    void mergingTwoLevelTwoHeroesOnTheBandMakesLevelThree() {
        RunState state = shop("aoi");
        resolver().apply(state, new Action.Buy(0, 0));
        buy(state, "aoi", 0);
        buy(state, "aoi", 0);
        buy(state, "aoi", 1);
        buy(state, "aoi", 1);
        buy(state, "aoi", 1);
        assertThat(state.band().units()).extracting(Unit::level).containsExactly(2, 2);
        Unit survivor = state.band().at(1);
        Unit copy = state.band().at(0);
        copy.setPerk(Perk.IRON_BLADE);
        copy.addTemporaryBonus(new Stats(0, 3));
        TavernResult result = resolver().apply(state, new Action.Merge(0, 1));
        assertThat(state.band().units()).containsExactly(survivor);
        assertThat(survivor.level()).isEqualTo(3);
        assertThat(survivor.xp().points()).isEqualTo(5);
        assertThat(survivor.stats()).hasToString("7/10");
        assertThat(survivor.temporaryBonus()).isEqualTo(new Stats(0, 3));
        assertThat(survivor.perk()).contains(Perk.IRON_BLADE);
        HeroesMerged merged = result.events().of(HeroesMerged.class).getFirst();
        assertThat(merged.source()).isEqualTo(MergeSource.BAND);
        assertThat(merged.removedUnitId()).isEqualTo(copy.id());
        assertThat(result.events().of(XpGained.class).getFirst()).isEqualTo(new XpGained(0, survivor.id(), "aoi", 2, 5).withSeq(result.events().of(XpGained.class).getFirst().seq()));
        assertThat(result.triggers()).containsExactly(PendingTrigger.of(Trigger.LEVEL_UP, survivor.id()));
        assertThat(state.tavern().bonusSlot()).isPresent();
        assertThat(state.gold()).isEqualTo(7);
    }

    @Test
    void mergeIsRefusedForDifferentHeroesOrAMaxedSurvivor() {
        RunState state = shop("aoi");
        resolver().apply(state, new Action.Buy(0, 0));
        buy(state, "bard", 1);
        assertThat(violation(state, new Action.Merge(1, 0)).code()).isEqualTo(Code.NOT_SAME_HERO);
        assertThat(violation(state, new Action.Merge(0, 5)).code()).isEqualTo(Code.INVALID_INDEX);
        Unit maxed = unit("bard", 2, 1, 3);
        state.band().add(maxed);
        assertThat(violation(state, new Action.Merge(1, 2)).code()).isEqualTo(Code.MAX_LEVEL);
    }

    @Test
    void sellingPaysTheLevelAndRemovesTheHero() {
        RunState state = runAtTurn(14, 1);
        Unit one = unit("merchant", 4, 1, 1);
        Unit two = unit("aoi", 2, 2, 2);
        Unit three = unit("bard", 2, 1, 3);
        state.band().add(one);
        state.band().add(two);
        state.band().add(three);
        state.setGold(0);
        TavernResult result = resolver().apply(state, new Action.Sell(1));
        assertThat(state.gold()).isEqualTo(2);
        assertThat(state.band().units()).containsExactly(one, three);
        assertThat(result.events().events()).extracting(GameEvent::getClass).containsExactly(HeroSold.class, GoldChanged.class);
        assertThat(result.events().of(HeroSold.class).getFirst().gold()).isEqualTo(2);
        assertThat(result.triggers()).containsExactly(PendingTrigger.of(Trigger.SELL, two.id()));
        resolver().apply(state, new Action.Sell(1));
        resolver().apply(state, new Action.Sell(0));
        assertThat(state.gold()).isEqualTo(6);
        assertThat(state.band().isEmpty()).isTrue();
        assertThat(violation(state, new Action.Sell(0)).code()).isEqualTo(Code.INVALID_INDEX);
    }

    @Test
    void moveFourToZeroPutsTheHeroInFront() {
        RunState state = runAtTurn(14, 1);
        for (String hero : List.of("aoi", "bard", "archer", "hotaru", "farmhand")) {
            state.band().add(unit(hero, 1, 1));
        }
        TavernResult result = resolver().apply(state, new Action.Move(4, 0));
        assertThat(state.band().units()).extracting(Unit::heroId).containsExactly("farmhand", "aoi", "bard", "archer", "hotaru");
        HeroesReordered reordered = result.events().of(HeroesReordered.class).getFirst();
        assertThat(reordered.from()).isEqualTo(4);
        assertThat(reordered.order()).isEqualTo(state.band().units().stream().map(Unit::id).toList());
        assertThat(violation(state, new Action.Move(2, 2)).code()).isEqualTo(Code.INVALID_INDEX);
        assertThat(violation(state, new Action.Move(0, 5)).code()).isEqualTo(Code.INVALID_INDEX);
    }

    @Test
    void theBonusSlotCanBeBoughtAndThenCloses() {
        RunState state = shop("aoi");
        resolver().apply(state, new Action.Buy(0, 0));
        buy(state, "aoi", 0);
        buy(state, "aoi", 0);
        assertThat(state.tavern().bonusSlot()).isPresent();
        String bonusHero = state.tavern().heroSlot(3).itemId();
        state.setGold(10);
        resolver().apply(state, new Action.Buy(3, 1));
        assertThat(state.band().at(1).heroId()).isEqualTo(bonusHero);
        assertThat(state.tavern().bonusSlot()).isEmpty();
        assertThat(state.tavern().heroSlotCount()).isEqualTo(3);
    }

    @Test
    void goldNeverGoesNegativeAndInvariantsHoldUnderRandomLegalPlay() {
        RunState state = runAtTurn(99, 1);
        GameRandom dice = new GameRandom(99);
        int accepted = 0;
        int rejected = 0;
        for (int i = 0; i < 400; i++) {
            Action action = switch (dice.nextInt(7)) {
                case 0 -> new Action.Buy(dice.nextInt(4), dice.nextInt(6));
                case 1 -> new Action.Sell(dice.nextInt(5));
                case 2 -> new Action.Move(dice.nextInt(5), dice.nextInt(5));
                case 3 -> {
                    int from = dice.nextInt(5);
                    yield new Action.Merge(from, (from + 1 + dice.nextInt(4)) % 5);
                }
                case 4 -> new Action.Roll();
                case 5 -> new Action.Freeze(ItemKind.HERO, dice.nextInt(4));
                default -> new Action.StartTurn();
            };
            try {
                resolver().apply(state, action);
                accepted++;
            } catch (RuleViolation e) {
                rejected++;
            }
            assertThat(state.gold()).isBetween(0, 50);
            state.assertInvariants();
        }
        assertThat(accepted).isGreaterThan(50);
        assertThat(rejected).isGreaterThan(50);
    }
}
