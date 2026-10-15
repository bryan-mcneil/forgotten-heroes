package com.forgottenheroes.engine.resolver;

import com.forgottenheroes.engine.content.TargetSelect;
import com.forgottenheroes.engine.content.TargetSpec;
import com.forgottenheroes.engine.model.Unit;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a target selector into the units it means, at this moment, on this board. Only living units
 * are ever picked, and "ally" never includes the unit whose ability fires. The selectors battles need
 * (enemies, positions) arrive with the battle resolver.
 */
public final class Targeting {

    private Targeting() { }

    public static List<Unit> select(TargetSpec spec, EffectContext context) {
        int count = spec.count().resolve(context.level(), context.attack());
        return switch (spec.select()) {
            case SELF -> context.selfUnit().filter(Unit::isAlive).map(List::of).orElse(List.of());
            case CHOSEN -> context.chosen() == null ? List.of() : List.of(context.chosen());
            case TRIGGERING_UNIT -> context.triggering() == null || !context.triggering().isAlive() ? List.of() : List.of(context.triggering());
            case ALL_ALLIES -> allies(context);
            case RANDOM_ALLY -> firstOf(context.rng().shuffled(allies(context)), count);
            case TAVERN_HEROES -> throw new IllegalArgumentException("TAVERN_HEROES is not a unit selector; use BUFF_TAVERN");
            case RANDOM_ENEMY, ALL_ENEMIES, ALL_UNITS, ADJACENT, NEAREST_ALLIES_BEHIND, NEAREST_ALLIES_AHEAD, FIRST_ENEMY,
                 LAST_ENEMY, LOWEST_HEALTH_ENEMY, HIGHEST_HEALTH_ENEMY, FRONT_ALLY, LEVEL_2_PLUS_ALLIES ->
                throw new UnsupportedOperationException(spec.select() + " arrives with the trigger system (Step 16)");
        };
    }

    /** Every living unit on the same side except the unit whose ability fires. */
    static List<Unit> allies(EffectContext context) {
        List<Unit> allies = new ArrayList<>();
        for (Unit unit : context.band().alive()) {
            if (unit != context.self()) {
                allies.add(unit);
            }
        }
        return allies;
    }

    static <T> List<T> firstOf(List<T> items, int count) {
        return items.subList(0, Math.min(count, items.size()));
    }

    /** Used by the UI and tests to know which selectors exist today. */
    public static boolean supports(TargetSelect select) {
        return switch (select) {
            case SELF, CHOSEN, TRIGGERING_UNIT, ALL_ALLIES, RANDOM_ALLY -> true;
            default -> false;
        };
    }
}
