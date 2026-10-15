package com.forgottenheroes.engine.action;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.forgottenheroes.engine.model.ItemKind;

/**
 * What a player (or the engine itself) wants to do. An action is only an intent: the resolver
 * validates it and either applies it, emitting events, or throws a {@code RuleViolation} and
 * leaves the state untouched. The JSON {@code "type"} field is the record's name, so replays and
 * bug reports can list actions as data.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(Action.StartTurn.class),
    @JsonSubTypes.Type(Action.Roll.class),
    @JsonSubTypes.Type(Action.Freeze.class),
    @JsonSubTypes.Type(Action.Unfreeze.class),
    @JsonSubTypes.Type(Action.Buy.class),
    @JsonSubTypes.Type(Action.BuyRelic.class),
    @JsonSubTypes.Type(Action.Sell.class),
    @JsonSubTypes.Type(Action.Move.class),
    @JsonSubTypes.Type(Action.Merge.class),
    @JsonSubTypes.Type(Action.EndTurn.class)
})
public sealed interface Action {

    /** Engine-internal: begin the next turn (gold, tavern, free roll). The session applies it, never a client. */
    record StartTurn() implements Action { }

    /** Refill the unfrozen shelves; free once per turn, then 1 gold. */
    record Roll() implements Action { }

    /** Keep a shelf item through rolls and the next start of turn. */
    record Freeze(ItemKind kind, int slot) implements Action {
        public Freeze {
            requireKind(kind);
            requireIndex(slot, "slot");
        }
    }

    /** Let a frozen shelf item be replaced again. */
    record Unfreeze(ItemKind kind, int slot) implements Action {
        public Unfreeze {
            requireKind(kind);
            requireIndex(slot, "slot");
        }
    }

    /** Buy the hero on a shelf and put it at a band index (equal to the band size appends; onto a copy merges). */
    record Buy(int slot, int bandIndex) implements Action {
        public Buy {
            requireIndex(slot, "slot");
            requireIndex(bandIndex, "bandIndex");
        }
    }

    /** Buy a relic; {@code bandIndex} names the hero it is used on, or is null for relics that pick their own targets. */
    record BuyRelic(int slot, Integer bandIndex) implements Action {
        public BuyRelic {
            requireIndex(slot, "slot");
            if (bandIndex != null) {
                requireIndex(bandIndex, "bandIndex");
            }
        }
    }

    /** Sell the hero at a band index for gold equal to its level. */
    record Sell(int bandIndex) implements Action {
        public Sell {
            requireIndex(bandIndex, "bandIndex");
        }
    }

    /** Move a hero from one band index to another; the others close up. */
    record Move(int from, int to) implements Action {
        public Move {
            requireIndex(from, "from");
            requireIndex(to, "to");
        }
    }

    /** Drop the hero at {@code from} onto the same hero at {@code to}: one unit, more XP. */
    record Merge(int from, int to) implements Action {
        public Merge {
            requireIndex(from, "from");
            requireIndex(to, "to");
            if (from == to) {
                throw new IllegalArgumentException("a hero cannot merge with itself");
            }
        }
    }

    /** Leave the tavern: the battle follows, then the next turn. Resolved by {@code GameSession.endTurn}. */
    record EndTurn() implements Action { }

    private static void requireIndex(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative: " + value);
        }
    }

    private static void requireKind(ItemKind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind is required");
        }
    }
}
