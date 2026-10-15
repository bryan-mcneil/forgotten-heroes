package com.forgottenheroes.engine.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The shop: hero slots, relic slots, the permanent buff future heroes spawn with (Royal Decree) and
 * the temporary bonus slot a level-up opens. Slot counts grow with the turn (GDD §4.3).
 */
public final class Tavern {

    private final List<TavernSlot> heroSlots = new ArrayList<>();
    private final List<TavernSlot> relicSlots = new ArrayList<>();
    private Stats permanentBuff = Stats.ZERO;
    private TavernSlot bonusSlot;

    /** 3 hero slots on turns 1–4, 4 on turns 5–8, 5 from turn 9. */
    public static int heroSlotsFor(int turn) {
        requireTurn(turn);
        if (turn <= 4) {
            return 3;
        }
        return turn <= 8 ? 4 : 5;
    }

    /** 1 relic slot on turns 1–2, 2 from turn 3. */
    public static int relicSlotsFor(int turn) {
        requireTurn(turn);
        return turn <= 2 ? 1 : 2;
    }

    public List<TavernSlot> heroSlots() {
        return List.copyOf(heroSlots);
    }

    public List<TavernSlot> relicSlots() {
        return List.copyOf(relicSlots);
    }

    /** Hero slots including the bonus slot, which sits at index {@code heroSlots().size()} when open. */
    public int heroSlotCount() {
        return heroSlots.size() + (bonusSlot == null ? 0 : 1);
    }

    public boolean isBonusSlotIndex(int index) {
        return bonusSlot != null && index == heroSlots.size();
    }

    public TavernSlot heroSlot(int index) {
        if (isBonusSlotIndex(index)) {
            return bonusSlot;
        }
        requireIndex(index, heroSlots.size());
        return heroSlots.get(index);
    }

    public TavernSlot relicSlot(int index) {
        requireIndex(index, relicSlots.size());
        return relicSlots.get(index);
    }

    public void setHeroSlot(int index, TavernSlot slot) {
        Objects.requireNonNull(slot, "slot");
        if (isBonusSlotIndex(index)) {
            bonusSlot = slot.isEmpty() ? null : slot;
            return;
        }
        requireIndex(index, heroSlots.size());
        heroSlots.set(index, slot);
    }

    public void setRelicSlot(int index, TavernSlot slot) {
        requireIndex(index, relicSlots.size());
        relicSlots.set(index, Objects.requireNonNull(slot, "slot"));
    }

    /** Grow or shrink to the turn's count; slots that survive keep their contents, new ones are empty. */
    public void resizeHeroSlots(int count) {
        resize(heroSlots, count);
    }

    public void resizeRelicSlots(int count) {
        resize(relicSlots, count);
    }

    /** An extra relic slot for this turn (Herbalist stocks a Ration), used when no relic slot is empty. */
    public void addRelicSlot(TavernSlot slot) {
        relicSlots.add(Objects.requireNonNull(slot, "slot"));
    }

    public Stats permanentBuff() {
        return permanentBuff;
    }

    public void addPermanentBuff(Stats buff) {
        permanentBuff = permanentBuff.plus(buff);
    }

    public Optional<TavernSlot> bonusSlot() {
        return Optional.ofNullable(bonusSlot);
    }

    public void openBonusSlot(TavernSlot slot) {
        if (slot.isEmpty()) {
            throw new IllegalArgumentException("the bonus slot needs a hero");
        }
        bonusSlot = slot;
    }

    public void closeBonusSlot() {
        bonusSlot = null;
    }

    public Tavern copy() {
        Tavern copy = new Tavern();
        copy.heroSlots.addAll(heroSlots);
        copy.relicSlots.addAll(relicSlots);
        copy.permanentBuff = permanentBuff;
        copy.bonusSlot = bonusSlot;
        return copy;
    }

    private static void resize(List<TavernSlot> slots, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("slot count cannot be negative: " + count);
        }
        while (slots.size() > count) {
            slots.removeLast();
        }
        while (slots.size() < count) {
            slots.add(TavernSlot.EMPTY);
        }
    }

    private static void requireIndex(int index, int size) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("slot " + index + " is not in 0.." + (size - 1));
        }
    }

    private static void requireTurn(int turn) {
        if (turn < 1) {
            throw new IllegalArgumentException("turns start at 1: " + turn);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Tavern that
            && heroSlots.equals(that.heroSlots)
            && relicSlots.equals(that.relicSlots)
            && permanentBuff.equals(that.permanentBuff)
            && Objects.equals(bonusSlot, that.bonusSlot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(heroSlots, relicSlots, permanentBuff, bonusSlot);
    }

    @Override
    public String toString() {
        return "heroes " + heroSlots + (bonusSlot == null ? "" : " bonus " + bonusSlot) + ", relics " + relicSlots
            + (permanentBuff.isZero() ? "" : ", buff +" + permanentBuff);
    }
}
