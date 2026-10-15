package com.forgottenheroes.engine.model;

import static com.forgottenheroes.engine.testing.TestFixtures.newRun;
import static com.forgottenheroes.engine.testing.TestFixtures.unit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RunStateTest {

    @Test
    void aNewRunStartsBeforeTurnOneWithFiveHearts() {
        RunState state = newRun(42);
        assertThat(state.runId()).isEqualTo("run-42");
        assertThat(state.runSeed()).isEqualTo(42L);
        assertThat(state.turn()).isZero();
        assertThat(state.gold()).isZero();
        assertThat(state.hearts()).isEqualTo(Rules.START_HEARTS);
        assertThat(state.crowns()).isZero();
        assertThat(state.status()).isEqualTo(RunStatus.IN_PROGRESS);
        assertThat(state.band().isEmpty()).isTrue();
        assertThat(state.version()).isZero();
        assertThat(state.lastBattle()).isNull();
        state.assertInvariants();
    }

    @Test
    void unitIdsCountUpAndSurviveACopy() {
        RunState state = newRun(1);
        assertThat(state.nextUnitId()).isEqualTo(new UnitId("u1"));
        assertThat(state.nextUnitId()).isEqualTo(new UnitId("u2"));
        assertThat(state.copy().nextUnitId()).isEqualTo(new UnitId("u3"));
    }

    @Test
    void settersRefuseValuesOutsideTheRules() {
        RunState state = newRun(1);
        assertThatThrownBy(() -> state.setGold(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> state.setGold(Rules.MAX_GOLD + 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> state.setHearts(6)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> state.setCrowns(11)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> state.setTurn(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> state.setRollsThisTurn(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> state.setLastEventSeq(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RunState.newRun(" ", 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sixUnitsBreakTheInvariants() {
        RunState state = newRun(1);
        for (int i = 0; i < 5; i++) {
            state.band().add(unit("a", 1, 1));
        }
        state.assertInvariants();
        assertThatThrownBy(() -> state.band().add(unit("a", 1, 1))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aFallenUnitOnTheBandBreaksTheInvariants() {
        RunState state = newRun(1);
        Unit unit = unit("a", 1, 1);
        state.band().add(unit);
        unit.takeDamage(1);
        assertThatThrownBy(state::assertInvariants).isInstanceOf(IllegalStateException.class).hasMessageContaining("fallen");
    }

    @Test
    void slotCountsMustMatchTheTurnOnceItStarted() {
        RunState state = newRun(1);
        state.setTurn(5);
        state.tavern().resizeHeroSlots(3);
        state.tavern().resizeRelicSlots(2);
        assertThatThrownBy(state::assertInvariants).isInstanceOf(IllegalStateException.class).hasMessageContaining("hero slots");
        state.tavern().resizeHeroSlots(4);
        state.assertInvariants();
        state.tavern().resizeRelicSlots(1);
        assertThatThrownBy(state::assertInvariants).isInstanceOf(IllegalStateException.class).hasMessageContaining("relic slots");
    }

    @Test
    void aSecondUnitWithTheSameIdIsRefusedByTheBand() {
        RunState state = newRun(1);
        Unit a = unit("a", 1, 1);
        state.band().add(a);
        Unit impostor = new Unit(new UnitId(a.id().value()), "b", new Stats(1, 1));
        assertThatThrownBy(() -> state.band().add(impostor)).isInstanceOf(IllegalArgumentException.class);
        state.assertInvariants();
    }

    @Test
    void copyIsEqualAndIndependent() {
        RunState state = newRun(7);
        state.setTurn(1);
        state.setGold(10);
        state.tavern().resizeHeroSlots(3);
        state.tavern().resizeRelicSlots(1);
        state.band().add(unit("a", 1, 1));
        state.setLastBattle(new BattleSummary(1, Outcome.WIN, 3));
        state.bumpVersion();
        state.setLastEventSeq(12);
        RunState copy = state.copy();
        assertThat(copy).isEqualTo(state).hasSameHashCodeAs(state);
        copy.setGold(3);
        assertThat(copy).isNotEqualTo(state);
        assertThat(state.gold()).isEqualTo(10);
        assertThat(state.version()).isEqualTo(1);
        assertThat(state.toString()).startsWith("run run-7 turn 1 gold 10 hearts 5 crowns 0 IN_PROGRESS");
    }

    @Test
    void outcomesFlipAndStatusesKnowWhenTheRunIsOver() {
        assertThat(Outcome.WIN.flipped()).isEqualTo(Outcome.LOSS);
        assertThat(Outcome.LOSS.flipped()).isEqualTo(Outcome.WIN);
        assertThat(Outcome.DRAW.flipped()).isEqualTo(Outcome.DRAW);
        assertThat(RunStatus.IN_PROGRESS.isOver()).isFalse();
        assertThat(RunStatus.WON.isOver()).isTrue();
        assertThatThrownBy(() -> new BattleSummary(0, Outcome.WIN, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BattleSummary(1, null, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BattleSummary(1, Outcome.WIN, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
