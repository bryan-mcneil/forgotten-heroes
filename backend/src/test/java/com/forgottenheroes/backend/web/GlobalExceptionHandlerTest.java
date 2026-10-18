package com.forgottenheroes.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.forgottenheroes.backend.security.CurrentUser;
import com.forgottenheroes.backend.service.ActiveRunException;
import com.forgottenheroes.backend.service.NotFoundException;
import com.forgottenheroes.backend.service.RunService;
import com.forgottenheroes.backend.repo.VersionConflictException;
import com.forgottenheroes.engine.resolver.RuleViolation;
import com.forgottenheroes.engine.resolver.RuleViolation.Code;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.forgottenheroes.backend.security.SecurityConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Every error body is an RFC 9457 ProblemDetail with a {@code code}; this is the table in the handler's Javadoc. */
@WebMvcTest(RunController.class)
@Import(SecurityConfig.class)
@WithMockUser
class GlobalExceptionHandlerTest {

    private static final String BUY = "{\"type\":\"Buy\",\"slot\":0,\"bandIndex\":0,\"expectedVersion\":1}";

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
    void ruleViolationIs422WithTheEngineCode() {
        given(runs.apply(eq("dev"), eq("run-1"), any())).willThrow(new RuleViolation(Code.NOT_ENOUGH_GOLD, "You have 2 gold; a hero costs 3."));

        assertThat(post("/api/runs/run-1/actions", BUY))
            .hasStatus(422)
            .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .bodyJson()
            .isLenientlyEqualTo("""
                {"type":"https://heroes.bryanmcneil.pro/errors/rule-violation","title":"Not enough gold","status":422,
                 "code":"NOT_ENOUGH_GOLD","detail":"You have 2 gold; a hero costs 3.","instance":"/api/runs/run-1/actions"}""");
    }

    @Test
    void missingFieldIs400ValidationFailedNamingTheField() {
        assertThat(post("/api/runs/run-1/actions", "{\"type\":\"Buy\",\"bandIndex\":0,\"expectedVersion\":1}"))
            .hasStatus(400)
            .bodyJson()
            .isLenientlyEqualTo("""
                {"status":400,"code":"VALIDATION_FAILED","detail":"slot must not be null",
                 "errors":[{"field":"slot","message":"must not be null"}]}""");
    }

    @Test
    void negativeIndexIs400() {
        assertThat(post("/api/runs/run-1/actions", "{\"type\":\"Sell\",\"bandIndex\":-1,\"expectedVersion\":1}"))
            .hasStatus(400)
            .bodyJson()
            .extractingPath("$.detail").isEqualTo("bandIndex must be greater than or equal to 0");
    }

    @Test
    void unknownFieldIs400MalformedJson() {
        assertThat(post("/api/runs/run-1/actions", "{\"type\":\"Roll\",\"expectedVersion\":1,\"cheat\":true}"))
            .hasStatus(400)
            .bodyJson()
            .extractingPath("$.code").isEqualTo("MALFORMED_JSON");
    }

    @Test
    void unknownActionTypeIs400MalformedJson() {
        assertThat(post("/api/runs/run-1/actions", "{\"type\":\"Steal\",\"expectedVersion\":1}"))
            .hasStatus(400)
            .bodyJson()
            .extractingPath("$.code").isEqualTo("MALFORMED_JSON");
    }

    @Test
    void notFoundIs404() {
        given(runs.get("dev", "nope")).willThrow(new NotFoundException("Run", "nope"));

        assertThat(mvc.get().uri("/api/runs/nope"))
            .hasStatus(404)
            .bodyJson()
            .isLenientlyEqualTo("{\"status\":404,\"code\":\"NOT_FOUND\",\"detail\":\"Run nope was not found\"}");
    }

    @Test
    void staleVersionIs409WithTheCurrentVersion() {
        given(runs.apply(eq("dev"), eq("run-1"), any())).willThrow(new VersionConflictException("run-1", 1, 4));

        assertThat(post("/api/runs/run-1/actions", BUY))
            .hasStatus(409)
            .bodyJson()
            .isLenientlyEqualTo("{\"status\":409,\"code\":\"VERSION_CONFLICT\",\"currentVersion\":4}");
    }

    @Test
    void aSecondRunIs409WithTheRunToResume() {
        given(runs.start("dev")).willThrow(new ActiveRunException("run-1"));

        assertThat(mvc.post().uri("/api/runs"))
            .hasStatus(409)
            .bodyJson()
            .isLenientlyEqualTo("{\"status\":409,\"code\":\"RUN_ALREADY_ACTIVE\",\"activeRunId\":\"run-1\"}");
    }

    @Test
    void notImplementedIs501() {
        given(runs.start("dev")).willThrow(new UnsupportedOperationException("later"));

        assertThat(mvc.post().uri("/api/runs"))
            .hasStatus(501)
            .bodyJson()
            .extractingPath("$.code").isEqualTo("NOT_IMPLEMENTED");
    }

    @Test
    void springsOwnErrorsCarryACodeToo() {
        assertThat(mvc.get().uri("/api/runs/run-1/battles/four"))
            .hasStatus(400)
            .bodyJson()
            .extractingPath("$.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    void unknownRoutesAre404WithACode() {
        assertThat(mvc.get().uri("/api/nothing"))
            .hasStatus(404)
            .bodyJson()
            .isLenientlyEqualTo("{\"status\":404,\"code\":\"NOT_FOUND\",\"type\":\"https://heroes.bryanmcneil.pro/errors/not-found\"}");
    }

    @Test
    void unexpectedErrorsAre500WithoutAStackTrace() {
        given(runs.get("dev", "run-1")).willThrow(new IllegalStateException("secret internals"));

        MvcTestResult result = mvc.get().uri("/api/runs/run-1").exchange();

        assertThat(result).hasStatus(500).bodyJson().extractingPath("$.code").isEqualTo("INTERNAL_ERROR");
        assertThat(result).bodyText().doesNotContain("secret internals").doesNotContain("Exception");
    }

    private MvcTestResult post(String uri, String json) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }
}
