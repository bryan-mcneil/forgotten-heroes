package com.forgottenheroes.backend.repo.dynamo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgottenheroes.backend.repo.BattleLog;
import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.content.ContentLoader;
import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.session.BandSnapshot;
import com.forgottenheroes.engine.session.GameSession;
import com.forgottenheroes.engine.session.TurnResult;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** A real battle's events, written as one folder and read back equal; and the size guard. */
@Testcontainers
class DynamoBattleLogRepositoryTest {

    private static final GameSession SESSION = new GameSession(ContentLoader.load());

    @Container
    static final GenericContainer<?> DYNAMO = DynamoLocal.container();

    static DynamoLocal dynamo;
    static DynamoBattleLogRepository repository;

    @BeforeAll
    static void createTableAndRepository() throws Exception {
        dynamo = DynamoLocal.connect(DYNAMO);
        repository = new DynamoBattleLogRepository(dynamo.enhanced(), dynamo.tableName());
    }

    private static TurnResult playOneTurn(String runId) {
        RunState state = SESSION.start(runId, 42L).state();
        SESSION.apply(state, new Action.Buy(0, 0));
        return SESSION.endTurn(state, BandSnapshot.EMPTY);
    }

    @Test
    void aBattleComesBackWithEveryEvent() {
        TurnResult turn = playOneTurn("run-9");
        BattleLog log = new BattleLog("run-9", 1, "Mirror", turn.ghost(), turn.events(), turn.outcome());
        Instant before = Instant.now();

        repository.save(log);

        assertThat(repository.find("run-9", 1)).contains(log);              // events, band and result all equal
        assertThat(repository.find("run-9", 2)).isEmpty();
        assertThat(repository.find("run-8", 1)).isEmpty();

        Map<String, AttributeValue> raw = dynamo.rawItem("RUN#run-9", "BATTLE#001");
        assertThat(raw.get("result").s()).isEqualTo(turn.outcome().name());
        assertThat(raw.get("events").s()).startsWith("[{\"type\":\"");
        assertThat(raw.get("events").s().getBytes(StandardCharsets.UTF_8).length)
            .as("a real battle stays far under the guard").isLessThan(DynamoBattleLogRepository.MAX_BYTES / 4);
        long expiresAt = Long.parseLong(raw.get("expiresAt").n());
        assertThat(expiresAt).isBetween(before.plus(Duration.ofDays(7)).getEpochSecond(),
            Instant.now().plus(Duration.ofDays(7)).getEpochSecond());
    }

    @Test
    void anOversizedLogIsRefusedBeforeItReachesTheLedger() {
        TurnResult turn = playOneTurn("run-7");
        List<GameEvent> tooMany = Collections.nCopies(20_000, turn.events().getFirst());   // ≈ 700 KB of JSON
        BattleLog log = new BattleLog("run-7", 1, "Mirror", turn.ghost(), tooMany, Outcome.DRAW);

        assertThatThrownBy(() -> repository.save(log))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("300 KB guard");
        assertThat(repository.find("run-7", 1)).isEmpty();
    }
}
