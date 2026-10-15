package com.forgottenheroes.engine.bot;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.model.ItemKind;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Rules;
import com.forgottenheroes.engine.rng.GameRandom;

/**
 * Any action at all, chosen at random; once gold runs low it ends the turn one time in five. The
 * floor of play: a bot that cannot beat it has a bug, a hero that cannot beat it is too weak.
 */
public final class RandomBot implements Bot {

    @Override
    public String name() {
        return "random";
    }

    @Override
    public Action next(RunState state, ContentIndex content, GameRandom rng) {
        if (state.gold() < Rules.HERO_PRICE && rng.chance(20)) {
            return new Action.EndTurn();
        }
        int heroSlots = Math.max(1, state.tavern().heroSlotCount());
        int relicSlots = Math.max(1, state.tavern().relicSlots().size());
        int band = Math.max(1, state.band().size());
        return switch (rng.nextInt(8)) {
            case 0, 1 -> new Action.Buy(rng.nextInt(heroSlots), rng.nextInt(band + 1));
            case 2 -> new Action.BuyRelic(rng.nextInt(relicSlots), rng.chance(50) ? null : rng.nextInt(band));
            case 3 -> new Action.Sell(rng.nextInt(band));
            case 4 -> new Action.Move(rng.nextInt(band), rng.nextInt(band));
            case 5 -> {
                int from = rng.nextInt(band);
                yield new Action.Merge(from, (from + 1 + rng.nextInt(Math.max(1, band - 1))) % Math.max(2, band));
            }
            case 6 -> new Action.Roll();
            default -> rng.chance(50)
                ? new Action.Freeze(rng.chance(50) ? ItemKind.HERO : ItemKind.RELIC, rng.nextInt(heroSlots))
                : new Action.Unfreeze(rng.chance(50) ? ItemKind.HERO : ItemKind.RELIC, rng.nextInt(heroSlots));
        };
    }
}
