package com.forgottenheroes.engine.mutator;

import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.HeroDefinition;
import com.forgottenheroes.engine.content.RelicDefinition;
import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.event.GameEvent.BonusSlotOpened;
import com.forgottenheroes.engine.event.GameEvent.TavernItemFrozen;
import com.forgottenheroes.engine.event.GameEvent.TavernItemUnfrozen;
import com.forgottenheroes.engine.event.GameEvent.TavernRolled;
import com.forgottenheroes.engine.model.ItemKind;
import com.forgottenheroes.engine.model.Rank;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Tavern;
import com.forgottenheroes.engine.model.TavernSlot;
import com.forgottenheroes.engine.rng.GameRandom;
import java.util.List;

/** Changes to the shelves: refills, freezes, slot counts. */
public final class TavernMutator {

    private TavernMutator() { }

    /** Bring the slot counts to this turn's table and close the bonus slot; frozen items in surviving slots stay. */
    public static void resizeForTurn(RunState state) {
        Tavern tavern = state.tavern();
        tavern.resizeHeroSlots(Tavern.heroSlotsFor(state.turn()));
        tavern.resizeRelicSlots(Tavern.relicSlotsFor(state.turn()));
        tavern.closeBonusSlot();
    }

    /**
     * Refill every unfrozen slot: each draws uniformly from the heroes (or relics) of rank up to the
     * turn's rank (GDD §4.3); new heroes carry the tavern's permanent buff. An unfrozen bonus slot closes.
     */
    public static void fill(RunState state, EventLog log, ContentIndex content, GameRandom rng, boolean free) {
        Tavern tavern = state.tavern();
        Rank rank = Rank.availableAt(state.turn());
        List<HeroDefinition> heroes = content.heroesOfRankUpTo(rank);
        List<RelicDefinition> relics = content.relicsOfRankUpTo(rank);
        List<TavernSlot> heroSlots = tavern.heroSlots();
        for (int i = 0; i < heroSlots.size(); i++) {
            if (!heroSlots.get(i).frozen()) {
                tavern.setHeroSlot(i, TavernSlot.hero(rng.pick(heroes).id(), tavern.permanentBuff()));
            }
        }
        List<TavernSlot> relicSlots = tavern.relicSlots();
        for (int i = 0; i < relicSlots.size(); i++) {
            if (!relicSlots.get(i).frozen()) {
                RelicDefinition relic = rng.pick(relics);
                tavern.setRelicSlot(i, TavernSlot.relic(relic.id(), relic.price()));
            }
        }
        if (tavern.bonusSlot().filter(slot -> !slot.frozen()).isPresent()) {
            tavern.closeBonusSlot();
        }
        log.append(new TavernRolled(0, free, tavern.allHeroSlots(), tavern.relicSlots()));
    }

    /** Empty a hero shelf after a purchase; the bonus slot simply closes. */
    public static void takeHero(RunState state, int slot) {
        state.tavern().setHeroSlot(slot, TavernSlot.EMPTY);
    }

    /** Empty a relic shelf after a purchase. */
    public static void takeRelic(RunState state, int slot) {
        state.tavern().setRelicSlot(slot, TavernSlot.EMPTY);
    }

    /**
     * GDD §4.4: a level-up puts one hero of the next rank (capped at VI) in a temporary extra slot. While the
     * roster is incomplete the pool falls back to every hero up to that rank.
     */
    public static void openBonusSlot(RunState state, EventLog log, ContentIndex content, GameRandom rng) {
        Tavern tavern = state.tavern();
        Rank next = Rank.availableAt(state.turn()).next();
        List<HeroDefinition> pool = content.heroesOfRank(next);
        if (pool.isEmpty()) {
            pool = content.heroesOfRankUpTo(next);
        }
        TavernSlot item = TavernSlot.hero(rng.pick(pool).id(), tavern.permanentBuff());
        tavern.openBonusSlot(item);
        log.append(new BonusSlotOpened(0, tavern.heroSlots().size(), item));
    }

    public static void freeze(RunState state, EventLog log, ItemKind kind, int slot) {
        TavernSlot item = slot(state.tavern(), kind, slot);
        put(state.tavern(), kind, slot, item.withFrozen(true));
        log.append(new TavernItemFrozen(0, kind, slot, item.itemId()));
    }

    public static void unfreeze(RunState state, EventLog log, ItemKind kind, int slot) {
        TavernSlot item = slot(state.tavern(), kind, slot);
        put(state.tavern(), kind, slot, item.withFrozen(false));
        log.append(new TavernItemUnfrozen(0, kind, slot, item.itemId()));
    }

    /** The slot of either kind at an index; the resolver has already checked the index. */
    public static TavernSlot slot(Tavern tavern, ItemKind kind, int index) {
        return kind == ItemKind.HERO ? tavern.heroSlot(index) : tavern.relicSlot(index);
    }

    private static void put(Tavern tavern, ItemKind kind, int index, TavernSlot slot) {
        if (kind == ItemKind.HERO) {
            tavern.setHeroSlot(index, slot);
        } else {
            tavern.setRelicSlot(index, slot);
        }
    }
}
