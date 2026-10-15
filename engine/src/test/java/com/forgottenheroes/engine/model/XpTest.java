package com.forgottenheroes.engine.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class XpTest {

    @ParameterizedTest
    @CsvSource({
        "0, 1",
        "1, 1",
        "2, 2",
        "3, 2",
        "4, 2",
        "5, 3"
    })
    void levelFollowsTheXpTable(int points, int expectedLevel) {
        assertThat(new Xp(points).level()).isEqualTo(expectedLevel);
    }

    @Test
    void plusAddsPoints() {
        assertThat(Xp.ZERO.plus(2)).isEqualTo(new Xp(2));
    }

    @Test
    void plusNeverPassesMax() {
        assertThat(new Xp(4).plus(3)).isEqualTo(new Xp(5));
    }

    @Test
    void isMaxOnlyAtFive() {
        assertThat(new Xp(5).isMax()).isTrue();
        assertThat(new Xp(4).isMax()).isFalse();
    }

    @Test
    void outOfRangeIsRejected() {
        assertThatThrownBy(() -> new Xp(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Xp(6)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Xp.ZERO.plus(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
