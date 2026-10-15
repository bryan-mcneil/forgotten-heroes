package com.forgottenheroes.engine.content;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.forgottenheroes.engine.json.EngineJsonModule;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Reads {@code heroes.json}, {@code tokens.json} and {@code relics.json} from the classpath, checks
 * them against {@code content.schema.json} (a typo fails here, with the path to it, before any rule
 * runs), maps them to the DSL records and hands back a {@link ContentIndex}.
 */
public final class ContentLoader {

    /** The folder on the classpath holding the real game content. */
    public static final String DEFAULT_FOLDER = "content/";

    private static final String SCHEMA = "content/content.schema.json";
    private static final ObjectMapper MAPPER = JsonMapper.builder()
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .changeDefaultVisibility(visibility -> visibility.withIsGetterVisibility(JsonAutoDetect.Visibility.NONE))
        .addModule(new EngineJsonModule())
        .build();

    private ContentLoader() { }

    /** The real content: the three files under {@code content/}. */
    public static ContentIndex load() {
        return load(DEFAULT_FOLDER);
    }

    /** The three files under any classpath folder, so tests can load broken or tiny content sets. */
    public static ContentIndex load(String classpathFolder) {
        String folder = classpathFolder.endsWith("/") ? classpathFolder : classpathFolder + "/";
        return fromJson(read(folder + "heroes.json"), read(folder + "tokens.json"), read(folder + "relics.json"));
    }

    /** Builds the index from JSON text; every check (schema, ids, references) runs here. */
    public static ContentIndex fromJson(String heroesJson, String tokensJson, String relicsJson) {
        ObjectNode all = MAPPER.createObjectNode();
        all.set("heroes", parse(heroesJson, "heroes.json"));
        all.set("tokens", parse(tokensJson, "tokens.json"));
        all.set("relics", parse(relicsJson, "relics.json"));
        validateAgainstSchema(all);
        try {
            List<HeroDefinition> heroes = List.of(MAPPER.treeToValue(all.get("heroes"), HeroDefinition[].class));
            List<TokenDefinition> tokens = List.of(MAPPER.treeToValue(all.get("tokens"), TokenDefinition[].class));
            List<RelicDefinition> relics = List.of(MAPPER.treeToValue(all.get("relics"), RelicDefinition[].class));
            return new ContentIndex(heroes, tokens, relics);
        } catch (JacksonException e) {
            throw new ContentException("content does not map to the DSL records: " + e.getMessage(), e);
        }
    }

    /** The mapper the content files are read with; the backend reuses it to serve {@code GET /api/content}. */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    private static JsonNode parse(String json, String fileName) {
        try {
            return MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new ContentException(fileName + " is not valid JSON: " + e.getOriginalMessage(), e);
        }
    }

    private static void validateAgainstSchema(JsonNode all) {
        Schema schema;
        try (InputStream in = open(SCHEMA)) {
            schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(in);
        } catch (IOException e) {
            throw new ContentException("cannot read " + SCHEMA, e);
        }
        List<Error> errors = schema.validate(all);
        if (!errors.isEmpty()) {
            StringBuilder message = new StringBuilder("content fails the schema (" + errors.size() + " problem(s)):");
            for (Error error : errors) {
                message.append("\n  - ").append(error.getInstanceLocation()).append(": ").append(error.getMessage());
            }
            throw new ContentException(message.toString());
        }
    }

    private static String read(String resource) {
        try (InputStream in = open(resource)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ContentException("cannot read " + resource, e);
        }
    }

    private static InputStream open(String resource) {
        InputStream in = ContentLoader.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new ContentException("missing classpath resource " + resource);
        }
        return in;
    }
}
