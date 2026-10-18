package com.forgottenheroes.backend.repo.dynamo;

import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.backend.repo.LeaderboardEntry;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** One drawer per season; the sort key is the ranking, so the Query result needs no sorting. */
@Testcontainers
class DynamoLeaderboardRepositoryTest {

    private static final Instant OCTOBER = Instant.parse("2026-10-17T20:00:00Z");

    @Container
    static final GenericContainer<?> DYNAMO = DynamoLocal.container();

    static DynamoLocal dynamo;
    static DynamoLeaderboardRepository repository;

    @BeforeAll
    static void createTableAndRepository() throws Exception {
        dynamo = DynamoLocal.connect(DYNAMO);
        repository = new DynamoLeaderboardRepository(dynamo.enhanced(), dynamo.tableName());
    }

    @Test
    void theDrawerIsReadBestFirstAndTheLimitCapsIt() {
        repository.save(new LeaderboardEntry("2026-10", "ann", "Ann", 7, 12, OCTOBER));
        repository.save(new LeaderboardEntry("2026-10", "bob", "Bob", 10, 14, OCTOBER.plusSeconds(1)));
        repository.save(new LeaderboardEntry("2026-10", "cat", "Cat", 10, 11, OCTOBER.plusSeconds(2)));
        repository.save(new LeaderboardEntry("2026-09", "dan", "Dan", 10, 5, OCTOBER.minusSeconds(1_000_000)));   // another drawer

        assertThat(repository.top("2026-10", 50)).extracting(LeaderboardEntry::displayName).containsExactly("Cat", "Bob", "Ann");
        assertThat(repository.top("2026-10", 2)).extracting(LeaderboardEntry::displayName).containsExactly("Cat", "Bob");
        assertThat(repository.top("2026-10", 50).getFirst()).isEqualTo(new LeaderboardEntry("2026-10", "cat", "Cat", 10, 11, OCTOBER.plusSeconds(2)));
        assertThat(repository.top("2026-09", 50)).extracting(LeaderboardEntry::userId).containsExactly("dan");
        assertThat(repository.top("2026-08", 50)).isEmpty();
    }

    @Test
    void theSortKeyRanksCrownsThenTurnsWithZeroPadding() {
        assertThat(Keys.leaderboardSort(10, 11, "cat")).isEqualTo("89#011#cat");
        assertThat(Keys.leaderboardSort(10, 11, "cat")).as("same crowns: fewer turns first").isLessThan(Keys.leaderboardSort(10, 14, "bob"));
        assertThat(Keys.leaderboardSort(10, 100, "x")).as("more crowns win even with many more turns").isLessThan(Keys.leaderboardSort(9, 5, "y"));
        assertThat(Keys.leaderboardSort(0, 1, "z")).isEqualTo("99#001#z");
    }

    @Test
    void aRowExpiresAtTheEndOfTheFollowingSeason() {
        repository.save(new LeaderboardEntry("2026-10", "eve", "Eve", 4, 8, OCTOBER));

        Map<String, AttributeValue> raw = dynamo.rawItem(Keys.leaderboard("2026-10"), Keys.leaderboardSort(4, 8, "eve"));

        assertThat(raw.get(Ttl.ATTRIBUTE).n()).isEqualTo(String.valueOf(Instant.parse("2026-12-01T00:00:00Z").getEpochSecond()));
        assertThat(Ttl.endOfNextSeason(YearMonth.of(2026, 12))).isEqualTo(Instant.parse("2027-02-01T00:00:00Z").getEpochSecond());
    }

    @Test
    void theSamePlayerWithTheSameScoreIsOneRow() {
        repository.save(new LeaderboardEntry("2026-11", "fay", "Fay", 6, 9, OCTOBER));
        repository.save(new LeaderboardEntry("2026-11", "fay", "Fay the Second", 6, 9, OCTOBER.plusSeconds(60)));

        assertThat(repository.top("2026-11", 50)).singleElement()
            .extracting(LeaderboardEntry::displayName).isEqualTo("Fay the Second");
    }
}
