package com.forgottenheroes.engine.rng;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class Hash64Test {

    @Test
    void mixIsTheSameFunctionTheJdkUses() {
        // The first number out of SplittableRandom(0) is splitmix64's mix(0 + GOLDEN_GAMMA): our of(0).
        assertThat(Hash64.of(0)).isEqualTo(new SplittableRandom(0).nextLong());
        assertThat(Hash64.of(0)).isEqualTo(0xE220A8397B1DCDAFL);
    }

    @Test
    void sameInputGivesTheSameOutput() {
        assertThat(Hash64.of(42, 5)).isEqualTo(Hash64.of(42, 5));
        assertThat(Hash64.ofString("battle")).isEqualTo(Hash64.ofString("battle"));
    }

    @Test
    void orderMatters() {
        assertThat(Hash64.of(1, 2)).isNotEqualTo(Hash64.of(2, 1));
        assertThat(Hash64.ofString("ab")).isNotEqualTo(Hash64.ofString("ba"));
    }

    @Test
    void nearbyInputsLandFarApart() {
        assertThat(Hash64.of(42, 1)).isNotEqualTo(Hash64.of(42, 2));
        assertThat(Hash64.of(42, 1)).isNotEqualTo(Hash64.of(43, 1));
        assertThat(Hash64.of(1)).isNotEqualTo(Hash64.of(1, 0));
    }
}
