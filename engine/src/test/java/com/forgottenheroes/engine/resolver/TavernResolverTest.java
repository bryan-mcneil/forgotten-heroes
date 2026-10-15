package com.forgottenheroes.engine.resolver;

import static com.forgottenheroes.engine.testing.TestFixtures.content;
import static com.forgottenheroes.engine.testing.TestFixtures.newRun;
import static com.forgottenheroes.engine.testing.TestFixtures.resolver;
import static com.forgottenheroes.engine.testing.TestFixtures.runAtTurn;
import static com.forgottenheroes.engine.testing.TestFixtures.unit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.event.GameEvent.GoldChanged;
import com.forgottenheroes.engine.event.GameEvent.HeartsChanged;
import com.forgottenheroes.engine.event.GameEvent.HeartsReason;
import com.forgottenheroes.engine.event.GameEvent.RunStarted;
import com.forgottenheroes.engine.event.GameEvent.TavernItemFrozen;
import com.forgottenheroes.engine.event.GameEvent.TavernRolled;
import com.forgottenheroes.engine.event.GameEvent.TurnStarted;
import com.forgottenheroes.engine.model.ItemKind;
import com.forgottenheroes.engine.model.Rank;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.RunStatus;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.TavernSlot;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.resolver.RuleViolation.Code;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TavernResolverTest {

    private static final Action.StartTurn START = new Action.StartTurn();
    private static final Action.Roll ROLL = new Action.Roll();

    private static RuleViolation violation(RunState state, Action action) {
        try {
            resolver().apply(state, action);
        } catch (RuleViolation e) {
            return e;
        }
        throw new AssertionError("expected a RuleViolation for " + action);
    }

    @Test
    void theFirstTurnTellsItsStory() {
        RunState state = newRun(42);
        TavernResult result = resolver().apply(state, START);
        System.out.println(result.events().story());
        assertThat(result.events().events()).extracting(GameEvent::getClass)
            .containsExactly(RunStarted.class, TurnStarted.class, GoldChanged.class, TavernRolled.class);
        assertThat(result.events().of(TurnStarted.class).getFirst()).isEqualTo(new TurnStarted(2, 1, 10, Rank.I, 3, 1));
        assertThat(result.events().of(GoldChanged.class).getFirst()).isEqualTo(new GoldChanged(3, 0, 10, GameEvent.GoldReason.TURN_START));
        assertThat(result.triggers()).containsExactly(PendingTrigger.of(Trigger.START_OF_TURN));
        assertThat(state.turn()).isEqualTo(1);
        assertThat(state.gold()).isEqualTo(10);
        assertThat(state.freeRollAvailable()).isTrue();
        assertThat(state.tavern().heroSlots()).hasSize(3).allMatch(slot -> !slot.isEmpty() && !slot.frozen());
        assertThat(state.tavern().relicSlots()).hasSize(1).allMatch(slot -> !slot.isEmpty());
        assertThat(state.lastEventSeq()).isEqualTo(4);
        state.assertInvariants();
    }

    @ParameterizedTest
    @CsvSource({
        "1, 3, 1",
        "2, 3, 1",
        "3, 3, 2",
        "4, 3, 2",
        "5, 4, 2",
        "8, 4, 2",
        "9, 5, 2",
        "12, 5, 2"
    })
    void slotCountsFollowTheTurnTable(int turn, int heroSlots, int relicSlots) {
        RunState state = runAtTurn(1, turn);
        assertThat(state.tavern().heroSlots()).hasSize(heroSlots);
        assertThat(state.tavern().relicSlots()).hasSize(relicSlots);
        state.assertInvariants();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11})
    void theTavernRankFollowsTheTurn(int turn) {
        RunState state = runAtTurn(3, turn - 1);
        TavernResult result = resolver().apply(state, START);
        Rank available = Rank.availableAt(turn);
        assertThat(result.events().of(TurnStarted.class).getFirst().rank()).isEqualTo(available);
        for (TavernSlot slot : state.tavern().heroSlots()) {
            assertThat(content().hero(slot.itemId()).rank()).isLessThanOrEqualTo(available);
        }
        for (TavernSlot slot : state.tavern().relicSlots()) {
            assertThat(content().relic(slot.itemId()).rank()).isLessThanOrEqualTo(available);
        }
    }

    @Test
    void theFirstRollIsFreeAndTheNextCostsOneGold() {
        RunState state = runAtTurn(7, 1);
        TavernResult free = resolver().apply(state, ROLL);
        assertThat(state.gold()).isEqualTo(10);
        assertThat(state.freeRollAvailable()).isFalse();
        assertThat(free.events().of(TavernRolled.class).getFirst().free()).isTrue();
        assertThat(free.events().of(GoldChanged.class)).isEmpty();

        TavernResult paid = resolver().apply(state, ROLL);
        assertThat(state.gold()).isEqualTo(9);
        assertThat(paid.events().of(GoldChanged.class).getFirst().reason()).isEqualTo(GameEvent.GoldReason.ROLL);
        assertThat(paid.events().of(TavernRolled.class).getFirst().free()).isFalse();
        assertThat(state.rollsThisTurn()).isEqualTo(2);
    }

    @Test
    void rollingWithNoGoldAndNoFreeRollChangesNothing() {
        RunState state = runAtTurn(7, 1);
        resolver().apply(state, ROLL);
        state.setGold(0);
        RunState before = state.copy();
        RuleViolation violation = violation(state, ROLL);
        assertThat(violation.code()).isEqualTo(Code.NOT_ENOUGH_GOLD);
        assertThat(violation).hasMessageContaining("NOT_ENOUGH_GOLD");
        assertThat(state).isEqualTo(before);
    }

    @Test
    void aFrozenHeroSurvivesRollsAndTheNextStartOfTurn() {
        RunState state = runAtTurn(11, 1);
        TavernSlot kept = state.tavern().heroSlot(1);
        TavernResult frozen = resolver().apply(state, new Action.Freeze(ItemKind.HERO, 1));
        assertThat(frozen.events().of(TavernItemFrozen.class).getFirst()).isEqualTo(new TavernItemFrozen(0, ItemKind.HERO, 1, kept.itemId()).withSeq(5));
        for (int i = 0; i < 3; i++) {
            resolver().apply(state, ROLL);
            assertThat(state.tavern().heroSlot(1)).isEqualTo(kept.withFrozen(true));
        }
        resolver().apply(state, START);
        assertThat(state.turn()).isEqualTo(2);
        assertThat(state.tavern().heroSlot(1)).isEqualTo(kept.withFrozen(true));
    }

    @Test
    void anUnfrozenItemIsReplacedOnTheNextRoll() {
        RunState state = runAtTurn(11, 1);
        resolver().apply(state, new Action.Freeze(ItemKind.RELIC, 0));
        resolver().apply(state, new Action.Unfreeze(ItemKind.RELIC, 0));
        assertThat(state.tavern().relicSlot(0).frozen()).isFalse();
        TavernResult result = resolver().apply(state, ROLL);
        assertThat(result.events().of(TavernRolled.class).getFirst().relics().getFirst().frozen()).isFalse();
    }

    @Test
    void theMercyHeartComesOnTurnThreeOnlyWhenAHeartIsMissing() {
        RunState hurt = runAtTurn(5, 2);
        hurt.setHearts(4);
        TavernResult result = resolver().apply(hurt, START);
        assertThat(hurt.hearts()).isEqualTo(5);
        assertThat(result.events().of(HeartsChanged.class).getFirst()).isEqualTo(new HeartsChanged(0, 4, 5, HeartsReason.MERCY).withSeq(result.events().of(HeartsChanged.class).getFirst().seq()));

        RunState whole = runAtTurn(5, 2);
        assertThat(resolver().apply(whole, START).events().of(HeartsChanged.class)).isEmpty();
        assertThat(whole.hearts()).isEqualTo(5);

        RunState earlier = runAtTurn(5, 1);
        earlier.setHearts(3);
        assertThat(resolver().apply(earlier, START).events().of(HeartsChanged.class)).isEmpty();
        assertThat(earlier.hearts()).isEqualTo(3);
    }

    @Test
    void freezeRefusesBadSlots() {
        RunState state = runAtTurn(9, 1);
        assertThat(violation(state, new Action.Freeze(ItemKind.HERO, 3)).code()).isEqualTo(Code.INVALID_INDEX);
        assertThat(violation(state, new Action.Freeze(ItemKind.RELIC, 1)).code()).isEqualTo(Code.INVALID_INDEX);
        state.tavern().setHeroSlot(0, TavernSlot.EMPTY);
        assertThat(violation(state, new Action.Freeze(ItemKind.HERO, 0)).code()).isEqualTo(Code.SLOT_EMPTY);
        resolver().apply(state, new Action.Freeze(ItemKind.HERO, 1));
        assertThat(violation(state, new Action.Freeze(ItemKind.HERO, 1)).code()).isEqualTo(Code.ALREADY_FROZEN);
        assertThat(violation(state, new Action.Unfreeze(ItemKind.HERO, 2)).code()).isEqualTo(Code.NOT_FROZEN);
    }

    @Test
    void theBonusSlotCanBeFrozenAndClosesOnAnUnfrozenRoll() {
        RunState state = runAtTurn(9, 1);
        state.tavern().openBonusSlot(TavernSlot.hero("aoi", Stats.ZERO));
        resolver().apply(state, new Action.Freeze(ItemKind.HERO, 3));
        resolver().apply(state, ROLL);
        assertThat(state.tavern().bonusSlot()).isPresent();
        resolver().apply(state, new Action.Unfreeze(ItemKind.HERO, 3));
        resolver().apply(state, ROLL);
        assertThat(state.tavern().bonusSlot()).isEmpty();
    }

    @Test
    void startOfTurnClosesTheBonusSlotAndResetsTurnCounters() {
        RunState state = runAtTurn(9, 1);
        Unit unit = unit("aoi", 2, 2);
        unit.counters().recordUse();
        state.band().add(unit);
        state.tavern().openBonusSlot(TavernSlot.hero("aoi", Stats.ZERO).withFrozen(true));
        resolver().apply(state, START);
        assertThat(state.tavern().bonusSlot()).isEmpty();
        assertThat(unit.counters().usesThisTurn()).isZero();
        assertThat(unit.counters().usesThisBattle()).isEqualTo(1);
    }

    @Test
    void nothingWorksBeforeTheFirstTurnOrAfterTheRun() {
        RunState state = newRun(1);
        assertThat(violation(state, ROLL).code()).isEqualTo(Code.WRONG_PHASE);
        assertThat(violation(state, new Action.Freeze(ItemKind.HERO, 0)).code()).isEqualTo(Code.WRONG_PHASE);
        RunState over = runAtTurn(1, 1);
        over.setStatus(RunStatus.LOST);
        assertThat(violation(over, ROLL).code()).isEqualTo(Code.RUN_OVER);
        assertThat(violation(over, START).code()).isEqualTo(Code.RUN_OVER);
    }

    @Test
    void laterActionsAreNotThereYet() {
        RunState state = runAtTurn(1, 1);
        
        assertThatThrownBy(() -> resolver().apply(state, new Action.BuyRelic(0, 0))).isInstanceOf(UnsupportedOperationException.class);
        
        
        
        assertThatThrownBy(() -> resolver().apply(state, new Action.EndTurn())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theSameSeedFillsTheSameShelvesAndAnotherSeedDoesNot() {
        RunState a = runAtTurn(2024, 1);
        RunState b = runAtTurn(2024, 1);
        assertThat(a.tavern()).isEqualTo(b.tavern());
        resolver().apply(a, ROLL);
        resolver().apply(b, ROLL);
        assertThat(a.tavern()).isEqualTo(b.tavern());
        RunState c = runAtTurn(2025, 1);
        assertThat(c.tavern()).isNotEqualTo(a.tavern());
    }

    @Test
    void eventNumbersContinueAcrossActions() {
        RunState state = runAtTurn(1, 1);
        int after = state.lastEventSeq();
        TavernResult result = resolver().apply(state, ROLL);
        assertThat(result.events().events().getFirst().seq()).isEqualTo(after + 1);
        assertThat(state.lastEventSeq()).isEqualTo(result.events().lastSeq());
    }
}
