package com.forgottenheroes.engine.model;

/**
 * One shelf in the tavern. Holds a hero id or a relic id with its price; a hero slot also carries the
 * tavern buff it was stocked with (Royal Decree). An empty slot has no item and cannot be frozen.
 */
public record TavernSlot(String itemId, int price, Stats bonus, boolean frozen) {

    public static final TavernSlot EMPTY = new TavernSlot(null, 0, Stats.ZERO, false);

    public TavernSlot {
        if (itemId != null && itemId.isBlank()) {
            throw new IllegalArgumentException("item id cannot be blank; use EMPTY");
        }
        if (price < 0) {
            throw new IllegalArgumentException("price cannot be negative: " + price);
        }
        if (bonus == null) {
            bonus = Stats.ZERO;
        }
        if (itemId == null && (frozen || price != 0 || !bonus.isZero())) {
            throw new IllegalArgumentException("an empty slot has no price, bonus or freeze");
        }
    }

    public static TavernSlot hero(String heroId, Stats bonus) {
        return new TavernSlot(heroId, Rules.HERO_PRICE, bonus, false);
    }

    public static TavernSlot relic(String relicId, int price) {
        return new TavernSlot(relicId, price, Stats.ZERO, false);
    }

    public boolean isEmpty() {
        return itemId == null;
    }

    public TavernSlot withFrozen(boolean newFrozen) {
        return new TavernSlot(itemId, price, bonus, newFrozen);
    }

    public TavernSlot withBonus(Stats newBonus) {
        return new TavernSlot(itemId, price, newBonus, frozen);
    }

    public TavernSlot withPrice(int newPrice) {
        return new TavernSlot(itemId, newPrice, bonus, frozen);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "-";
        }
        return itemId + "@" + price + (bonus.isZero() ? "" : "+" + bonus) + (frozen ? " (frozen)" : "");
    }
}
