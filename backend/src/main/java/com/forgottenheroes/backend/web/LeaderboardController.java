package com.forgottenheroes.backend.web;

import com.forgottenheroes.backend.service.LeaderboardService;
import com.forgottenheroes.backend.web.dto.LeaderboardEntryDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/leaderboard?season=2026-10}: top 50 of a season; no season means this month. */
@RestController
public class LeaderboardController {

    private final LeaderboardService leaderboard;

    public LeaderboardController(LeaderboardService leaderboard) {
        this.leaderboard = leaderboard;
    }

    @GetMapping("/api/leaderboard")
    public List<LeaderboardEntryDto> top(@RequestParam(required = false) String season) {
        return leaderboard.top(season);
    }
}
