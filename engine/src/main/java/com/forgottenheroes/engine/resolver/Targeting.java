package com.forgottenheroes.engine.resolver;

import com.forgottenheroes.engine.content.TargetSpec;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.Unit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns a target selector into the units it means, at this moment, on this board (GDD §7). Only
 * living units are ever picked; "ally" never includes the unit whose ability fires; "enemy" is empty
 * in the tavern. Ties (lowest health, random order) are broken deterministically: by the seeded dice
 * or by position, front first.
 */
public final class Targeting {

    private Targeting() { }

    public static List<Unit> select(TargetSpec spec, EffectContext context) {
        int count = spec.count().resolve(context.level(), context.attack());
        return switch (spec.select()) {
            case SELF -> context.selfUnit().map(List::of).orElse(List.of());
            case CHOSEN -> context.chosen() == null ? List.of() : List.of(context.chosen());
            case TRIGGERING_UNIT -> context.triggering() == null || !context.triggering().isAlive() ? List.of() : List.of(context.triggering());
            case ALL_ALLIES -> allies(context);
            case RANDOM_ALLY -> firstOf(context.rng().shuffled(allies(context)), count);
            case ALL_ENEMIES -> enemies(context);
            case RANDOM_ENEMY -> firstOf(context.rng().shuffled(enemies(context)), count);
            case ALL_UNITS -> {
                List<Unit> all = new ArrayList<>(context.band().alive());
                all.addAll(enemies(context));
                yield all;
            }
            case ADJACENT -> {
                List<Unit> adjacent = new ArrayList<>();
                nearestAhead(context, 1).forEach(adjacent::add);
                nearestBehind(context, 1).forEach(adjacent::add);
                yield adjacent;
            }
            case NEAREST_ALLIES_BEHIND -> nearestBehind(context, count);
            case NEAREST_ALLIES_AHEAD -> nearestAhead(context, count);
            case FIRST_ENEMY -> firstOf(enemies(context), 1);
            case LAST_ENEMY -> {
                List<Unit> enemies = enemies(context);
                yield enemies.isEmpty() ? List.of() : List.of(enemies.getLast());
            }
            case LOWEST_HEALTH_ENEMY -> enemies(context).stream().min(Comparator.comparingInt(Unit::health)).map(List::of).orElse(List.of());
            case HIGHEST_HEALTH_ENEMY -> enemies(context).stream().max(Comparator.comparingInt(Unit::health)).map(List::of).orElse(List.of());
            case FRONT_ALLY -> firstOf(allies(context), 1);
            case LEVEL_2_PLUS_ALLIES -> firstOf(context.rng().shuffled(allies(context).stream().filter(unit -> unit.level() >= 2).toList()), count);
            case TAVERN_HEROES -> throw new IllegalArgumentException("TAVERN_HEROES is not a unit selector; use BUFF_TAVERN");
        };
    }

    /** Every living unit on the same side except the unit whose ability fires, front first. */
    static List<Unit> allies(EffectContext context) {
        List<Unit> allies = new ArrayList<>();
        for (Unit unit : context.band().alive()) {
            if (unit != context.self()) {
                allies.add(unit);
            }
        }
        return allies;
    }

    /** Every living unit on the other side, front first; none in the tavern. */
    static List<Unit> enemies(EffectContext context) {
        return context.hasEnemies() ? context.enemies().alive() : List.of();
    }

    private static List<Unit> nearestBehind(EffectContext context, int count) {
        return neighbours(context, count, 1);
    }

    private static List<Unit> nearestAhead(EffectContext context, int count) {
        return neighbours(context, count, -1);
    }

    /** Living allies next to self in one direction, nearest first; {@code step} is +1 for behind, −1 for ahead. */
    private static List<Unit> neighbours(EffectContext context, int count, int step) {
        Unit self = context.self();
        Band band = context.band();
        int index = self == null ? -1 : band.indexOf(self);
        if (index < 0) {
            return List.of();
        }
        List<Unit> found = new ArrayList<>();
        for (int i = index + step; i >= 0 && i < band.size() && found.size() < count; i += step) {
            Unit unit = band.at(i);
            if (unit.isAlive()) {
                found.add(unit);
            }
        }
        return found;
    }

    static <T> List<T> firstOf(List<T> items, int count) {
        return List.copyOf(items.subList(0, Math.min(count, items.size())));
    }
}
