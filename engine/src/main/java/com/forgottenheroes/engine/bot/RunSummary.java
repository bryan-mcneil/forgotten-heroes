package com.forgottenheroes.engine.bot;

import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.model.RunStatus;
import com.forgottenheroes.engine.session.BandSnapshot;
import java.util.List;
import java.util.Map;

/**
 * What one bot run amounted to: how it ended, how long it took, every battle with the band that
 * fought it, and how much gold abilities earned per hero. Small enough to keep ten thousand of.
 */
public record RunSummary(long seed, String bot, RunStatus status, int turns, int crowns, int hearts, int actions, int violations,
                         List<TurnRecord> battles, Map<String, Integer> goldFromAbilities, int events) {

    /** One turn's battle: the outcome for the bot, its length, and the band the bot sent in. */
    public record TurnRecord(int turn, Outcome outcome, int clashes, BandSnapshot band) { }

    public RunSummary {
        battles = List.copyOf(battles);
        goldFromAbilities = Map.copyOf(goldFromAbilities);
    }

    public long wins() {
        return battles.stream().filter(record -> record.outcome() == Outcome.WIN).count();
    }

    public long losses() {
        return battles.stream().filter(record -> record.outcome() == Outcome.LOSS).count();
    }

    public long draws() {
        return battles.stream().filter(record -> record.outcome() == Outcome.DRAW).count();
    }
}
