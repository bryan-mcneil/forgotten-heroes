package com.forgottenheroes.engine.resolver;

import com.forgottenheroes.engine.content.AbilityDefinition;
import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.Limit;
import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.mutator.BandMutator;
import com.forgottenheroes.engine.mutator.UnitMutator;
import com.forgottenheroes.engine.resolver.RuleViolation.Code;
import com.forgottenheroes.engine.rng.GameRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * Fires abilities when their moment comes (GDD §4.6). A moment is a trigger plus the unit it is
 * about; the dispatcher finds the units that may answer it, drops the ones whose limit is spent,
 * sorts them (higher attack, then higher health, then home before away, then front first) and runs
 * each. Moments caused by an ability (a summon, a level-up) go to the back of a queue, never into a
 * recursive call. After every moment a settle pass processes units at zero health: their Fall
 * ability fires while they still hold their place, then their friends' Ally-fall abilities, then
 * they leave and any summons take the slot. A guard stops runaway cascades.
 */
public final class TriggerDispatcher {

    /** More activations than this in one dispatch means content is looping; the engine refuses to hang. */
    public static final int MAX_ACTIVATIONS = 1000;

    private final ContentIndex content;
    private final AbilityInterpreter interpreter;

    public TriggerDispatcher(ContentIndex content) {
        this.content = content;
        this.interpreter = new AbilityInterpreter(content);
    }

    /** One dispatch: fire the given moments and everything they cause, settling falls as they happen. */
    public void dispatch(Board board, GameRandom rng, EventLog log, List<PendingTrigger> moments) {
        Run run = new Run(board, rng, log);
        run.queue.addAll(moments);
        run.settle();
        while (!run.queue.isEmpty()) {
            PendingTrigger moment = run.queue.poll();
            run.fire(moment);
            run.settle();
        }
    }

    /** The order several eligible abilities resolve in: attack, health, home first, front first. */
    static Comparator<Candidate> ordering() {
        return Comparator.comparingInt((Candidate c) -> c.unit().attack()).reversed()
            .thenComparing(Comparator.comparingInt((Candidate c) -> c.unit().health()).reversed())
            .thenComparing(c -> c.side() == Side.HOME ? 0 : 1)
            .thenComparingInt(Candidate::index);
    }

    record Candidate(Unit unit, Side side, int index, AbilityDefinition ability) { }

    /** The state of one dispatch: the queue of moments and the activation count. */
    private final class Run {

        private final Board board;
        private final GameRandom rng;
        private final EventLog log;
        private final Deque<PendingTrigger> queue = new ArrayDeque<>();
        private int activations;

        Run(Board board, GameRandom rng, EventLog log) {
            this.board = board;
            this.rng = rng;
            this.log = log;
        }

        void fire(PendingTrigger moment) {
            fire(moment.trigger(), moment.subject(), moment.side(), -1, null);
        }

        /** Run every eligible ability for a moment now, in order. */
        void fire(Trigger trigger, Unit subject, Side subjectSide, int summonIndex, List<Unit> deferredSummons) {
            List<Candidate> candidates = candidates(trigger, subject, subjectSide);
            candidates.sort(ordering());
            for (Candidate candidate : candidates) {
                if (!stillEligible(candidate, trigger)) {
                    continue;
                }
                if (++activations > MAX_ACTIVATIONS) {
                    throw new RuleViolation(Code.ENGINE_GUARD, "more than " + MAX_ACTIVATIONS + " ability activations in one action");
                }
                candidate.unit().counters().recordUse();
                int landing = summonIndex >= 0 ? summonIndex : candidate.index() + 1;
                List<PendingTrigger> caused = new ArrayList<>();
                interpreter.run(candidate.unit(), candidate.ability(), board, candidate.side(), subject, rng, log, caused, landing,
                    deferredSummons);
                queue.addAll(caused);
            }
        }

        /** Who may answer a moment (GDD §7): the unit itself, its friends, or everybody, depending on the trigger. */
        private List<Candidate> candidates(Trigger trigger, Unit subject, Side subjectSide) {
            List<Candidate> found = new ArrayList<>();
            switch (trigger) {
                case START_OF_TURN, END_OF_TURN -> collect(found, Side.HOME, trigger, null, true);
                case START_OF_BATTLE -> {
                    collect(found, Side.HOME, trigger, null, true);
                    collect(found, Side.AWAY, trigger, null, true);
                }
                case BUY, SELL, LEVEL_UP, EAT_RELIC, SUMMONED, HURT, FALL, BEFORE_ATTACK, AFTER_ATTACK, KNOCKOUT ->
                    self(found, subject, subjectSide, trigger);
                case ALLY_SUMMONED, ALLY_FALL -> collect(found, subjectSide, trigger, subject, true);
                case ALLY_AHEAD_FALLS, ALLY_AHEAD_ATTACKS -> behind(found, subject, subjectSide, trigger);
            }
            return found;
        }

        private void self(List<Candidate> found, Unit subject, Side side, Trigger trigger) {
            if (subject == null) {
                return;
            }
            interpreter.abilityOf(subject).filter(ability -> ability.trigger() == trigger).ifPresent(ability -> {
                int index = board.hasSide(side) ? board.band(side).indexOf(subject) : -1;
                found.add(new Candidate(subject, side, Math.max(index, 0), ability));
            });
        }

        private void collect(List<Candidate> found, Side side, Trigger trigger, Unit except, boolean aliveOnly) {
            if (!board.hasSide(side)) {
                return;
            }
            Band band = board.band(side);
            for (int i = 0; i < band.size(); i++) {
                Unit unit = band.at(i);
                if (unit == except || (aliveOnly && !unit.isAlive())) {
                    continue;
                }
                int index = i;
                interpreter.abilityOf(unit).filter(ability -> ability.trigger() == trigger)
                    .ifPresent(ability -> found.add(new Candidate(unit, side, index, ability)));
            }
        }

        private void behind(List<Candidate> found, Unit subject, Side side, Trigger trigger) {
            if (subject == null || !board.hasSide(side)) {
                return;
            }
            Band band = board.band(side);
            int index = band.indexOf(subject);
            if (index < 0 || index + 1 >= band.size()) {
                return;
            }
            Unit unit = band.at(index + 1);
            if (unit.isAlive()) {
                interpreter.abilityOf(unit).filter(ability -> ability.trigger() == trigger)
                    .ifPresent(ability -> found.add(new Candidate(unit, side, index + 1, ability)));
            }
        }

        /** Earlier abilities in the same batch may have killed or spent this one. */
        private boolean stillEligible(Candidate candidate, Trigger trigger) {
            Unit unit = candidate.unit();
            if (trigger != Trigger.FALL && trigger != Trigger.SELL && !unit.isAlive()) {
                return false;
            }
            Limit limit = candidate.ability().limit();
            if (limit.perTurn() != null && unit.counters().usesThisTurn() >= limit.perTurn().resolve(unit.level(), unit.attack())) {
                return false;
            }
            return limit.perBattle() == null
                || unit.counters().usesThisBattle() < limit.perBattle().resolve(unit.level(), unit.attack());
        }

        /** Process every unit at zero health, highest attack first, until nobody else falls. */
        void settle() {
            while (true) {
                List<Candidate> fallen = new ArrayList<>();
                for (Side side : Side.values()) {
                    if (!board.hasSide(side)) {
                        continue;
                    }
                    Band band = board.band(side);
                    for (int i = 0; i < band.size(); i++) {
                        if (!band.at(i).isAlive()) {
                            fallen.add(new Candidate(band.at(i), side, i, null));
                        }
                    }
                }
                if (fallen.isEmpty()) {
                    return;
                }
                fallen.sort(ordering());
                processFall(fallen.getFirst().unit(), fallen.getFirst().side());
            }
        }

        /**
         * GDD §4.5: Fall ability (unit still in place), Phoenix Feather, friends' Ally-fall abilities,
         * then the unit leaves and its summons take the slot.
         */
        private void processFall(Unit unit, Side side) {
            Band band = board.band(side);
            int index = band.indexOf(unit);
            UnitMutator.fell(band, side, unit, log);
            List<Unit> deferred = new ArrayList<>();
            fire(Trigger.FALL, unit, side, index, deferred);
            if (unit.hasPerk(Perk.FAIRY_CHARM)) {
                deferred.add(Unit.fromToken(content.token("sprite"), board.nextUnitId(), new Stats(1, 1)));
            }
            boolean revived = false;
            if (unit.hasPerk(Perk.PHOENIX_FEATHER)) {
                UnitMutator.consumePerk(band, side, unit, log);
                UnitMutator.revive(band, side, unit, log, new Stats(1, 1));
                revived = true;
            }
            int landing = revived ? index + 1 : index;
            fire(Trigger.ALLY_FALL, unit, side, landing, deferred);
            fire(Trigger.ALLY_AHEAD_FALLS, unit, side, landing, deferred);
            if (!revived) {
                band.remove(unit);
            }
            for (Unit summoned : deferred) {
                if (band.isFull()) {
                    log.append(new com.forgottenheroes.engine.event.GameEvent.SummonFailed(0, summoned.heroId(), side, unit.id()));
                    continue;
                }
                int at = Math.min(landing, band.size());
                BandMutator.summon(band, side, at, summoned, log, unit.id());
                queue.add(PendingTrigger.of(Trigger.ALLY_SUMMONED, summoned, side));
                queue.add(PendingTrigger.of(Trigger.SUMMONED, summoned, side));
                landing = at + 1;
            }
        }
    }
}
