package com.forgottenheroes.engine.rng;

/**
 * Turns numbers into other numbers that look random but never change: the same input always gives
 * the same output. Every seed in the game is made this way, so a run can be replayed from one number.
 * The mixer is splitmix64, the same function SplittableRandom uses inside.
 */
public final class Hash64 {

    /** A large odd constant (2^64 divided by the golden ratio) that spreads nearby inputs apart. */
    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

    private Hash64() { }

    /** Scrambles one number: flipping any input bit changes about half the output bits. */
    public static long mix(long x) {
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    /** Hashes the parts in order: of(1, 2) and of(2, 1) differ, so (runSeed, turn) is safe. */
    public static long of(long... parts) {
        long hash = 0;
        for (long part : parts) {
            hash = mix(hash + GOLDEN_GAMMA + part);
        }
        return hash;
    }

    /** Hashes a label such as "battle", one character at a time. */
    public static long ofString(String text) {
        long hash = text.length();
        for (int i = 0; i < text.length(); i++) {
            hash = mix(hash + GOLDEN_GAMMA + text.charAt(i));
        }
        return hash;
    }
}
