package com.forgottenheroes.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.forgottenheroes.backend.repo.InMemoryLeaderboardRepository;
import com.forgottenheroes.backend.web.dto.LeaderboardEntryDto;
import com.forgottenheroes.engine.model.RunStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Ranking, seasons and what counts as a result, against the in-memory board: no Spring, no Docker. */
class LeaderboardServiceTest {

    private static final Instant OCTOBER = Instant.parse("2026-10-17T20:00:00Z");
    private static final Instant SEPTEMBER = Instant.parse("2026-09-30T23:59:59Z");

    private final InMemoryLeaderboardRepository rows = new InMemoryLeaderboardRepository();
    private final LeaderboardService service = new LeaderboardService(rows);

    @Test
    void threeFinishedRunsComeBackBestFirstWithRanks() {
        service.record("ann", "Ann", RunStatus.LOST, 7, 12, OCTOBER);
        service.record("bob", "Bob", RunStatus.WON, 10, 14, OCTOBER.plusSeconds(1));
        service.record("cat", "Cat", RunStatus.WON, 10, 11, OCTOBER.plusSeconds(2));

        List<LeaderboardEntryDto> top = service.top("2026-10");

        assertThat(top)
            .extracting(LeaderboardEntryDto::rank, LeaderboardEntryDto::displayName, LeaderboardEntryDto::crowns, LeaderboardEntryDto::turns)
            .containsExactly(tuple(1, "Cat", 10, 11), tuple(2, "Bob", 10, 14), tuple(3, "Ann", 7, 12));
    }

    @Test
    void onlyARunPlayedToItsEndIsAResult() {
        service.record("ann", "Ann", RunStatus.ABANDONED, 9, 4, OCTOBER);
        service.record("ann", "Ann", RunStatus.IN_PROGRESS, 9, 4, OCTOBER);

        assertThat(service.top("2026-10")).isEmpty();
    }

    @Test
    void theSameResultTwiceIsOneRow() {
        service.record("ann", "Ann", RunStatus.LOST, 7, 12, OCTOBER);
        service.record("ann", "Ann the Renamed", RunStatus.LOST, 7, 12, OCTOBER.plusSeconds(3600));

        assertThat(service.top("2026-10")).singleElement()
            .extracting(LeaderboardEntryDto::displayName, LeaderboardEntryDto::finishedAt)
            .containsExactly("Ann the Renamed", OCTOBER.plusSeconds(3600));
    }

    @Test
    void seasonsAreSeparateBoardsAndTheDefaultIsThisMonth() {
        service.record("ann", "Ann", RunStatus.WON, 10, 9, SEPTEMBER);
        service.record("bob", "Bob", RunStatus.LOST, 3, 6, OCTOBER);

        assertThat(service.top("2026-09")).extracting(LeaderboardEntryDto::displayName).containsExactly("Ann");
        assertThat(service.top("2026-10")).extracting(LeaderboardEntryDto::displayName).containsExactly("Bob");
        assertThat(service.top(null)).isEqualTo(service.top(LeaderboardService.currentSeason()));
        assertThat(service.top("  ")).isEqualTo(service.top(null));
    }

    @Test
    void aSeasonMustBeAMonth() {
        for (String bad : List.of("october", "2026-13", "2026", "2026-10-17")) {
            assertThatThrownBy(() -> service.top(bad)).as(bad)
                .isInstanceOf(InvalidRequestException.class)
                .extracting("code").isEqualTo("INVALID_SEASON");
        }
    }

    @Test
    void theSeasonIsTheUtcMonthTheRunFinishedIn() {
        assertThat(LeaderboardService.seasonOf(Instant.parse("2026-10-31T23:30:00Z"))).isEqualTo("2026-10");
        assertThat(LeaderboardService.seasonOf(Instant.parse("2026-11-01T00:00:00Z"))).isEqualTo("2026-11");
    }
}
