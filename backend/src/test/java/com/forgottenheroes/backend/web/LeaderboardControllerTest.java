package com.forgottenheroes.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.forgottenheroes.backend.security.SecurityConfig;
import com.forgottenheroes.backend.service.InvalidRequestException;
import com.forgottenheroes.backend.service.LeaderboardService;
import com.forgottenheroes.backend.web.dto.LeaderboardEntryDto;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** The leaderboard door: the season parameter in, ranked rows out, 400 for a season that is not a month. */
@WebMvcTest(LeaderboardController.class)
@Import(SecurityConfig.class)
@WithMockUser
class LeaderboardControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private LeaderboardService leaderboard;

    @Test
    void theTopOfASeasonAsJson() {
        given(leaderboard.top("2026-10")).willReturn(List.of(
            new LeaderboardEntryDto(1, "Cat", 10, 11, Instant.parse("2026-10-17T20:00:02Z")),
            new LeaderboardEntryDto(2, "Bob", 10, 14, Instant.parse("2026-10-17T20:00:01Z"))));

        assertThat(mvc.get().uri("/api/leaderboard").param("season", "2026-10"))
            .hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("[{\"rank\":1,\"displayName\":\"Cat\",\"crowns\":10,\"turns\":11,\"finishedAt\":\"2026-10-17T20:00:02Z\"},"
                + "{\"rank\":2,\"displayName\":\"Bob\",\"crowns\":10,\"turns\":14}]");
    }

    @Test
    void noSeasonMeansThisMonth() {
        given(leaderboard.top(null)).willReturn(List.of());

        assertThat(mvc.get().uri("/api/leaderboard")).hasStatusOk().bodyJson().isLenientlyEqualTo("[]");
        then(leaderboard).should().top(null);
    }

    @Test
    void aSeasonThatIsNotAMonthIs400() {
        given(leaderboard.top("october")).willThrow(new InvalidRequestException("INVALID_SEASON", "A season is a month written like 2026-10, not 'october'."));

        assertThat(mvc.get().uri("/api/leaderboard").param("season", "october"))
            .hasStatus(400)
            .bodyJson()
            .isLenientlyEqualTo("{\"status\":400,\"code\":\"INVALID_SEASON\",\"title\":\"Invalid season\"}");
    }
}
