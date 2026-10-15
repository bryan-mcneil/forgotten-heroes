package com.forgottenheroes.engine.resolver.battle;

import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.event.GameEvent.BattleEndReason;
import com.forgottenheroes.engine.event.GameEvent.BattleEnded;
import com.forgottenheroes.engine.event.GameEvent.BattleStarted;
import com.forgottenheroes.engine.event.GameEvent.Clash;
import com.forgottenheroes.engine.event.GameEvent.DamageKind;
import com.forgottenheroes.engine.event.GameEvent.Knockout;
import com.forgottenheroes.engine.event.GameEvent.LineShifted;
import com.forgottenheroes.engine.event.UnitView;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.model.Rules;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.model.UnitId;
import com.forgottenheroes.engine.mutator.UnitMutator;
import com.forgottenheroes.engine.resolver.PendingTrigger;
import com.forgottenheroes.engine.resolver.TriggerDispatcher;
import com.forgottenheroes.engine.rng.GameRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * The clash loop of GDD §4.5. Both fronts hit at the same time, computed from the stats before the
 * hit; the hurt, the knockouts and the falls are then resolved in the §4.6 order by the trigger
 * dispatcher; the lines close up; repeat. A battle never hangs: after {@code CLASH_CAP} clashes it is a
 * draw. The bands handed in are copied, never changed.
 */
public final class BattleResolver {

    private final TriggerDispatcher dispatcher;

    public BattleResolver(ContentIndex content) {
        this.dispatcher = new TriggerDispatcher(content);
    }

    /** A battle where the home band is the player's, on turn 1: the shape tests use. */
    public BattleResult resolve(Band home, Band away, long seed) {
        return resolve(home, away, seed, 1, Side.HOME);
    }

    public BattleResult resolve(Band home, Band away, long seed, int turn, Side playerSide) {
        BattleState state = new BattleState(home, away);
        GameRandom rng = new GameRandom(seed).fork("abilities");
        EventLog log = new EventLog();
        log.append(new BattleStarted(0, turn, playerSide, views(state.band(Side.HOME)), views(state.band(Side.AWAY))));

        fire(state, rng, log, List.of(PendingTrigger.of(Trigger.START_OF_BATTLE)));
        while (state.bothAlive() && state.clashes() < Rules.CLASH_CAP) {
            clash(state, rng, log);
        }

        BattleEndReason reason = state.bothAlive() ? BattleEndReason.CLASH_CAP : BattleEndReason.ELIMINATION;
        Outcome homeOutcome = outcome(state);
        Side side = playerSide;
        Outcome playerOutcome = side == Side.HOME ? homeOutcome : homeOutcome.flipped();
        log.append(new BattleEnded(0, homeOutcome, playerOutcome, reason, state.clashes()));
        return new BattleResult(log, homeOutcome, playerSide, reason, state.clashes());
    }

    /** One exchange between the two fronts, then everything it causes. */
    private void clash(BattleState state, GameRandom rng, EventLog log) {
        Band home = state.band(Side.HOME);
        Band away = state.band(Side.AWAY);
        Unit a = home.front();
        Unit b = away.front();
        log.append(new Clash(0, state.nextClash(), a.id(), b.id()));
        fire(state, rng, log, List.of(PendingTrigger.of(Trigger.BEFORE_ATTACK, a, Side.HOME),
            PendingTrigger.of(Trigger.BEFORE_ATTACK, b, Side.AWAY)));
        if (!state.bothAlive()) {
            return;
        }
        a = home.front();
        b = away.front();

        Damage.Hit toB = Damage.attack(a, b);
        Damage.Hit toA = Damage.attack(b, a);
        UnitMutator.damage(away, Side.AWAY, b, log, toB.amount(), a.id(), UnitMutator.positionOf(home, Side.HOME, a), DamageKind.ATTACK);
        UnitMutator.damage(home, Side.HOME, a, log, toA.amount(), b.id(), UnitMutator.positionOf(away, Side.AWAY, b), DamageKind.ATTACK);

        List<PendingTrigger> moments = new ArrayList<>();
        if (toA.amount() > 0) {
            moments.add(PendingTrigger.of(Trigger.HURT, a, Side.HOME));
        }
        if (toB.amount() > 0) {
            moments.add(PendingTrigger.of(Trigger.HURT, b, Side.AWAY));
        }
        if (!b.isAlive() && toB.amount() > 0) {
            log.append(new Knockout(0, a.id(), a.heroId(), UnitMutator.positionOf(home, Side.HOME, a), b.id()));
            moments.add(PendingTrigger.of(Trigger.KNOCKOUT, a, Side.HOME));
        }
        if (!a.isAlive() && toA.amount() > 0) {
            log.append(new Knockout(0, b.id(), b.heroId(), UnitMutator.positionOf(away, Side.AWAY, b), a.id()));
            moments.add(PendingTrigger.of(Trigger.KNOCKOUT, b, Side.AWAY));
        }
        fire(state, rng, log, moments);

        List<PendingTrigger> after = new ArrayList<>();
        if (a.isAlive() && home.contains(a.id())) {
            after.add(PendingTrigger.of(Trigger.ALLY_AHEAD_ATTACKS, a, Side.HOME));
            after.add(PendingTrigger.of(Trigger.AFTER_ATTACK, a, Side.HOME));
        }
        if (b.isAlive() && away.contains(b.id())) {
            after.add(PendingTrigger.of(Trigger.ALLY_AHEAD_ATTACKS, b, Side.AWAY));
            after.add(PendingTrigger.of(Trigger.AFTER_ATTACK, b, Side.AWAY));
        }
        fire(state, rng, log, after);
    }

    /**
     * Hand the moments to the dispatcher (which also settles every fall they cause) and report the
     * lines that closed up afterwards.
     */
    private void fire(BattleState state, GameRandom rng, EventLog log, List<PendingTrigger> moments) {
        List<UnitId> homeBefore = order(state.band(Side.HOME));
        List<UnitId> awayBefore = order(state.band(Side.AWAY));
        dispatcher.dispatch(state, rng, log, moments);
        shifted(state, log, Side.HOME, homeBefore);
        shifted(state, log, Side.AWAY, awayBefore);
    }

    private static void shifted(BattleState state, EventLog log, Side side, List<UnitId> before) {
        List<UnitId> after = order(state.band(side));
        if (!after.equals(before)) {
            log.append(new LineShifted(0, side, after));
        }
    }

    private static Outcome outcome(BattleState state) {
        boolean homeAlive = state.band(Side.HOME).anyAlive();
        boolean awayAlive = state.band(Side.AWAY).anyAlive();
        if (homeAlive == awayAlive) {
            return Outcome.DRAW;
        }
        return homeAlive ? Outcome.WIN : Outcome.LOSS;
    }

    private static List<UnitId> order(Band band) {
        return band.units().stream().map(Unit::id).toList();
    }

    private static List<UnitView> views(Band band) {
        return band.units().stream().map(UnitView::of).toList();
    }
}
