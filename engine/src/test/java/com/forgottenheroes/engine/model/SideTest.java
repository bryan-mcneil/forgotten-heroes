package com.forgottenheroes.engine.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SideTest {

    @Test
    void oppositeFlips() {
        assertThat(Side.HOME.opposite()).isEqualTo(Side.AWAY);
        assertThat(Side.AWAY.opposite()).isEqualTo(Side.HOME);
    }
}
