package com.forgottenheroes.engine.rng;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameRandomTest {

    private static List<Integer> draw(GameRandom random, int count) {
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            numbers.add(random.nextInt(1000));
        }
        return numbers;
    }

    @Test
    void sameSeedGivesTheSameThousandNumbers() {
        assertThat(draw(new GameRandom(42), 1000)).isEqualTo(draw(new GameRandom(42), 1000));
    }

    @Test
    void differentSeedsGiveDifferentNumbers() {
        assertThat(draw(new GameRandom(42), 1000)).isNotEqualTo(draw(new GameRandom(43), 1000));
    }

    @Test
    void forksWithDifferentLabelsAreIndependent() {
        GameRandom turn = new GameRandom(42);
        assertThat(draw(turn.fork("shop"), 100)).isNotEqualTo(draw(turn.fork("battle"), 100));
    }

    @Test
    void forkDependsOnTheSeedAndLabelOnly() {
        GameRandom untouched = new GameRandom(42);
        GameRandom busy = new GameRandom(42);
        draw(busy, 50);
        assertThat(draw(busy.fork("battle"), 100)).isEqualTo(draw(untouched.fork("battle"), 100));
    }

    @Test
    void nextIntStaysBelowTheBound() {
        GameRandom random = new GameRandom(7);
        for (int i = 0; i < 1000; i++) {
            assertThat(random.nextInt(6)).isBetween(0, 5);
        }
        assertThatThrownBy(() -> random.nextInt(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void chanceAtTheEdgesIsCertain() {
        GameRandom random = new GameRandom(7);
        for (int i = 0; i < 100; i++) {
            assertThat(random.chance(0)).isFalse();
            assertThat(random.chance(100)).isTrue();
        }
        assertThatThrownBy(() -> random.chance(101)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pickOnOneElementReturnsIt() {
        assertThat(new GameRandom(1).pick(List.of("only"))).isEqualTo("only");
        assertThatThrownBy(() -> new GameRandom(1).pick(List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shuffledIsAPermutationAndLeavesTheOriginalAlone() {
        List<Integer> original = List.of(1, 2, 3, 4, 5, 6, 7, 8);
        List<Integer> shuffled = new GameRandom(3).shuffled(original);
        assertThat(shuffled).containsExactlyInAnyOrderElementsOf(original).isNotEqualTo(original);
        assertThat(original).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    void blankForkLabelsAreRejected() {
        assertThatThrownBy(() -> new GameRandom(1).fork(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
