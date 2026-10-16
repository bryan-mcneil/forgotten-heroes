package com.forgottenheroes.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.forgottenheroes.backend.security.CurrentUser;
import com.forgottenheroes.backend.service.RunService;
import com.forgottenheroes.backend.web.dto.ActionRequest;
import com.forgottenheroes.backend.web.dto.ActionResponse;
import com.forgottenheroes.backend.web.dto.BandDto;
import com.forgottenheroes.backend.web.dto.RunStateDto;
import com.forgottenheroes.backend.web.dto.TavernDto;
import com.forgottenheroes.backend.web.dto.UnitDto;
import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.RunStatus;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.UnitId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** The web layer alone: routes, JSON in and out, the caller's id passed down. The service is a mock, so no rule runs here. */
@WebMvcTest(RunController.class)
class RunControllerTest {

    private static final RunStateDto STATE = new RunStateDto("run-1", 1, 10, 5, 0, RunStatus.IN_PROGRESS, true,
        new BandDto(List.of(new UnitDto("u1", "ash_cat", 2, 3, 1, 0, null))),
        new TavernDto(List.of(), List.of(), null), null, 3L, 7);

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RunService runs;

    @MockitoBean
    private CurrentUser user;

    @BeforeEach
    void theCallerIsDev() {
        given(user.id()).willReturn("dev");
    }

    @Test
    void startAnswers201WithTheState() {
        given(runs.start("dev")).willReturn(STATE);

        assertThat(mvc.post().uri("/api/runs"))
            .hasStatus(201)
            .bodyJson()
            .isLenientlyEqualTo("{\"runId\":\"run-1\",\"gold\":10,\"hearts\":5,\"status\":\"IN_PROGRESS\",\"version\":3}");
    }

    @Test
    void nullFieldsAreLeftOutOfTheJson() {
        given(runs.get("dev", "run-1")).willReturn(STATE);

        assertThat(mvc.get().uri("/api/runs/run-1"))
            .hasStatusOk()
            .bodyJson()
            .extractingPath("$.band.units[0]")
            .asMap()
            .doesNotContainKey("perk")
            .containsEntry("heroId", "ash_cat");
    }

    @Test
    void actionBodyIsReadByItsTypeFieldAndEventsAreWrittenWithTheirs() {
        GameEvent bought = new GameEvent.HeroBought(8, new UnitId("u2"), "ash_cat", 0, 1, new Stats(2, 3), 3);
        given(runs.apply("dev", "run-1", new ActionRequest.Buy(0, 1, 3L))).willReturn(new ActionResponse(STATE, List.of(bought)));

        assertThat(mvc.post().uri("/api/runs/run-1/actions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"Buy\",\"slot\":0,\"bandIndex\":1,\"expectedVersion\":3}"))
            .hasStatusOk()
            .bodyJson()
            .extractingPath("$.events[0]")
            .asMap()
            .containsEntry("type", "HeroBought")
            .containsEntry("unitId", "u2")
            .containsEntry("seq", 8);
    }

    @Test
    void endTurnPassesTheCallerAndTheExpectedVersionOn() {
        assertThat(mvc.post().uri("/api/runs/run-1/end-turn")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":3}"))
            .hasStatusOk();

        then(runs).should().endTurn("dev", "run-1", 3L);
    }

    @Test
    void battleTurnIsReadFromThePath() {
        assertThat(mvc.get().uri("/api/runs/run-1/battles/4")).hasStatusOk();

        then(runs).should().battle("dev", "run-1", 4);
    }

    @Test
    void perkIsWrittenWhenPresent() {
        RunStateDto withPerk = new RunStateDto("run-1", 1, 10, 5, 0, RunStatus.IN_PROGRESS, true,
            new BandDto(List.of(new UnitDto("u1", "ash_cat", 2, 3, 1, 0, Perk.values()[0]))),
            new TavernDto(List.of(), List.of(), null), null, 3L, 7);
        given(runs.get("dev", "run-1")).willReturn(withPerk);

        assertThat(mvc.get().uri("/api/runs/run-1"))
            .hasStatusOk()
            .bodyJson()
            .extractingPath("$.band.units[0].perk")
            .isEqualTo(Perk.values()[0].name());
    }
}
