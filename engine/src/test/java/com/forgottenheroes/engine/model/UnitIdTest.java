package com.forgottenheroes.engine.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UnitIdTest {

    @Test
    void equalValuesAreEqualIds() {
        assertThat(new UnitId("u1")).isEqualTo(new UnitId("u1"));
        assertThat(new UnitId("u1")).isNotEqualTo(new UnitId("u2"));
    }

    @Test
    void blankIdsAreRejected() {
        assertThatThrownBy(() -> new UnitId(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UnitId(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
