package com.forgottenheroes.engine.properties;

import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.content.HeroDefinition;
import com.forgottenheroes.engine.event.EventJson;
import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.event.GameEvent.BattleEnded;
import com.forgottenheroes.engine.json.RunStateJson;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Rules;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.Tavern;
import com.forgottenheroes.engine.model.TavernSlot;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.model.UnitId;
import com.forgottenheroes.engine.model.Xp;
import com.forgottenheroes.engine.resolver.RuleViolation;
import com.forgottenheroes.engine.resolver.battle.BattleResolver;
import com.forgottenheroes.engine.resolver.battle.BattleResult;
import com.forgottenheroes.engine.session.GameSession;
import com.forgottenheroes.engine.testing.TestFixtures;
import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

/**
 * Properties (quality strategy §3): facts that must hold for every seed and every action list,
 * not just the examples. jqwik generates the inputs and shrinks a failure to its smallest case.
 */
class EnginePropertiesTest {

    private static final GameSession SESSION = new GameSession(TestFixtures.content());
    private static final BattleResolver BATTLE = new BattleResolver(TestFixtures.content());
    private static final List<String> HERO_IDS = TestFixtures.content().heroes().stream().map(HeroDefinition::id).toList();

    /** Plays the actions through the session, keeping every event; refusals are swallowed, bugs are not. */
    private static List<GameEvent> play(RunState state, List<Action> actions, List<RunState> snapshots) {
        List<GameEvent> events = new ArrayList<>();
        for (Action action : actions) {
            if (state.status().isOver()) {
                break;
            }
            try {
                events.addAll(action instanceof Action.EndTurn
                    ? SESSION.endTurn(state, state.toSnapshot()).events()
                    : SESSION.apply(state, action).events());
                if (snapshots != null) {
                    snapshots.add(state.copy());
                }
            } catch (RuleViolation refused) {
                // the rules said no: fine
            }
        }
        return events;
    }

    @Property(tries = 150)
    void sameSeedAndActionsGiveTheSameEventJson(@ForAll long seed, @ForAll long actionSeed, @ForAll @IntRange(min = 1, max = 120) int steps) {
        List<Action> actions = RandomActions.sequence(actionSeed, steps);
        List<GameEvent> first = new ArrayList<>(SESSION.start("p", seed).events());
        RunState one = SESSION.start("p", seed).state();
        first.addAll(play(one, actions, null));
        List<GameEvent> second = new ArrayList<>(SESSION.start("p", seed).events());
        RunState two = SESSION.start("p", seed).state();
        second.addAll(play(two, actions, null));
        assertThat(EventJson.toJson(first)).isEqualTo(EventJson.toJson(second));
        assertThat(one).isEqualTo(two);
    }

    @Property(tries = 150)
    void invariantsHoldAfterEveryAcceptedAction(@ForAll long seed, @ForAll long actionSeed, @ForAll @IntRange(min = 1, max = 150) int steps) {
        RunState state = SESSION.start("p", seed).state();
        for (Action action : RandomActions.sequence(actionSeed, steps)) {
            if (state.status().isOver()) {
                break;
            }
            List<TavernSlot> frozenBefore = state.tavern().allHeroSlots().stream().filter(TavernSlot::frozen).toList();
            try {
                if (action instanceof Action.EndTurn) {
                    SESSION.endTurn(state, state.toSnapshot());
                } else {
                    SESSION.apply(state, action);
                }
            } catch (RuleViolation refused) {
                continue;
            }
            state.assertInvariants();
            assertThat(state.gold()).isBetween(0, Rules.MAX_GOLD);
            assertThat(state.band().size()).isLessThanOrEqualTo(Rules.BAND_SIZE);
            assertThat(state.band().units()).allMatch(unit -> unit.health() > 0);
            if (!state.status().isOver()) {
                assertThat(state.tavern().heroSlots()).hasSize(Tavern.heroSlotsFor(state.turn()));
            }
            if (action instanceof Action.Roll) {
                assertThat(state.tavern().allHeroSlots().stream().filter(TavernSlot::frozen).toList()).isEqualTo(frozenBefore);
            }
        }
    }

    @Property(tries = 150)
    void rejectedActionsLeaveTheStateUntouched(@ForAll long seed, @ForAll long actionSeed, @ForAll @IntRange(min = 1, max = 120) int steps) {
        RunState state = SESSION.start("p", seed).state();
        int refusals = 0;
        for (Action action : RandomActions.sequence(actionSeed, steps)) {
            if (state.status().isOver() || action instanceof Action.EndTurn) {
                if (!state.status().isOver()) {
                    SESSION.endTurn(state, state.toSnapshot());
                }
                continue;
            }
            RunState before = state.copy();
            try {
                SESSION.apply(state, action);
            } catch (RuleViolation refused) {
                refusals++;
                assertThat(state).isEqualTo(before);
                assertThat(state.version()).isEqualTo(before.version());
            }
        }
        assertThat(refusals).isGreaterThanOrEqualTo(0);
    }

    @Property(tries = 200)
    void battlesTerminateUnderTheCap(@ForAll("bands") Band home, @ForAll("bands") Band away, @ForAll long seed) {
        Band homeBefore = home.copy();
        Band awayBefore = away.copy();
        BattleResult result = BATTLE.resolve(home, away, seed);
        assertThat(result.clashes()).isLessThanOrEqualTo(Rules.CLASH_CAP);
        assertThat(result.events().events().getLast()).isInstanceOf(BattleEnded.class);
        assertThat(home).isEqualTo(homeBefore);
        assertThat(away).isEqualTo(awayBefore);
    }

    @Property(tries = 150)
    void runStateRoundTripsThroughJson(@ForAll long seed, @ForAll long actionSeed, @ForAll @IntRange(min = 0, max = 100) int steps) {
        RunState state = SESSION.start("p", seed).state();
        play(state, RandomActions.sequence(actionSeed, steps), null);
        String json = RunStateJson.toJson(state);
        RunState back = RunStateJson.fromJson(json);
        assertThat(back).isEqualTo(state);
        assertThat(RunStateJson.toJson(back)).isEqualTo(json);
    }

    @Provide
    Arbitrary<Band> bands() {
        Arbitrary<Unit> unit = Combinators.combine(
                Arbitraries.of(HERO_IDS),
                Arbitraries.integers().between(0, Rules.MAX_STAT),
                Arbitraries.integers().between(1, Rules.MAX_STAT),
                Arbitraries.integers().between(0, Xp.MAX),
                Arbitraries.of(Perk.values()).injectNull(0.5))
            .as((hero, attack, health, xp, perk) -> {
                Unit built = new Unit(new UnitId("x"), hero, new Stats(attack, health));
                built.gainXp(xp);
                if (perk != null) {
                    built.setPerk(perk);
                }
                return built;
            });
        return unit.list().ofMinSize(1).ofMaxSize(Rules.BAND_SIZE).map(units -> {
            Band band = new Band();
            int n = 1;
            for (Unit u : units) {
                band.add(Unit.restore(new UnitId("x" + n++), u.heroId(), u.base(), Stats.ZERO, Stats.ZERO, u.xp(), u.perk().orElse(null),
                    u.counters(), 0));
            }
            return band;
        });
    }
}
