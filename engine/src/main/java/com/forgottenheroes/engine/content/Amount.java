package com.forgottenheroes.engine.content;

import java.util.List;

/**
 * A number in the DSL that may depend on the hero's level or attack (GDD §7). Exactly one of the
 * four shapes is set: {@code {"flat": 4}}, {@code {"perLevel": 1}} (1/2/3), {@code {"byLevel": [1, 2, 3]}}
 * or {@code {"percentOfAttack": 50}} (50 % × level of the unit's attack, rounded down).
 */
public record Amount(Integer flat, Integer perLevel, List<Integer> byLevel, Integer percentOfAttack) {

    public static final int LEVELS = 3;

    public Amount {
        int set = 0;
        if (flat != null) {
            set++;
        }
        if (perLevel != null) {
            set++;
        }
        if (byLevel != null) {
            set++;
            if (byLevel.size() != LEVELS) {
                throw new IllegalArgumentException("byLevel needs " + LEVELS + " values, got " + byLevel);
            }
            byLevel = List.copyOf(byLevel);
        }
        if (percentOfAttack != null) {
            set++;
        }
        if (set != 1) {
            throw new IllegalArgumentException("an amount needs exactly one of flat, perLevel, byLevel, percentOfAttack");
        }
        if (isNegative(flat) || isNegative(perLevel) || isNegative(percentOfAttack)
                || (byLevel != null && byLevel.stream().anyMatch(value -> value < 0))) {
            throw new IllegalArgumentException("amounts cannot be negative");
        }
    }

    public static Amount flat(int value) {
        return new Amount(value, null, null, null);
    }

    public static Amount perLevel(int value) {
        return new Amount(null, value, null, null);
    }

    public static Amount byLevel(int level1, int level2, int level3) {
        return new Amount(null, null, List.of(level1, level2, level3), null);
    }

    public static Amount percentOfAttack(int percent) {
        return new Amount(null, null, null, percent);
    }

    /** The concrete number for a unit of this level with this attack. */
    public int resolve(int level, int attack) {
        if (level < 1 || level > LEVELS) {
            throw new IllegalArgumentException("level must be 1.." + LEVELS + ", got " + level);
        }
        if (flat != null) {
            return flat;
        }
        if (perLevel != null) {
            return perLevel * level;
        }
        if (byLevel != null) {
            return byLevel.get(level - 1);
        }
        return attack * percentOfAttack * level / 100;
    }

    private static boolean isNegative(Integer value) {
        return value != null && value < 0;
    }
}
