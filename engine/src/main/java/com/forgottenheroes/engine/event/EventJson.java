package com.forgottenheroes.engine.event;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.forgottenheroes.engine.json.EngineJsonModule;
import java.util.List;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * The one mapper for events, configured once: the {@code type} field names the record, absent
 * values are left out instead of written as {@code null}, and fields keep their declaration order,
 * so the same log always produces the same bytes. The golden file in the tests is this contract.
 */
public final class EventJson {

    private static final TypeReference<List<GameEvent>> EVENT_LIST = new TypeReference<>() { };

    /** Two-space indents and LF line ends on every platform, so the golden file is the same on Windows and Linux. */
    private static final PrettyPrinter PRETTY = new DefaultPrettyPrinter()
        .withObjectIndenter(new DefaultIndenter("  ", "\n"))
        .withArrayIndenter(new DefaultIndenter("  ", "\n"));

    private static final ObjectMapper MAPPER = JsonMapper.builder()
        .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .changeDefaultVisibility(visibility -> visibility.withIsGetterVisibility(JsonAutoDetect.Visibility.NONE))
        .addModule(new EngineJsonModule())
        .build();

    private EventJson() { }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String toJson(GameEvent event) {
        return MAPPER.writeValueAsString(event);
    }

    /** A list must be written through the typed writer, or Jackson forgets the {@code type} field of each element. */
    public static String toJson(List<? extends GameEvent> events) {
        return MAPPER.writerFor(EVENT_LIST).writeValueAsString(events);
    }

    /** Indented, one field per line: what the golden file and bug reports use. */
    public static String toPrettyJson(List<? extends GameEvent> events) {
        return MAPPER.writerFor(EVENT_LIST).with(PRETTY).writeValueAsString(events);
    }

    public static GameEvent fromJson(String json) {
        return MAPPER.readValue(json, GameEvent.class);
    }

    public static List<GameEvent> listFromJson(String json) {
        return MAPPER.readValue(json, EVENT_LIST);
    }
}
