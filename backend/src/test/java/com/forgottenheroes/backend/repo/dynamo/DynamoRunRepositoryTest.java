package com.forgottenheroes.backend.repo.dynamo;

import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.content.ContentLoader;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.session.BandSnapshot;
import com.forgottenheroes.engine.session.GameSession;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** A real run, played by the real engine, written as one JSON folder and read back equal. */
@Testcontainers
class DynamoRunRepositoryTest {

    private static final GameSession SESSION = new GameSession(ContentLoader.load());

    @Container
    static final GenericContainer<?> DYNAMO = DynamoLocal.container();

    static DynamoLocal dynamo;
    static DynamoRunRepository repository;

    @BeforeAll
    static void createTableAndRepository() throws Exception {
        dynamo = DynamoLocal.connect(DYNAMO);
        repository = new DynamoRunRepository(dynamo.enhanced(), dynamo.tableName());
    }

    @Test
    void aRunInProgressComesBackEqualAndIsKeptForEver() {
        RunState state = SESSION.start("run-1", 42L).state();
        SESSION.apply(state, new Action.Buy(0, 0));
        SESSION.endTurn(state, BandSnapshot.EMPTY);                       // turn 2, one crown, one hero

        repository.save("bryan", state);

        assertThat(repository.find("bryan", "run-1")).contains(state);   // every field, band and tavern included
        assertThat(repository.find("someone-else", "run-1")).as("another drawer").isEmpty();
        assertThat(repository.find("bryan", "no-such-run")).isEmpty();

        Map<String, AttributeValue> raw = dynamo.rawItem("USER#bryan", "RUN#run-1");
        assertThat(raw.get("turn").n()).isEqualTo("2");
        assertThat(raw.get("crowns").n()).isEqualTo("1");
        assertThat(raw.get("status").s()).isEqualTo("IN_PROGRESS");
        assertThat(raw.get("version").n()).isEqualTo(String.valueOf(state.version()));
        assertThat(raw.get("state").s()).startsWith("{\"runId\":\"run-1\"");
        assertThat(raw).as("no TTL while the run is in progress").doesNotContainKey("expiresAt");
    }

    @Test
    void aFinishedRunExpiresNinetyDaysLater() {
        RunState state = SESSION.start("run-2", 7L).state();
        SESSION.abandon(state);
        Instant before = Instant.now();

        repository.save("bryan", state);

        Map<String, AttributeValue> raw = dynamo.rawItem("USER#bryan", "RUN#run-2");
        long expiresAt = Long.parseLong(raw.get("expiresAt").n());
        assertThat(expiresAt).isBetween(before.plus(Duration.ofDays(90)).getEpochSecond(),
            Instant.now().plus(Duration.ofDays(90)).getEpochSecond());
        assertThat(raw.get("status").s()).isEqualTo("ABANDONED");
        assertThat(repository.find("bryan", "run-2")).contains(state);
    }

    @Test
    void savingAgainReplacesTheWholeFolder() {
        RunState state = SESSION.start("run-3", 42L).state();
        repository.save("bryan", state);
        long firstVersion = state.version();

        SESSION.apply(state, new Action.Buy(0, 0));
        repository.save("bryan", state);

        RunState found = repository.find("bryan", "run-3").orElseThrow();
        assertThat(found.version()).isEqualTo(firstVersion + 1);
        assertThat(found.gold()).isEqualTo(7);
        assertThat(found.band().size()).isEqualTo(1);
        assertThat(found).isEqualTo(state);
    }
}
