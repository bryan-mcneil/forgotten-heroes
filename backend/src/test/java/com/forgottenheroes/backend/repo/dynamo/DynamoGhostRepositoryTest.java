package com.forgottenheroes.backend.repo.dynamo;

import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.backend.repo.Ghost;
import com.forgottenheroes.engine.session.BandSnapshot;
import com.forgottenheroes.engine.session.UnitSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** One drawer per turn; the sort key decides the order the drawer is read in. */
@Testcontainers
class DynamoGhostRepositoryTest {

    private static final Instant NOON = Instant.parse("2026-10-16T12:00:00Z");
    private static final BandSnapshot BAND = new BandSnapshot(List.of(
        new UnitSnapshot("aoi", 3, 4, 2, null), new UnitSnapshot("hotaru", 1, 6, 0, null)));

    @Container
    static final GenericContainer<?> DYNAMO = DynamoLocal.container();

    static DynamoLocal dynamo;
    static DynamoGhostRepository repository;

    @BeforeAll
    static void createTableAndRepository() throws Exception {
        dynamo = DynamoLocal.connect(DYNAMO);
        repository = new DynamoGhostRepository(dynamo.enhanced(), dynamo.tableName());
    }

    @Test
    void aDrawerIsReadHighestCrownsFirstThenNewest() {
        repository.save(new Ghost("ann", "r1", 3, 2, 5, BAND, false, NOON));
        repository.save(new Ghost("bob", "r2", 3, 7, 4, BAND, false, NOON.plusSeconds(60)));
        repository.save(new Ghost("cat", "r3", 3, 7, 3, BAND, true, NOON.plusSeconds(120)));
        repository.save(new Ghost("dan", "r4", 4, 9, 5, BAND, false, NOON));          // turn 4: another drawer

        List<Ghost> turn3 = repository.findByTurn(3);

        assertThat(turn3).extracting(Ghost::runId).containsExactly("r3", "r2", "r1");
        assertThat(turn3.getFirst()).isEqualTo(new Ghost("cat", "r3", 3, 7, 3, BAND, true, NOON.plusSeconds(120)));
        assertThat(repository.findByTurn(4)).extracting(Ghost::ownerUserId).containsExactly("dan");
        assertThat(repository.findByTurn(5)).isEmpty();
    }

    @Test
    void theSortKeyPutsCrownsBeforeTimeWithZeroPadding() {
        assertThat(Keys.ghostSort(7, NOON, "r1")).isEqualTo("07#2026-10-16T12:00:00.000Z#r1");
        assertThat(Keys.ghostSort(10, NOON, "r1")).as("as text, 10 must sort after 9").isGreaterThan(Keys.ghostSort(9, NOON, "r1"));
        assertThat(Keys.ghostSort(7, NOON.plusMillis(1), "r1")).isGreaterThan(Keys.ghostSort(7, NOON, "r9"));
    }

    @Test
    void theWindowReadsOnlyTheSliceAndStopsAtTheLimit() {
        for (int crowns = 0; crowns <= 10; crowns++) {
            repository.save(new Ghost("p" + crowns, "w" + crowns, 8, crowns, 5, BAND, false, NOON.plusSeconds(crowns)));
        }
        repository.save(new Ghost("late", "w99", 8, 7, 5, BAND, false, NOON.plusSeconds(600)));   // a newer 7

        assertThat(repository.findCandidates(8, 6, 8, 25)).extracting(Ghost::runId)
            .as("between \"06#\" and \"09#\", highest first, newest first within 7")
            .containsExactly("w8", "w99", "w7", "w6");
        assertThat(repository.findCandidates(8, 6, 8, 2)).extracting(Ghost::runId)
            .as("the limit caps the read").containsExactly("w8", "w99");
        assertThat(repository.findCandidates(8, 10, 10, 25)).extracting(Ghost::crowns)
            .as("\"10#\" sorts after \"09#\", not before \"2#\"").containsExactly(10);
        assertThat(repository.findCandidates(9, 0, 10, 25)).as("another drawer").isEmpty();
    }

    @Test
    void aGhostCarriesItsExpiryThirtyDaysAfterItWasSaved() {
        repository.save(new Ghost("eve", "r5", 6, 4, 2, BAND, false, NOON));

        Map<String, AttributeValue> raw = dynamo.rawItem("GHOST#T6", "04#2026-10-16T12:00:00.000Z#r5");
        assertThat(raw.get("expiresAt").n()).isEqualTo(String.valueOf(NOON.plus(Duration.ofDays(30)).getEpochSecond()));
        assertThat(raw.get("bot").bool()).isFalse();
        assertThat(raw.get("band").s()).contains("\"heroId\":\"aoi\"");
    }
}
