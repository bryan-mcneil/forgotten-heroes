package com.forgottenheroes.backend.web;

import com.forgottenheroes.engine.model.Position;
import com.forgottenheroes.engine.model.Rank;
import com.forgottenheroes.engine.model.UnitId;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * What springdoc writes at the top of {@code /v3/api-docs}, plus three corrections: the engine's
 * {@code EngineJsonModule} writes a unit id and a position as text and a rank as a number, but
 * springdoc describes classes, not JSON, so it would document them as objects. The title and
 * version are fixed on purpose: the exported file is snapshot-tested, so it must not change per commit.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    static {
        SpringDocUtils.getConfig()
            .replaceWithSchema(UnitId.class, new StringSchema().example("u7"))
            .replaceWithSchema(Position.class, new StringSchema().example("home:0"))
            .replaceWithSchema(Rank.class, new IntegerSchema().example(3));
    }

    @Bean
    OpenAPI forgottenHeroesApi() {
        return new OpenAPI().info(new Info()
            .title("Forgotten Heroes API")
            .version("v1")
            .description("Server-authoritative API of the auto-battler. Every rule runs in the engine; "
                + "errors are RFC 9457 ProblemDetail bodies with a stable `code`."));
    }
}
