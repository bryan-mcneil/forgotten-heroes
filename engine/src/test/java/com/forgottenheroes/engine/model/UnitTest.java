package com.forgottenheroes.engine.model;

import static com.forgottenheroes.engine.testing.TestFixtures.content;
import static com.forgottenheroes.engine.testing.TestFixtures.unit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UnitTest {

    @Test
    void statsAreDerivedFromTheThreePartsMinusDamage() {
        Unit unit = unit("aoi", 2, 2);
        unit.addPermanentBonus(new Stats(1, 0));
        unit.addTemporaryBonus(new Stats(0, 3));
        unit.takeDamage(2);
        assertThat(unit.attack()).isEqualTo(3);
        assertThat(unit.maxHealth()).isEqualTo(5);
        assertThat(unit.health()).isEqualTo(3);
        assertThat(unit.stats()).isEqualTo(new Stats(3, 3));
        assertThat(unit.statsBeforeDamage()).isEqualTo(new Stats(3, 5));
    }

    @Test
    void statsAreCappedAtFifty() {
        Unit unit = unit("aoi", 45, 45);
        unit.addPermanentBonus(new Stats(10, 2));
        unit.addTemporaryBonus(new Stats(0, 10));
        assertThat(unit.attack()).isEqualTo(Rules.MAX_STAT);
        assertThat(unit.maxHealth()).isEqualTo(Rules.MAX_STAT);
    }

    @Test
    void aUnitFallsAtZeroHealthAndCardsNeverShowNegativeHealth() {
        Unit unit = unit("aoi", 2, 2);
        assertThat(unit.isAlive()).isTrue();
        unit.takeDamage(5);
        assertThat(unit.isAlive()).isFalse();
        assertThat(unit.health()).isEqualTo(-3);
        assertThat(unit.stats()).isEqualTo(new Stats(2, 0));
        unit.heal();
        assertThat(unit.isAlive()).isTrue();
    }

    @Test
    void clearTemporaryKeepsPermanentBonuses() {
        Unit unit = unit("aoi", 2, 2);
        unit.addPermanentBonus(new Stats(1, 1));
        unit.addTemporaryBonus(new Stats(3, 3));
        unit.clearTemporary();
        assertThat(unit.stats()).isEqualTo(new Stats(3, 3));
    }

    @Test
    void xpDrivesTheLevel() {
        Unit unit = unit("aoi", 2, 2);
        assertThat(unit.level()).isEqualTo(1);
        assertThat(unit.gainXp(1)).isEqualTo(1);
        assertThat(unit.level()).isEqualTo(1);
        assertThat(unit.gainXp(1)).isEqualTo(1);
        assertThat(unit.level()).isEqualTo(2);
        assertThat(unit.gainXp(10)).isEqualTo(3);
        assertThat(unit.level()).isEqualTo(3);
        assertThat(unit.gainXp(1)).isZero();
    }

    @Test
    void aUnitHoldsOnePerkAtATime() {
        Unit unit = unit("aoi", 2, 2);
        assertThat(unit.perk()).isEmpty();
        unit.setPerk(Perk.IRON_BLADE);
        unit.setPerk(Perk.WARD);
        assertThat(unit.perk()).contains(Perk.WARD);
        assertThat(unit.hasPerk(Perk.WARD)).isTrue();
        assertThat(unit.hasPerk(Perk.IRON_BLADE)).isFalse();
        unit.removePerk();
        assertThat(unit.perk()).isEmpty();
    }

    @Test
    void mergeStatsSetThePermanentBonusAboveBase() {
        Unit unit = unit("aoi", 2, 2);
        unit.setPermanentStats(new Stats(5, 3));
        assertThat(unit.permanentBonus()).isEqualTo(new Stats(3, 1));
        assertThatThrownBy(() -> unit.setPermanentStats(new Stats(1, 1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resetStatsReplacesEverything() {
        Unit unit = unit("aoi", 2, 2);
        unit.addPermanentBonus(new Stats(4, 4));
        unit.takeDamage(3);
        unit.resetStats(new Stats(1, 1));
        assertThat(unit.stats()).isEqualTo(new Stats(1, 1));
        assertThat(unit.permanentBonus()).isEqualTo(Stats.ZERO);
    }

    @Test
    void fromDefinitionUsesTheRosterStats() {
        Unit merchant = Unit.fromDefinition(content().hero("merchant"), new UnitId("u1"));
        assertThat(merchant.heroId()).isEqualTo("merchant");
        assertThat(merchant.stats()).hasToString("4/1");
        Unit scarecrow = Unit.fromToken(content().token("scarecrow"), new UnitId("u2"), new Stats(2, 2));
        assertThat(scarecrow.stats()).hasToString("2/2");
    }

    @Test
    void copyForBattleResetsDamageAndBattleCounterOnly() {
        Unit unit = unit("aoi", 2, 2);
        unit.addTemporaryBonus(new Stats(3, 3));
        unit.takeDamage(1);
        unit.counters().recordUse();
        Unit copy = unit.copyForBattle();
        assertThat(copy.id()).isEqualTo(unit.id());
        assertThat(copy.damageTaken()).isZero();
        assertThat(copy.temporaryBonus()).isEqualTo(new Stats(3, 3));
        assertThat(copy.counters().usesThisBattle()).isZero();
        assertThat(copy.counters().usesThisTurn()).isEqualTo(1);
        copy.takeDamage(4);
        assertThat(unit.damageTaken()).isEqualTo(1);
    }

    @Test
    void copyIsEqualButIndependent() {
        Unit unit = unit("aoi", 2, 2, Perk.CHAINMAIL);
        Unit copy = unit.copy();
        assertThat(copy).isEqualTo(unit).hasSameHashCodeAs(unit);
        copy.addPermanentBonus(new Stats(1, 1));
        assertThat(copy).isNotEqualTo(unit);
        assertThat(unit).hasToString(unit.heroId() + "#" + unit.id().value() + " 2/2 L1 CHAINMAIL");
    }

    @Test
    void negativeDamageIsRejected() {
        assertThatThrownBy(() -> unit("aoi", 2, 2).takeDamage(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void countersTrackTurnAndBattleSeparately() {
        Counters counters = new Counters();
        counters.recordUse();
        counters.recordUse();
        counters.resetBattle();
        assertThat(counters.usesThisTurn()).isEqualTo(2);
        assertThat(counters.usesThisBattle()).isZero();
        counters.resetTurn();
        assertThat(counters).isEqualTo(new Counters()).hasToString("turn 0, battle 0");
    }
}
