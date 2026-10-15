package com.forgottenheroes.engine.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PositionTest {

    @Test
    void indexZeroIsTheFront() {
        assertThat(new Position(Side.HOME, 0).isFront()).isTrue();
        assertThat(new Position(Side.HOME, 1).isFront()).isFalse();
    }

    @Test
    void indexMustFitTheBand() {
        assertThatThrownBy(() -> new Position(Side.HOME, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Position(Side.AWAY, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void printsLikeAScenarioFile() {
        assertThat(new Position(Side.AWAY, 2)).hasToString("away:2");
    }
}
