package com.forgottenheroes.engine.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StatsTest {

    @Test
    void plusAddsBothNumbers() {
        assertThat(new Stats(2, 3).plus(new Stats(1, 1))).isEqualTo(new Stats(3, 4));
    }

    @Test
    void withAttackChangesOnlyAttack() {
        assertThat(new Stats(2, 3).withAttack(5)).isEqualTo(new Stats(5, 3));
    }

    @Test
    void withHealthChangesOnlyHealth() {
        assertThat(new Stats(2, 3).withHealth(5)).isEqualTo(new Stats(2, 5));
    }

    @Test
    void clampCapsAtMaxStat() {
        assertThat(new Stats(60, 70).clamp()).isEqualTo(new Stats(50, 50));
        assertThat(new Stats(2, 3).clamp()).isEqualTo(new Stats(2, 3));
    }

    @Test
    void negativeStatsAreRejected() {
        assertThatThrownBy(() -> new Stats(-1, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Stats(3, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void originalIsUntouchedByPlus() {
        Stats original = new Stats(2, 3);
        original.plus(new Stats(1, 1));
        assertThat(original).isEqualTo(new Stats(2, 3));
    }

    @Test
    void printsAsAttackSlashHealth() {
        assertThat(new Stats(2, 3)).hasToString("2/3");
    }
}
