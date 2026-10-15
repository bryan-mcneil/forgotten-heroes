package com.forgottenheroes.engine.resolver;

import static com.forgottenheroes.engine.testing.TestFixtures.newRun;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.event.GameEvent.GoldChanged;
import com.forgottenheroes.engine.event.GameEvent.GoldReason;
import com.forgottenheroes.engine.event.GameEvent.RunEnded;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.RunStatus;
import com.forgottenheroes.engine.model.Rules;
import com.forgottenheroes.engine.mutator.GoldMutator;
import com.forgottenheroes.engine.mutator.RunMutator;
import org.junit.jupiter.api.Test;

class MutatorTest {

    @Test
    void goldGainsAreCappedAndEveryChangeIsAnEvent() {
        RunState state = newRun(1);
        EventLog log = new EventLog();
        GoldMutator.set(state, log, 48, GoldReason.TURN_START);
        GoldMutator.gain(state, log, 5, GoldReason.SELL);
        assertThat(state.gold()).isEqualTo(Rules.MAX_GOLD);
        GoldMutator.spend(state, log, 3, GoldReason.BUY_HERO);
        assertThat(state.gold()).isEqualTo(47);
        assertThat(log.of(GoldChanged.class)).extracting(GoldChanged::after).containsExactly(48, 50, 47);
        assertThatThrownBy(() -> GoldMutator.gain(state, log, -1, GoldReason.SELL)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GoldMutator.spend(state, log, -1, GoldReason.ROLL)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void runProgressMutatorsWriteTheirEvents() {
        RunState state = newRun(1);
        EventLog log = new EventLog();
        RunMutator.changeCrowns(state, log, 1);
        RunMutator.changeHearts(state, log, -1, com.forgottenheroes.engine.event.GameEvent.HeartsReason.BATTLE_LOST);
        RunMutator.endTurn(state, log);
        RunMutator.endRun(state, log, RunStatus.WON);
        assertThat(state.crowns()).isEqualTo(1);
        assertThat(state.hearts()).isEqualTo(4);
        assertThat(state.status()).isEqualTo(RunStatus.WON);
        assertThat(log.of(RunEnded.class).getFirst().hearts()).isEqualTo(4);
        assertThat(log.size()).isEqualTo(4);
        assertThatThrownBy(() -> RunMutator.endRun(state, log, RunStatus.IN_PROGRESS)).isInstanceOf(IllegalArgumentException.class);
    }
}
