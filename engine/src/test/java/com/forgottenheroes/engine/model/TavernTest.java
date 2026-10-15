package com.forgottenheroes.engine.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TavernTest {

    @ParameterizedTest
    @CsvSource({
        "1, 3, 1",
        "2, 3, 1",
        "3, 3, 2",
        "4, 3, 2",
        "5, 4, 2",
        "8, 4, 2",
        "9, 5, 2",
        "12, 5, 2"
    })
    void slotCountsFollowTheTurnTable(int turn, int heroSlots, int relicSlots) {
        assertThat(Tavern.heroSlotsFor(turn)).isEqualTo(heroSlots);
        assertThat(Tavern.relicSlotsFor(turn)).isEqualTo(relicSlots);
    }

    @Test
    void turnZeroHasNoSlotCount() {
        assertThatThrownBy(() -> Tavern.heroSlotsFor(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Tavern.relicSlotsFor(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resizeKeepsSurvivingSlotsAndAddsEmptyOnes() {
        Tavern tavern = new Tavern();
        tavern.resizeHeroSlots(3);
        tavern.setHeroSlot(0, TavernSlot.hero("aoi", Stats.ZERO).withFrozen(true));
        tavern.resizeHeroSlots(4);
        assertThat(tavern.heroSlots()).hasSize(4);
        assertThat(tavern.heroSlot(0).frozen()).isTrue();
        assertThat(tavern.heroSlot(3)).isEqualTo(TavernSlot.EMPTY);
        tavern.resizeHeroSlots(1);
        assertThat(tavern.heroSlots()).hasSize(1);
        assertThatThrownBy(() -> tavern.resizeRelicSlots(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theBonusSlotSitsAfterTheRegularOnes() {
        Tavern tavern = new Tavern();
        tavern.resizeHeroSlots(3);
        assertThat(tavern.heroSlotCount()).isEqualTo(3);
        assertThatThrownBy(() -> tavern.heroSlot(3)).isInstanceOf(IndexOutOfBoundsException.class);
        tavern.openBonusSlot(TavernSlot.hero("shield_maiden", Stats.ZERO));
        assertThat(tavern.heroSlotCount()).isEqualTo(4);
        assertThat(tavern.isBonusSlotIndex(3)).isTrue();
        assertThat(tavern.heroSlot(3).itemId()).isEqualTo("shield_maiden");
        tavern.setHeroSlot(3, TavernSlot.EMPTY);
        assertThat(tavern.bonusSlot()).isEmpty();
        assertThat(tavern.heroSlotCount()).isEqualTo(3);
        assertThatThrownBy(() -> tavern.openBonusSlot(TavernSlot.EMPTY)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void relicSlotsCanGrowForAStockedRelic() {
        Tavern tavern = new Tavern();
        tavern.resizeRelicSlots(1);
        tavern.setRelicSlot(0, TavernSlot.relic("ration", 3));
        tavern.addRelicSlot(TavernSlot.relic("ration", 2));
        assertThat(tavern.relicSlots()).hasSize(2);
        assertThat(tavern.relicSlot(1).price()).isEqualTo(2);
        assertThatThrownBy(() -> tavern.relicSlot(2)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void permanentBuffAccumulates() {
        Tavern tavern = new Tavern();
        tavern.addPermanentBuff(new Stats(1, 1));
        tavern.addPermanentBuff(new Stats(1, 0));
        assertThat(tavern.permanentBuff()).isEqualTo(new Stats(2, 1));
    }

    @Test
    void copyIsEqualAndIndependent() {
        Tavern tavern = new Tavern();
        tavern.resizeHeroSlots(3);
        tavern.resizeRelicSlots(1);
        tavern.setHeroSlot(1, TavernSlot.hero("bard", new Stats(1, 1)));
        tavern.openBonusSlot(TavernSlot.hero("aoi", Stats.ZERO));
        Tavern copy = tavern.copy();
        assertThat(copy).isEqualTo(tavern).hasSameHashCodeAs(tavern);
        copy.setHeroSlot(1, TavernSlot.EMPTY);
        assertThat(copy).isNotEqualTo(tavern);
        assertThat(tavern.toString()).contains("bard@3+1/1").contains("bonus aoi@3");
    }

    @Test
    void slotsValidateThemselves() {
        assertThat(TavernSlot.EMPTY.isEmpty()).isTrue();
        assertThat(TavernSlot.EMPTY).hasToString("-");
        assertThat(TavernSlot.hero("aoi", Stats.ZERO).withFrozen(true)).hasToString("aoi@3 (frozen)");
        assertThat(TavernSlot.relic("ration", 3).withPrice(0).price()).isZero();
        assertThat(TavernSlot.hero("aoi", Stats.ZERO).withBonus(new Stats(1, 1)).bonus()).isEqualTo(new Stats(1, 1));
        assertThatThrownBy(() -> TavernSlot.EMPTY.withFrozen(true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TavernSlot(" ", 3, Stats.ZERO, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TavernSlot("aoi", -1, Stats.ZERO, false)).isInstanceOf(IllegalArgumentException.class);
    }
}
