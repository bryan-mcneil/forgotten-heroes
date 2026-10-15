package com.forgottenheroes.engine.session;

import static com.forgottenheroes.engine.testing.TestFixtures.content;
import static com.forgottenheroes.engine.testing.TestFixtures.unit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.event.EventJson;
import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.event.GameEvent.BattleEnded;
import com.forgottenheroes.engine.event.GameEvent.BattleStarted;
import com.forgottenheroes.engine.event.GameEvent.CrownsChanged;
import com.forgottenheroes.engine.event.GameEvent.HeartsChanged;
import com.forgottenheroes.engine.event.GameEvent.RunEnded;
import com.forgottenheroes.engine.event.GameEvent.RunStarted;
import com.forgottenheroes.engine.event.GameEvent.TurnEnded;
import com.forgottenheroes.engine.event.GameEvent.TurnStarted;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.RunStatus;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.TavernSlot;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.resolver.RuleViolation;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameSessionTest {

    private static final GameSession SESSION = new GameSession(content());

    private static BandSnapshot unbeatable() {
        return BandSnapshot.ofUnits(List.of(unit("x", 50, 50), unit("x", 50, 50), unit("x", 50, 50), unit("x", 50, 50), unit("x", 50, 50)));
    }

    private static void assertContiguous(List<GameEvent> events, int from) {
        for (int i = 0; i < events.size(); i++) {
            assertThat(events.get(i).seq()).as("event #" + i).isEqualTo(from + i);
        }
    }

    @Test
    void startBeginsTurnOne() {
        ActionResult started = SESSION.start("run-1", 42);
        RunState state = started.state();
        assertThat(state.turn()).isEqualTo(1);
        assertThat(state.gold()).isEqualTo(10);
        assertThat(state.version()).isEqualTo(1);
        assertThat(started.events().getFirst()).isInstanceOf(RunStarted.class);
        assertContiguous(started.events(), 1);
        assertThat(state.lastEventSeq()).isEqualTo(started.events().size());
        state.assertInvariants();
    }

    @Test
    void applyRunsATavernActionAndBumpsTheVersion() {
        RunState state = SESSION.start("run-1", 42).state();
        ActionResult bought = SESSION.apply(state, new Action.Buy(0, 0));
        assertThat(bought.state()).isSameAs(state);
        assertThat(state.band().size()).isEqualTo(1);
        assertThat(state.version()).isEqualTo(2);
        assertContiguous(bought.events(), 5);
    }

    @Test
    void clientsCannotStartOrEndTurnsThroughApply() {
        RunState state = SESSION.start("run-1", 42).state();
        RunState before = state.copy();
        assertThatThrownBy(() -> SESSION.apply(state, new Action.StartTurn())).isInstanceOf(RuleViolation.class).hasMessageContaining("WRONG_PHASE");
        assertThatThrownBy(() -> SESSION.apply(state, new Action.EndTurn())).isInstanceOf(RuleViolation.class).hasMessageContaining("WRONG_PHASE");
        assertThat(state).isEqualTo(before);
    }

    @Test
    void endingATurnAgainstAnEmptyGhostIsAWinAndStartsTheNextTurn() {
        RunState state = SESSION.start("run-1", 42).state();
        SESSION.apply(state, new Action.Buy(0, 0));
        int seqBefore = state.lastEventSeq();
        TurnResult result = SESSION.endTurn(state, BandSnapshot.EMPTY);
        assertThat(result.outcome()).isEqualTo(Outcome.WIN);
        assertThat(state.crowns()).isEqualTo(1);
        assertThat(state.hearts()).isEqualTo(5);
        assertThat(state.turn()).isEqualTo(2);
        assertThat(state.gold()).isEqualTo(10);
        assertThat(state.version()).isEqualTo(3);
        assertThat(state.lastBattle().outcome()).isEqualTo(Outcome.WIN);
        assertThat(result.ghost().size()).isEqualTo(1);
        assertThat(result.events()).extracting(GameEvent::getClass)
            .containsSubsequence(TurnEnded.class, BattleStarted.class, BattleEnded.class, CrownsChanged.class, TurnStarted.class);
        assertContiguous(result.events(), seqBefore + 1);
        assertThat(result.battle().clashes()).isZero();
        state.assertInvariants();
    }

    @Test
    void thePlayerIsHomeOnOddTurnsAndAwayOnEvenTurns() {
        RunState state = SESSION.start("run-1", 7).state();
        SESSION.apply(state, new Action.Buy(0, 0));
        TurnResult first = SESSION.endTurn(state, BandSnapshot.EMPTY);
        TurnResult second = SESSION.endTurn(state, BandSnapshot.EMPTY);
        assertThat(first.battle().playerSide()).isEqualTo(Side.HOME);
        assertThat(second.battle().playerSide()).isEqualTo(Side.AWAY);
        assertThat(second.outcome()).isEqualTo(Outcome.WIN);
        assertThat(second.events().stream().filter(BattleStarted.class::isInstance).map(BattleStarted.class::cast).findFirst().orElseThrow().playerSide())
            .isEqualTo(Side.AWAY);
    }

    @Test
    void tenCrownsWinTheRun() {
        RunState state = SESSION.start("run-1", 42).state();
        SESSION.apply(state, new Action.Buy(0, 0));
        TurnResult last = null;
        for (int turn = 1; turn <= 10; turn++) {
            last = SESSION.endTurn(state, BandSnapshot.EMPTY);
        }
        assertThat(state.status()).isEqualTo(RunStatus.WON);
        assertThat(state.crowns()).isEqualTo(10);
        assertThat(state.turn()).isEqualTo(10);
        assertThat(last.events().getLast()).isInstanceOf(RunEnded.class);
        assertThat(last.events().stream().noneMatch(TurnStarted.class::isInstance)).isTrue();
        assertThatThrownBy(() -> SESSION.apply(state, new Action.Roll())).isInstanceOf(RuleViolation.class).hasMessageContaining("RUN_OVER");
        assertThatThrownBy(() -> SESSION.endTurn(state, BandSnapshot.EMPTY)).isInstanceOf(RuleViolation.class).hasMessageContaining("RUN_OVER");
    }

    @Test
    void zeroHeartsLoseTheRunWithTheMercyHeartOnTheWay() {
        RunState state = SESSION.start("run-1", 42).state();
        SESSION.apply(state, new Action.Buy(0, 0));
        List<Integer> hearts = new ArrayList<>();
        while (!state.status().isOver()) {
            TurnResult result = SESSION.endTurn(state, unbeatable());
            assertThat(result.outcome()).isEqualTo(Outcome.LOSS);
            hearts.add(state.hearts());
        }
        assertThat(hearts).containsExactly(4, 4, 3, 2, 1, 0);
        assertThat(state.status()).isEqualTo(RunStatus.LOST);
        assertThat(state.turn()).isEqualTo(6);
        assertThat(state.hearts()).isZero();
    }

    @Test
    void temporaryStatsAreClearedAfterTheBattle() {
        RunState state = SESSION.start("run-1", 42).state();
        SESSION.apply(state, new Action.Buy(0, 0));
        Unit hero = state.band().front();
        state.tavern().setRelicSlot(0, TavernSlot.relic("battle_brew", 3));
        SESSION.apply(state, new Action.BuyRelic(0, 0));
        assertThat(hero.temporaryBonus()).isEqualTo(new Stats(3, 3));
        TurnResult result = SESSION.endTurn(state, BandSnapshot.EMPTY);
        assertThat(result.battle().events().of(BattleStarted.class).getFirst().home().getFirst().attack()).isEqualTo(hero.base().attack() + 3);
        assertThat(hero.temporaryBonus()).isEqualTo(Stats.ZERO);
        assertThat(hero.damageTaken()).isZero();
    }

    @Test
    void theRunStateIsNotTouchedByTheBattleCopies() {
        RunState state = SESSION.start("run-1", 42).state();
        SESSION.apply(state, new Action.Buy(0, 0));
        Unit hero = state.band().front();
        SESSION.endTurn(state, unbeatable());
        assertThat(hero.isAlive()).isTrue();
        assertThat(state.band().units()).containsExactly(hero);
    }

    @Test
    void aFullRunAgainstAMirrorIsDeterministicAndContiguous() {
        List<GameEvent> first = playTwelveTurns(2024);
        List<GameEvent> second = playTwelveTurns(2024);
        assertThat(EventJson.toJson(first)).isEqualTo(EventJson.toJson(second));
        assertContiguous(first, 1);
        assertThat(first.size()).isGreaterThan(100);
        assertThat(EventJson.toJson(playTwelveTurns(2025))).isNotEqualTo(EventJson.toJson(first));
    }

    private static List<GameEvent> playTwelveTurns(long seed) {
        List<GameEvent> all = new ArrayList<>();
        ActionResult started = SESSION.start("run-" + seed, seed);
        RunState state = started.state();
        all.addAll(started.events());
        int calls = 1;
        for (int turn = 1; turn <= 12 && !state.status().isOver(); turn++) {
            while (state.gold() >= 3 && !state.band().isFull()) {
                int slot = firstFullHeroSlot(state);
                if (slot < 0) {
                    break;
                }
                all.addAll(SESSION.apply(state, new Action.Buy(slot, state.band().size())).events());
                calls++;
            }
            state.assertInvariants();
            all.addAll(SESSION.endTurn(state, state.toSnapshot()).events());
            calls++;
            assertThat(state.version()).isEqualTo(calls);
        }
        return all;
    }

    private static int firstFullHeroSlot(RunState state) {
        for (int i = 0; i < state.tavern().heroSlotCount(); i++) {
            if (!state.tavern().heroSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    @Test
    void snapshotsKeepWhatAGhostNeedsAndRoundTripAsJson() {
        Unit veteran = unit("aoi", 2, 2, 2);
        veteran.addPermanentBonus(new Stats(2, 2));
        veteran.addTemporaryBonus(new Stats(1, 0));
        veteran.setPerk(Perk.WARD);
        veteran.takeDamage(1);
        BandSnapshot snapshot = BandSnapshot.of(Band.of(veteran, unit("bard", 2, 1)));
        assertThat(snapshot.units().getFirst()).isEqualTo(new UnitSnapshot("aoi", 5, 4, 2, Perk.WARD));
        assertThat(snapshot.power()).isEqualTo(5 + 4 + 2 + 1);
        Band ghost = snapshot.toBand("g");
        assertThat(ghost.front().id().value()).isEqualTo("g1");
        assertThat(ghost.front().stats()).isEqualTo(new Stats(5, 4));
        assertThat(ghost.front().level()).isEqualTo(2);
        assertThat(ghost.front().perk()).contains(Perk.WARD);
        String json = EventJson.mapper().writeValueAsString(snapshot);
        assertThat(EventJson.mapper().readValue(json, BandSnapshot.class)).isEqualTo(snapshot);
        assertThat(BandSnapshot.EMPTY.isEmpty()).isTrue();
        assertThatThrownBy(() -> new UnitSnapshot(" ", 1, 1, 0, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
