package com.forgottenheroes.engine.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgottenheroes.engine.event.GameEvent.CrownsChanged;
import com.forgottenheroes.engine.event.GameEvent.GoldChanged;
import com.forgottenheroes.engine.event.GameEvent.GoldReason;
import com.forgottenheroes.engine.event.GameEvent.TurnEnded;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventLogTest {

    @Test
    void sequenceNumbersStartAtOneAndIncrease() {
        EventLog log = new EventLog();
        GoldChanged first = log.append(new GoldChanged(0, 0, 10, GoldReason.TURN_START));
        GoldChanged second = log.append(new GoldChanged(0, 10, 9, GoldReason.ROLL));
        assertThat(first.seq()).isEqualTo(1);
        assertThat(second.seq()).isEqualTo(2);
        assertThat(log.events()).containsExactly(first, second);
        assertThat(log.lastSeq()).isEqualTo(2);
        assertThat(log.size()).isEqualTo(2);
        assertThat(log.isEmpty()).isFalse();
    }

    @Test
    void aLogCanContinueWhereThePreviousOneEnded() {
        EventLog log = new EventLog(41);
        assertThat(log.append(new TurnEnded(0, 3)).seq()).isEqualTo(42);
        assertThatThrownBy(() -> new EventLog(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sinceReturnsOnlyLaterEvents() {
        EventLog log = new EventLog();
        log.append(new TurnEnded(0, 1));
        log.append(new TurnEnded(0, 2));
        log.append(new TurnEnded(0, 3));
        assertThat(log.since(1)).extracting(GameEvent::seq).containsExactly(2, 3);
        assertThat(log.since(3)).isEmpty();
    }

    @Test
    void appendAllRenumbersInOrder() {
        EventLog log = new EventLog(5);
        log.appendAll(List.of(new TurnEnded(99, 1), new CrownsChanged(98, 0, 1)));
        assertThat(log.events()).extracting(GameEvent::seq).containsExactly(6, 7);
        assertThat(log.of(CrownsChanged.class)).hasSize(1);
        assertThat(log.of(GoldChanged.class)).isEmpty();
    }

    @Test
    void storyIsOneLinePerEvent() {
        EventLog log = new EventLog();
        log.append(new TurnEnded(0, 1));
        log.append(new CrownsChanged(0, 0, 1));
        assertThat(log.story()).isEqualTo("TurnEnded[seq=1, turn=1]\nCrownsChanged[seq=2, before=0, after=1]\n");
        assertThat(log.stream().count()).isEqualTo(2);
    }
}
