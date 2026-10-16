package com.forgottenheroes.backend.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The API contract is a file, and changing it is a decision. This test asks the running app for its
 * OpenAPI description, writes it to {@code backend/target/openapi.json} (what the frontend generates
 * its types from, Step 42) and compares it with the committed snapshot. A difference fails the build
 * until the snapshot is refreshed on purpose:
 * <pre>
 *   ./mvnw -pl backend -am -Dtest=OpenApiContractTest -Dopenapi.snapshot.update=true test
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {

    /** Surefire runs with the module folder as the working directory. */
    static final Path EXPORT = Path.of("target", "openapi.json");
    static final Path SNAPSHOT = Path.of("src", "test", "resources", "openapi.snapshot.json");

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    /** Two-space indents and LF line ends on every platform, so Windows and Linux write the same bytes. */
    private static final DefaultPrettyPrinter PRETTY = new DefaultPrettyPrinter()
        .withObjectIndenter(new DefaultIndenter("  ", "\n"))
        .withArrayIndenter(new DefaultIndenter("  ", "\n"));

    @Autowired
    private MockMvcTester mvc;

    @Test
    void theExportedContractMatchesTheSnapshot() throws IOException {
        MvcTestResult result = mvc.get().uri("/v3/api-docs").exchange();
        assertThat(result).hasStatusOk();
        JsonNode actual = MAPPER.readTree(result.getResponse().getContentAsByteArray());
        String pretty = MAPPER.writer().with(PRETTY).writeValueAsString(actual) + "\n";

        Files.createDirectories(EXPORT.getParent());
        Files.writeString(EXPORT, pretty);
        if (Boolean.getBoolean("openapi.snapshot.update")) {
            Files.createDirectories(SNAPSHOT.getParent());
            Files.writeString(SNAPSHOT, pretty);
            return;
        }

        assertThat(SNAPSHOT).as("the snapshot is missing; create it with -Dopenapi.snapshot.update=true").exists();
        JsonNode expected = MAPPER.readTree(Files.readString(SNAPSHOT));
        assertThat(actual.equals(expected))
            .as("""
                The API contract changed (compare backend/target/openapi.json with %s).
                If that was intended, refresh the snapshot and commit the diff:
                  ./mvnw -pl backend -am -Dtest=OpenApiContractTest -Dopenapi.snapshot.update=true test""", SNAPSHOT)
            .isTrue();
    }

    @Test
    void everyRouteOfTheContractIsDescribed() throws IOException {
        MvcTestResult result = mvc.get().uri("/v3/api-docs").exchange();
        JsonNode paths = MAPPER.readTree(result.getResponse().getContentAsByteArray()).get("paths");

        assertThat(paths.propertyNames()).containsExactlyInAnyOrder(
            "/api/health", "/api/content", "/api/me", "/api/leaderboard",
            "/api/runs", "/api/runs/{runId}", "/api/runs/{runId}/actions", "/api/runs/{runId}/end-turn",
            "/api/runs/{runId}/battles/{turn}", "/api/runs/{runId}/abandon");
    }
}
