package com.forgottenheroes.engine.rng;

/**
 * The recipe for every seed in a run. The run seed is drawn once at run start (Step 20) and
 * stored; everything else is computed from it, so the same run seed replays the same game.
 *
 * <pre>
 * runSeed  -> turnSeed(runSeed, turn)     the shop and tavern abilities of one turn
 * runSeed  -> battleSeed(runSeed, turn)   the fight at the end of that turn
 * turnSeed -> rollSeed(turnSeed, index)   the n-th shop roll of the turn, 0 = the free one
 * turnSeed -> actionSeed(turnSeed, n)     tavern abilities and relics of the action that produced event n
 * </pre>
 */
public final class SeedDerivation {

    private static final long BATTLE = Hash64.ofString("battle");
    private static final long ROLL = Hash64.ofString("roll");
    private static final long ACTION = Hash64.ofString("action");

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

    /** The dice for abilities fired by one tavern action, keyed by the last event number before it. */
    public static long actionSeed(long turnSeed, int lastEventSeq) {
        if (lastEventSeq < 0) {
            throw new IllegalArgumentException("event sequence cannot be negative: " + lastEventSeq);
        }
        return Hash64.of(turnSeed, ACTION, lastEventSeq);
    }

    private static void requireTurn(int turn) {
        if (turn < 1) {
            throw new IllegalArgumentException("turns start at 1: " + turn);
        }
    }
}
