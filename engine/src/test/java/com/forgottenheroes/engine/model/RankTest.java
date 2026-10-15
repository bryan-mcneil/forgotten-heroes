package com.forgottenheroes.engine.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RankTest {

    @ParameterizedTest
    @CsvSource({
        "1, I",
        "2, I",
        "3, II",
        "4, II",
        "5, III",
        "7, IV",
        "9, V",
        "11, VI",
        "20, VI"
    })
    void tavernRankFollowsTheTurnTable(int turn, Rank expected) {
        assertThat(Rank.availableAt(turn)).isEqualTo(expected);
    }

    @Test
    void turnUnlockedMatchesTheRulebook() {
        assertThat(Rank.VI.turnUnlocked()).isEqualTo(11);
    }

    @Test
    void nextGoesUpOneRank() {
        assertThat(Rank.I.next()).isEqualTo(Rank.II);
        assertThat(Rank.V.next()).isEqualTo(Rank.VI);
    }

    @Test
    void sixStaysSix() {
        assertThat(Rank.VI.next()).isEqualTo(Rank.VI);
    }

    @Test
    void turnZeroIsRejected() {
        assertThatThrownBy(() -> Rank.availableAt(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
