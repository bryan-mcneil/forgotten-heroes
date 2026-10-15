package com.forgottenheroes.engine.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UnitIdTest {

    @Test
    void sequenceCountsUp() {
        UnitId.Sequence ids = new UnitId.Sequence();
        assertThat(ids.next()).isEqualTo(new UnitId("u1"));
        assertThat(ids.next()).isEqualTo(new UnitId("u2"));
    }

    @Test
    void twoSequencesDoNotShareACount() {
        UnitId.Sequence first = new UnitId.Sequence();
        UnitId.Sequence second = new UnitId.Sequence();
        first.next();
        assertThat(second.next()).isEqualTo(new UnitId("u1"));
    }

    @Test
    void blankIdsAreRejected() {
        assertThatThrownBy(() -> new UnitId(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UnitId(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
