package com.forgottenheroes.engine.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PerkTest {

    @Test
    void onceOnlyPerksAreConsumable() {
        assertThat(Perk.WARD.consumable()).isTrue();
        assertThat(Perk.PHOENIX_FEATHER.consumable()).isTrue();
        assertThat(Perk.ENCHANTED_BLADE.consumable()).isTrue();
        assertThat(Perk.AEGIS.consumable()).isTrue();
    }

    @Test
    void lastingPerksAreNotConsumable() {
        assertThat(Perk.IRON_BLADE.consumable()).isFalse();
        assertThat(Perk.VENOM.consumable()).isFalse();
    }

    @Test
    void allNinePerksExist() {
        assertThat(Perk.values()).hasSize(9);
    }
}
