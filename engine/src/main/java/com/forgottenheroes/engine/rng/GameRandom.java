package com.forgottenheroes.engine.rng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;

/**
 * The only dice in the engine. Built from a seed, it hands out the same numbers in the same order
 * every time, so a run can be replayed. Forks give each subsystem its own stream: drawing one more
 * number in the shop never changes what happens in the battle.
 */
public final class GameRandom {

    private final long seed;
    private final SplittableRandom random;

    public GameRandom(long seed) {
        this.seed = seed;
        this.random = new SplittableRandom(seed);
    }

    /** The seed this generator started from, for logs and bug reports ("seed 42, turn 5"). */
    public long seed() {
        return seed;
    }

    /** A number from 0 up to, but not including, bound. */
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive: " + bound);
        }
        return random.nextInt(bound);
    }

    /** True with the given probability: chance(0) is never true, chance(100) always is. */
    public boolean chance(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("percent must be 0..100: " + percent);
        }
        return nextInt(100) < percent;
    }

    /** One element of the list, each equally likely. */
    public <T> T pick(List<T> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("cannot pick from an empty list");
        }
        return items.get(nextInt(items.size()));
    }

    /** A new list with the same elements in a random order. The given list is not changed. */
    public <T> List<T> shuffled(List<T> items) {
        List<T> copy = new ArrayList<>(items);
        Collections.shuffle(copy, random);
        return List.copyOf(copy);
    }

    /** A child generator for one subsystem. Same seed + same label = same stream, however many numbers this one drew. */
    public GameRandom fork(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("fork label cannot be blank");
        }
        return new GameRandom(Hash64.of(seed, Hash64.ofString(label)));
    }
}
