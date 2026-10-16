package com.forgottenheroes.backend.web;

import com.forgottenheroes.engine.json.EngineJsonModule;
import org.springframework.stereotype.Component;
import tools.jackson.databind.module.SimpleModule;

/**
 * The engine's Jackson module as a Spring bean, so the API writes unit ids and positions as text and
 * ranks as numbers, exactly like {@code EventJson}. Spring Boot registers every {@code JacksonModule}
 * bean with its auto-configured mapper, and {@code @WebMvcTest} slices include them too. Absent values
 * are left out by {@code spring.jackson.default-property-inclusion} in {@code application.yaml}.
 */
@Component
public class ApiJsonModule extends SimpleModule {

    private final EngineJsonModule engine = new EngineJsonModule();

    public ApiJsonModule() {
        super("forgotten-heroes-api");
    }

    @Override
    public void setupModule(SetupContext context) {
        engine.setupModule(context);
    }
}
