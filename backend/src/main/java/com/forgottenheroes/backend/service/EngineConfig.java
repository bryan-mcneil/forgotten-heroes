package com.forgottenheroes.backend.service;

import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.ContentLoader;
import com.forgottenheroes.engine.session.GameSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The engine as beans: the content files loaded once at startup and one {@code GameSession} for
 * every request. Both are stateless between calls, so one instance serves the whole app.
 */
@Configuration(proxyBeanMethods = false)
public class EngineConfig {

    @Bean
    ContentIndex contentIndex() {
        return ContentLoader.load();
    }

    @Bean
    GameSession gameSession(ContentIndex content) {
        return new GameSession(content);
    }
}
