package com.forgottenheroes.engine.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class AmountTest {

    @Test
    void flatIgnoresLevel() {
        assertThat(Amount.flat(4).resolve(1, 10)).isEqualTo(4);
        assertThat(Amount.flat(4).resolve(3, 10)).isEqualTo(4);
    }

    @Test
    void perLevelMultiplies() {
        assertThat(Amount.perLevel(2).resolve(1, 0)).isEqualTo(2);
        assertThat(Amount.perLevel(2).resolve(2, 0)).isEqualTo(4);
        assertThat(Amount.perLevel(2).resolve(3, 0)).isEqualTo(6);
    }

    @Test
    void byLevelLooksUpTheTable() {
        Amount amount = Amount.byLevel(2, 1, 0);
        assertThat(amount.resolve(1, 0)).isEqualTo(2);
        assertThat(amount.resolve(2, 0)).isEqualTo(1);
        assertThat(amount.resolve(3, 0)).isEqualTo(0);
    }

    @Test
    void percentOfAttackScalesWithLevelAndRoundsDown() {
        assertThat(Amount.percentOfAttack(50).resolve(1, 7)).isEqualTo(3);
        assertThat(Amount.percentOfAttack(50).resolve(2, 7)).isEqualTo(7);
        assertThat(Amount.percentOfAttack(50).resolve(3, 7)).isEqualTo(10);
    }

    @Test
    void exactlyOneShapeIsRequired() {
        assertThatThrownBy(() -> new Amount(null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Amount(1, 1, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeAndShortTablesAreRejected() {
        assertThatThrownBy(() -> Amount.flat(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Amount.byLevel(1, -2, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Amount(null, null, List.of(1, 2), null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void levelMustBeOneToThree() {
        assertThatThrownBy(() -> Amount.flat(1).resolve(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Amount.flat(1).resolve(4, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
