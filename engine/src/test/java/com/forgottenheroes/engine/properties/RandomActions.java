package com.forgottenheroes.engine.properties;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.model.ItemKind;
import com.forgottenheroes.engine.rng.GameRandom;
import java.util.ArrayList;
import java.util.List;

/** Random player actions, legal and illegal alike, derived from one seed so a failing case replays. */
final class RandomActions {

    private RandomActions() { }

    static List<Action> sequence(long seed, int count) {
        GameRandom rng = new GameRandom(seed);
        List<Action> actions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            actions.add(next(rng));
        }
        return actions;
    }

    static Action next(GameRandom rng) {
        return switch (rng.nextInt(10)) {
            case 0, 1, 2 -> new Action.Buy(rng.nextInt(6), rng.nextInt(6));
            case 3 -> new Action.BuyRelic(rng.nextInt(3), rng.chance(50) ? null : rng.nextInt(5));
            case 4 -> new Action.Sell(rng.nextInt(5));
            case 5 -> new Action.Move(rng.nextInt(5), rng.nextInt(5));
            case 6 -> {
                int from = rng.nextInt(5);
                yield new Action.Merge(from, (from + 1 + rng.nextInt(4)) % 5);
            }
            case 7 -> new Action.Roll();
            case 8 -> rng.chance(50) ? new Action.Freeze(rng.chance(50) ? ItemKind.HERO : ItemKind.RELIC, rng.nextInt(6))
                : new Action.Unfreeze(rng.chance(50) ? ItemKind.HERO : ItemKind.RELIC, rng.nextInt(6));
            default -> new Action.EndTurn();
        };
    }
}
