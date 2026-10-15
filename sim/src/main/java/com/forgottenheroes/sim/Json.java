package com.forgottenheroes.sim;

import com.forgottenheroes.engine.json.EngineJsonModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/** The simulator's file formats: reports as JSON, thresholds as YAML, one mapper each. */
final class Json {

    static final ObjectMapper JSON = JsonMapper.builder().addModule(new EngineJsonModule()).build();
    static final ObjectMapper YAML = YAMLMapper.builder().build();

    private Json() { }

    static void write(Path file, Object value) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.writeString(file, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(value) + "\n", StandardCharsets.UTF_8);
    }

    static <T> T read(Path file, Class<T> type) throws IOException {
        return JSON.readValue(Files.readString(file, StandardCharsets.UTF_8), type);
    }

    static <T> T readYaml(Path file, Class<T> type) throws IOException {
        return YAML.readValue(Files.readString(file, StandardCharsets.UTF_8), type);
    }
}
