package com.forgottenheroes.backend.service;

import com.forgottenheroes.backend.web.dto.LeaderboardEntryDto;
import java.util.List;
import org.springframework.stereotype.Service;

/** Top 50 of a season by crowns, then fewest turns (architecture §4.2). Stub until Step 27. */
@Service
public class LeaderboardService {

    /** {@code season} is {@code YYYY-MM}; null means the current month. */
    public List<LeaderboardEntryDto> top(String season) {
        throw new UnsupportedOperationException("arrives in Step 27");
    }
}
