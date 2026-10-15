package com.forgottenheroes.engine.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.HeroDefinition;
import com.forgottenheroes.engine.content.RelicDefinition;
import com.forgottenheroes.engine.content.TokenDefinition;
import com.forgottenheroes.engine.event.EventJson;
import com.forgottenheroes.engine.testing.TestFixtures;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import tools.jackson.databind.JsonNode;

/**
 * Table-driven content QA: every file under {@code src/test/resources/scenarios} becomes one test,
 * and a meta-test insists that every hero, relic and token id owns at least one scenario (a file
 * named {@code <id>.json} or {@code <id>-<anything>.json}).
 */
class ScenarioTest {

    private static final Path FOLDER = Path.of("src/test/resources/scenarios");

    @TestFactory
    Stream<DynamicTest> everyScenarioFile() throws IOException {
        return files().stream().map(file -> DynamicTest.dynamicTest(file.getFileName().toString(), () -> ScenarioRunner.run(read(file))));
    }

    @Test
    void everyContentIdHasAScenario() throws IOException {
        Set<String> owners = files().stream()
            .map(file -> file.getFileName().toString().replace(".json", "").split("-")[0])
            .collect(Collectors.toSet());
        ContentIndex content = TestFixtures.content();
        assertThat(owners).containsAll(content.heroes().stream().map(HeroDefinition::id).toList());
        assertThat(owners).containsAll(content.relics().stream().map(RelicDefinition::id).toList());
        assertThat(owners).containsAll(content.tokens().stream().map(TokenDefinition::id).toList());
        assertThat(files().size()).isGreaterThanOrEqualTo(36 + 16 + 7);
    }

    private static List<Path> files() throws IOException {
        try (Stream<Path> stream = Files.list(FOLDER)) {
            return stream.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static JsonNode read(Path file) throws IOException {
        return EventJson.mapper().readTree(Files.readString(file, StandardCharsets.UTF_8));
    }
}
