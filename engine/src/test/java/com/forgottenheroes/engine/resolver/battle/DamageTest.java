package com.forgottenheroes.engine.resolver.battle;

import static com.forgottenheroes.engine.testing.TestFixtures.unit;
import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.engine.model.Perk;
import org.junit.jupiter.api.Test;

class DamageTest {

    @Test
    void aPlainHitIsTheAttack() {
        assertThat(Damage.attack(unit("a", 4, 1), unit("b", 1, 1)).amount()).isEqualTo(4);
        assertThat(Damage.attack(unit("a", 0, 1), unit("b", 1, 1)).amount()).isZero();
    }

    @Test
    void ironBladeAddsThree() {
        assertThat(Damage.attack(unit("a", 4, 1, Perk.IRON_BLADE), unit("b", 1, 1)).amount()).isEqualTo(7);
    }

    @Test
    void chainmailTakesTwoOffButNeverBelowOne() {
        assertThat(Damage.attack(unit("a", 4, 1), unit("b", 1, 1, Perk.CHAINMAIL)).amount()).isEqualTo(2);
        assertThat(Damage.attack(unit("a", 2, 1), unit("b", 1, 1, Perk.CHAINMAIL)).amount()).isEqualTo(1);
        assertThat(Damage.attack(unit("a", 0, 1), unit("b", 1, 1, Perk.CHAINMAIL)).amount()).isZero();
        assertThat(Damage.incoming(unit("b", 1, 1, Perk.CHAINMAIL), 5).amount()).isEqualTo(3);
    }
}
