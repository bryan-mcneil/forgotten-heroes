package com.forgottenheroes.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.GitProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private GitProperties git;

    @Test
    void healthReportsUpAndTheRunningCommit() {
        given(git.getShortCommitId()).willReturn("abc1234");

        assertThat(mvc.get().uri("/api/health"))
            .hasStatusOk()
            .hasContentTypeCompatibleWith("application/json")
            .bodyJson()
            .isLenientlyEqualTo("{\"status\":\"UP\",\"version\":\"abc1234\"}");
    }
}
