package com.forgottenheroes.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;

/**
 * The whole stack over real HTTP: Tomcat on a random port, the real services, the in-memory
 * repositories and the engine. One run, one purchase, one battle, as the browser will do it in Step 44.
 * Runs with the local doorman (profile {@code local}, which reads {@code X-Dev-User} instead of a Cognito
 * token) but keeps the in-memory storage, so no Docker and no AWS are needed.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "app.storage=memory")
@AutoConfigureTestRestTemplate
@ActiveProfiles("local")
class ApiHappyPathTest {

    @Autowired
    private TestRestTemplate rest;

    /** Every call is made as the same player; without the header the local doorman would say {@code dev}, which also works. */
    @BeforeEach
    void callAsBryan() {
        ClientHttpRequestInterceptor asBryan = (request, body, execution) -> {
            request.getHeaders().set("X-Dev-User", "bryan");
            return execution.execute(request, body);
        };
        rest.getRestTemplate().setInterceptors(List.of(asBryan));
    }

    @Test
    void startBuyAndEndATurn() {
        ResponseEntity<JsonNode> started = rest.postForEntity("/api/runs", null, JsonNode.class);
        assertThat(started.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode run = started.getBody();
        String runId = run.get("runId").asString();
        long version = run.get("version").asLong();
        assertThat(run.get("turn").asInt()).isEqualTo(1);
        assertThat(run.has("runSeed")).as("the seed never leaves the server").isFalse();

        ResponseEntity<JsonNode> me = rest.getForEntity("/api/me", JsonNode.class);
        assertThat(me.getBody().get("activeRunId").asString()).isEqualTo(runId);

        ResponseEntity<JsonNode> bought = rest.postForEntity("/api/runs/{id}/actions",
            Map.of("type", "Buy", "slot", 0, "bandIndex", 0, "expectedVersion", version), JsonNode.class, runId);
        assertThat(bought.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bought.getBody().get("state").get("band").get("units")).hasSize(1);
        assertThat(bought.getBody().get("events").get(0).get("type").asString()).isNotEmpty();
        version = bought.getBody().get("state").get("version").asLong();

        ResponseEntity<JsonNode> stale = rest.postForEntity("/api/runs/{id}/actions",
            Map.of("type", "Roll", "expectedVersion", version - 1), JsonNode.class, runId);
        assertThat(stale.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(stale.getBody().get("code").asString()).isEqualTo("VERSION_CONFLICT");

        ResponseEntity<JsonNode> ended = rest.postForEntity("/api/runs/{id}/end-turn",
            Map.of("expectedVersion", version), JsonNode.class, runId);
        assertThat(ended.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode battle = ended.getBody().get("battle");
        assertThat(battle.get("opponentName").asString()).isEqualTo("Mirror");
        assertThat(battle.get("result").asString()).isIn("WIN", "LOSS", "DRAW");
        assertThat(battle.get("events").get(0).get("type").asString()).isEqualTo("BattleStarted");
        assertThat(ended.getBody().get("state").get("turn").asInt()).isEqualTo(2);

        ResponseEntity<JsonNode> replay = rest.getForEntity("/api/runs/{id}/battles/1", JsonNode.class, runId);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replay.getBody().get("events")).isEqualTo(battle.get("events"));

        ResponseEntity<JsonNode> renamed = rest.exchange("/api/me", HttpMethod.PUT,
            new HttpEntity<>(Map.of("displayName", "Bryan  the Bold")), JsonNode.class);   // the double space is collapsed
        assertThat(renamed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(renamed.getBody().get("displayName").asString()).isEqualTo("Bryan the Bold");
        assertThat(rest.getForEntity("/api/me", JsonNode.class).getBody().get("displayName").asString()).isEqualTo("Bryan the Bold");

        ResponseEntity<JsonNode> board = rest.getForEntity("/api/leaderboard", JsonNode.class);
        assertThat(board.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(board.getBody().isArray()).as("no run has finished yet, so this month's board is empty").isTrue();
        assertThat(board.getBody()).isEmpty();
    }
}
