package com.forgottenheroes.engine.rng;

/**
 * The recipe for every seed in a run. The run seed is drawn once at run start (Step 20) and
 * stored; everything else is computed from it, so the same run seed replays the same game.
 *
 * <pre>
 * runSeed  -> turnSeed(runSeed, turn)     the shop and tavern abilities of one turn
 * runSeed  -> battleSeed(runSeed, turn)   the fight at the end of that turn
 * turnSeed -> rollSeed(turnSeed, index)   the n-th shop roll of the turn, 0 = the free one
 * </pre>
 */
public final class SeedDerivation {

    private static final long BATTLE = Hash64.ofString("battle");
    private static final long ROLL = Hash64.ofString("roll");

    private SeedDerivation() { }

    public static long turnSeed(long runSeed, int turn) {
        requireTurn(turn);
        return Hash64.of(runSeed, turn);
    }

    public static long battleSeed(long runSeed, int turn) {
        requireTurn(turn);
        return Hash64.of(runSeed, turn, BATTLE);
    }

    public static long rollSeed(long turnSeed, int rollIndex) {
        if (rollIndex < 0) {
            throw new IllegalArgumentException("roll index cannot be negative: " + rollIndex);
        }
        return Hash64.of(turnSeed, ROLL, rollIndex);
    }

    private static void requireTurn(int turn) {
        if (turn < 1) {
            throw new IllegalArgumentException("turns start at 1: " + turn);
        }
    }
}
