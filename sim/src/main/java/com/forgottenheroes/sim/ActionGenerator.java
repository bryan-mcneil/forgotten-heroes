package com.forgottenheroes.sim;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.model.ItemKind;
import com.forgottenheroes.engine.rng.GameRandom;

/**
 * Random actions for the fuzzer, legal and illegal alike: indexes run past every real slot and band
 * position on purpose, because a refusal must come back as a {@code RuleViolation} and nothing else.
 */
final class ActionGenerator {

    private ActionGenerator() { }

    static Action next(GameRandom rng) {
        return switch (rng.nextInt(10)) {
            case 0, 1 -> new Action.Buy(rng.nextInt(7), rng.nextInt(7));
            case 2 -> new Action.BuyRelic(rng.nextInt(4), rng.chance(50) ? null : rng.nextInt(6));
            case 3 -> new Action.Sell(rng.nextInt(6));
            case 4 -> new Action.Move(rng.nextInt(6), rng.nextInt(6));
            case 5 -> {
                int from = rng.nextInt(6);
                yield new Action.Merge(from, (from + 1 + rng.nextInt(5)) % 6);
            }
            case 6 -> new Action.Roll();
            case 7 -> new Action.Freeze(rng.chance(50) ? ItemKind.HERO : ItemKind.RELIC, rng.nextInt(7));
            case 8 -> new Action.Unfreeze(rng.chance(50) ? ItemKind.HERO : ItemKind.RELIC, rng.nextInt(7));
            default -> rng.chance(70) ? new Action.EndTurn() : new Action.StartTurn();
        };
    }
}
