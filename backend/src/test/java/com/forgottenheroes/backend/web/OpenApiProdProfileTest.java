package com.forgottenheroes.backend.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** With the prod profile the API does not describe itself: {@code application-prod.yaml} turns springdoc off. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class OpenApiProdProfileTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void apiDocsAndSwaggerUiAreGoneInProd() {
        assertThat(mvc.get().uri("/v3/api-docs")).hasStatus(404);
        assertThat(mvc.get().uri("/swagger-ui.html")).hasStatus(404);
        assertThat(mvc.get().uri("/api/health")).hasStatusOk();
    }
}
