package com.forgottenheroes.engine.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Up to five units in fighting order: index 0 is the front and fights first. There are no gaps; an
 * "empty slot" is only something the UI draws.
 */
public final class Band {

    private final List<Unit> units = new ArrayList<>();

    public static Band of(Unit... units) {
        Band band = new Band();
        for (Unit unit : units) {
            band.add(unit);
        }
        return band;
    }

    public int size() {
        return units.size();
    }

    public boolean isEmpty() {
        return units.isEmpty();
    }

    public boolean isFull() {
        return units.size() >= Rules.BAND_SIZE;
    }

    /** A read-only view in fighting order. */
    public List<Unit> units() {
        return List.copyOf(units);
    }

    public Unit at(int index) {
        requireIndex(index, units.size() - 1);
        return units.get(index);
    }

    public Unit front() {
        return at(0);
    }

    public Unit back() {
        return at(units.size() - 1);
    }

    public Optional<Unit> find(UnitId id) {
        return units.stream().filter(unit -> unit.id().equals(id)).findFirst();
    }

    public boolean contains(UnitId id) {
        return find(id).isPresent();
    }

    /** The index of the unit, or -1 when it is not on this band. */
    public int indexOf(UnitId id) {
        for (int i = 0; i < units.size(); i++) {
            if (units.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public int indexOf(Unit unit) {
        return indexOf(unit.id());
    }

    /** Append at the back. */
    public void add(Unit unit) {
        insert(units.size(), unit);
    }

    /** Put the unit at the index; the units behind shift back one place. Index == size appends. */
    public void insert(int index, Unit unit) {
        Objects.requireNonNull(unit, "unit");
        if (isFull()) {
            throw new IllegalStateException("band is full (" + Rules.BAND_SIZE + ")");
        }
        if (contains(unit.id())) {
            throw new IllegalArgumentException("unit " + unit.id().value() + " is already on the band");
        }
        requireIndex(index, units.size());
        units.add(index, unit);
    }

    public Unit remove(int index) {
        requireIndex(index, units.size() - 1);
        return units.remove(index);
    }

    public boolean remove(Unit unit) {
        int index = indexOf(unit);
        if (index < 0) {
            return false;
        }
        units.remove(index);
        return true;
    }

    /** Take the unit at {@code from} out and put it back at {@code to}; everything else keeps its order. */
    public void move(int from, int to) {
        requireIndex(from, units.size() - 1);
        requireIndex(to, units.size() - 1);
        if (from != to) {
            Unit unit = units.remove(from);
            units.add(to, unit);
        }
    }

    /** The units still standing, in order. */
    public List<Unit> alive() {
        return units.stream().filter(Unit::isAlive).toList();
    }

    public boolean anyAlive() {
        return units.stream().anyMatch(Unit::isAlive);
    }

    /** Fresh copies of every unit, ready to fight (no damage, battle counters at zero). */
    public Band copyForBattle() {
        Band copy = new Band();
        for (Unit unit : units) {
            copy.add(unit.copyForBattle());
        }
        return copy;
    }

    /** Exact copies of every unit. */
    public Band copy() {
        Band copy = new Band();
        for (Unit unit : units) {
            copy.add(unit.copy());
        }
        return copy;
    }

    private static void requireIndex(int index, int max) {
        if (index < 0 || index > max) {
            throw new IndexOutOfBoundsException("index " + index + " is not in 0.." + max);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Band that && units.equals(that.units);
    }

    @Override
    public int hashCode() {
        return units.hashCode();
    }

    @Override
    public String toString() {
        return units.toString();
    }
}
