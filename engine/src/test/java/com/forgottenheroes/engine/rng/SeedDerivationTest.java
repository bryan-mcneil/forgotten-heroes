package com.forgottenheroes.engine.rng;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SeedDerivationTest {

    private static final long RUN = 42L;

    @Test
    void sameRunAndTurnGiveTheSameSeeds() {
        assertThat(SeedDerivation.turnSeed(RUN, 5)).isEqualTo(SeedDerivation.turnSeed(RUN, 5));
        assertThat(SeedDerivation.battleSeed(RUN, 5)).isEqualTo(SeedDerivation.battleSeed(RUN, 5));
    }

    @Test
    void everyTurnHasItsOwnSeed() {
        assertThat(SeedDerivation.turnSeed(RUN, 1)).isNotEqualTo(SeedDerivation.turnSeed(RUN, 2));
        assertThat(SeedDerivation.turnSeed(RUN, 1)).isNotEqualTo(SeedDerivation.turnSeed(RUN + 1, 1));
    }

    @Test
    void battleAndShopOfOneTurnDoNotShareASeed() {
        assertThat(SeedDerivation.battleSeed(RUN, 3)).isNotEqualTo(SeedDerivation.turnSeed(RUN, 3));
    }

    @Test
    void everyRollOfATurnHasItsOwnSeed() {
        long turn = SeedDerivation.turnSeed(RUN, 3);
        assertThat(SeedDerivation.rollSeed(turn, 0)).isNotEqualTo(SeedDerivation.rollSeed(turn, 1));
        assertThat(SeedDerivation.rollSeed(turn, 0)).isNotEqualTo(turn);
    }

    @Test
    void everyActionOfATurnHasItsOwnSeed() {
        long turn = SeedDerivation.turnSeed(RUN, 3);
        assertThat(SeedDerivation.actionSeed(turn, 4)).isNotEqualTo(SeedDerivation.actionSeed(turn, 5));
        assertThat(SeedDerivation.actionSeed(turn, 4)).isNotEqualTo(SeedDerivation.rollSeed(turn, 4));
        assertThatThrownBy(() -> SeedDerivation.actionSeed(turn, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void badTurnsAndRollsAreRejected() {
        assertThatThrownBy(() -> SeedDerivation.turnSeed(RUN, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SeedDerivation.battleSeed(RUN, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SeedDerivation.rollSeed(RUN, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
