package com.forgottenheroes.engine.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.forgottenheroes.engine.event.GameEvent.PerkGained;
import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.Position;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.UnitId;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * The golden test: the committed file is the contract with the frontend. When an event changes on
 * purpose, run {@code ./mvnw -pl engine test -Dtest=EventJsonTest -Dgolden.update=true}, review the
 * diff of the file, and commit it with the change.
 */
class EventJsonTest {

    private static final Path GOLDEN = Path.of("src/test/resources/golden/events-sample.json");

    @Test
    void serializedSampleMatchesTheGoldenFile() throws IOException {
        String actual = EventJson.toPrettyJson(SampleEvents.all()) + "\n";
        if (Boolean.getBoolean("golden.update") || !Files.exists(GOLDEN)) {
            Files.createDirectories(GOLDEN.getParent());
            Files.writeString(GOLDEN, actual, StandardCharsets.UTF_8);
        }
        String expected = Files.readString(GOLDEN, StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void goldenFileReadsBackAsTheSameEvents() throws IOException {
        List<GameEvent> events = EventJson.listFromJson(Files.readString(GOLDEN, StandardCharsets.UTF_8));
        assertThat(events).isEqualTo(SampleEvents.all());
        assertThat(events).hasSize(37);
    }

    @Test
    void everyEventTypeIsInTheSampleAndRegisteredForJson() {
        Set<Class<?>> permitted = Set.of(GameEvent.class.getPermittedSubclasses());
        Set<Class<?>> registered = Arrays.stream(GameEvent.class.getAnnotation(JsonSubTypes.class).value())
            .map(JsonSubTypes.Type::value)
            .collect(Collectors.toSet());
        Set<Class<?>> sampled = SampleEvents.all().stream().map(GameEvent::getClass).collect(Collectors.toSet());
        assertThat(registered).isEqualTo(permitted);
        assertThat(sampled).isEqualTo(permitted);
        assertThat(permitted).hasSize(34);
    }

    @Test
    void typeIsTheSimpleNameAndSmallValuesAreText() {
        String json = EventJson.toJson(new PerkGained(7, new UnitId("u1"), "aoi", new Position(Side.AWAY, 2), Perk.WARD, null));
        assertThat(json).isEqualTo(
            "{\"type\":\"PerkGained\",\"seq\":7,\"unitId\":\"u1\",\"heroId\":\"aoi\",\"position\":\"away:2\",\"perk\":\"WARD\"}");
        assertThat(EventJson.fromJson(json))
            .isEqualTo(new PerkGained(7, new UnitId("u1"), "aoi", new Position(Side.AWAY, 2), Perk.WARD, null));
    }

    @Test
    void absentValuesAreLeftOutNotWrittenAsNull() {
        assertThat(EventJson.toJson(SampleEvents.all())).doesNotContain("null");
    }
}
