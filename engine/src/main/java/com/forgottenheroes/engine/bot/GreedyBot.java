package com.forgottenheroes.engine.bot;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.HeroDefinition;
import com.forgottenheroes.engine.content.RelicDefinition;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Rules;
import com.forgottenheroes.engine.model.Tavern;
import com.forgottenheroes.engine.model.TavernSlot;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.model.Xp;
import com.forgottenheroes.engine.rng.GameRandom;

/**
 * Plays the obvious game: merge copies, buy the strongest affordable hero, use a relic on the best
 * fighter, keep the toughest hero in front, roll when the shelves hold nothing worth buying, then
 * end the turn. No lookahead, no memory; the baseline a balance pass compares heroes against.
 */
public final class GreedyBot implements Bot {

    @Override
    public String name() {
        return "greedy";
    }

    @Override
    public Action next(RunState state, ContentIndex content, GameRandom rng) {
        Tavern tavern = state.tavern();
        Band band = state.band();
        int gold = state.gold();

        // 1. A copy of one of my heroes on the shelf: merge it.
        for (int slot = 0; slot < tavern.heroSlotCount(); slot++) {
            TavernSlot item = tavern.heroSlot(slot);
            if (item.isEmpty() || gold < item.price()) {
                continue;
            }
            for (int i = 0; i < band.size(); i++) {
                Unit unit = band.at(i);
                if (unit.heroId().equals(item.itemId()) && unit.xp().points() < Xp.MAX) {
                    return new Action.Buy(slot, i);
                }
            }
        }
        // 2. Room on the band: the strongest affordable hero.
        int best = bestHeroSlot(tavern, content, gold);
        if (best >= 0 && !band.isFull()) {
            return new Action.Buy(best, band.size());
        }
        // 3. A full band and a clearly better hero on the shelf: sell the weakest to make room.
        if (best >= 0 && band.isFull()) {
            int weakest = weakest(band);
            HeroDefinition offer = content.hero(tavern.heroSlot(best).itemId());
            Unit weak = band.at(weakest);
            if (weak.level() == 1 && strength(offer) > weak.attack() + weak.maxHealth() + 2) {
                return new Action.Sell(weakest);
            }
        }
        // 4. A relic I can afford, on my best fighter.
        for (int slot = 0; slot < tavern.relicSlots().size(); slot++) {
            TavernSlot item = tavern.relicSlot(slot);
            if (item.isEmpty() || gold < item.price()) {
                continue;
            }
            RelicDefinition relic = content.relic(item.itemId());
            if (relic.id().equals("poison_vial")) {
                continue;
            }
            if (relic.needsChosenTarget()) {
                if (!band.isEmpty()) {
                    return new Action.BuyRelic(slot, strongest(band));
                }
            } else if (!band.isEmpty() || relic.id().equals("royal_decree")) {
                return new Action.BuyRelic(slot, null);
            }
        }
        // 5. The toughest hero stands in front.
        if (band.size() >= 2) {
            int toughest = toughest(band);
            if (toughest != 0) {
                return new Action.Move(toughest, 0);
            }
        }
        // 6. Nothing worth buying: roll while a roll still leaves money for a hero.
        boolean canRoll = state.freeRollAvailable() || gold >= Rules.ROLL_PRICE + Rules.HERO_PRICE;
        if (canRoll && (!band.isFull() || gold >= Rules.ROLL_PRICE + Rules.HERO_PRICE)) {
            return new Action.Roll();
        }
        return new Action.EndTurn();
    }

    private static int bestHeroSlot(Tavern tavern, ContentIndex content, int gold) {
        int best = -1;
        int bestScore = -1;
        for (int slot = 0; slot < tavern.heroSlotCount(); slot++) {
            TavernSlot item = tavern.heroSlot(slot);
            if (item.isEmpty() || gold < item.price()) {
                continue;
            }
            HeroDefinition hero = content.hero(item.itemId());
            int score = hero.rank().number() * 100 + strength(hero) + item.bonus().attack() + item.bonus().health();
            if (score > bestScore) {
                bestScore = score;
                best = slot;
            }
        }
        return best;
    }

    private static int strength(HeroDefinition hero) {
        return hero.attack() + hero.health();
    }

    private static int strongest(Band band) {
        int best = 0;
        for (int i = 1; i < band.size(); i++) {
            if (band.at(i).attack() > band.at(best).attack()) {
                best = i;
            }
        }
        return best;
    }

    private static int toughest(Band band) {
        int best = 0;
        for (int i = 1; i < band.size(); i++) {
            if (band.at(i).maxHealth() > band.at(best).maxHealth()) {
                best = i;
            }
        }
        return best;
    }

    private static int weakest(Band band) {
        int worst = 0;
        for (int i = 1; i < band.size(); i++) {
            Unit unit = band.at(i);
            Unit current = band.at(worst);
            if (unit.attack() + unit.maxHealth() < current.attack() + current.maxHealth()) {
                worst = i;
            }
        }
        return worst;
    }
}
