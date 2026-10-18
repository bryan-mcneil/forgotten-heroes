package com.forgottenheroes.backend.service;

import com.forgottenheroes.backend.repo.LeaderboardEntry;
import com.forgottenheroes.backend.repo.LeaderboardRepository;
import com.forgottenheroes.backend.web.dto.LeaderboardEntryDto;
import com.forgottenheroes.engine.model.RunStatus;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

/**
 * Top 50 of a season by crowns, then fewest turns (architecture §4.2). A season is a calendar month in
 * UTC, named {@code yyyy-MM}. One row per finished run: a player who finishes three runs in a month can
 * hold three places; a run played to its end counts, an abandoned one does not.
 */
@Service
public class LeaderboardService {

    static final int TOP = 50;

    private final LeaderboardRepository rows;

    public LeaderboardService(LeaderboardRepository rows) {
        this.rows = rows;
    }

    /** {@code season} is {@code yyyy-MM}; null or blank means the current month. Anything else is 400 {@code INVALID_SEASON}. */
    public List<LeaderboardEntryDto> top(String season) {
        String month = season == null || season.isBlank() ? currentSeason() : parse(season);
        List<LeaderboardEntry> best = rows.top(month, TOP);
        return IntStream.range(0, best.size())
            .mapToObj(i -> toDto(i + 1, best.get(i)))                 // the rank is the row's place in the Query result
            .toList();
    }

    /**
     * Called when a run ends. Only WON and LOST count: an abandoned run is not a result. The display
     * name travels with the row (no joins in DynamoDB), so a later rename shows on later rows only.
     */
    public void record(String userId, String displayName, RunStatus status, int crowns, int turns, Instant finishedAt) {
        if (status != RunStatus.WON && status != RunStatus.LOST) {
            return;
        }
        rows.save(new LeaderboardEntry(seasonOf(finishedAt), userId, displayName, crowns, turns, finishedAt));
    }

    static String seasonOf(Instant at) {
        return YearMonth.from(at.atOffset(ZoneOffset.UTC)).toString();
    }

    static String currentSeason() {
        return YearMonth.now(ZoneOffset.UTC).toString();
    }

    private static String parse(String season) {
        try {
            return YearMonth.parse(season).toString();
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("INVALID_SEASON", "A season is a month written like 2026-10, not '" + season + "'.");
        }
    }

    private static LeaderboardEntryDto toDto(int rank, LeaderboardEntry entry) {
        return new LeaderboardEntryDto(rank, entry.displayName(), entry.crowns(), entry.turns(), entry.finishedAt());
    }
}
