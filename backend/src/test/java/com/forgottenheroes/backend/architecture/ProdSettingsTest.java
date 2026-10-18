package com.forgottenheroes.backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * The prod settings file is the other place the local shortcut could sneak in: by switching the
 * {@code local} profile on next to {@code prod}, or by naming the header. Neither may ever appear there.
 */
class ProdSettingsTest {

    /** Surefire runs with the module folder as the working directory. */
    private static final Path PROD = Path.of("src", "main", "resources", "application-prod.yaml");

    @Test
    void prodSettingsNeverTurnOnTheLocalProfile() throws IOException {
        String prod = Files.readString(PROD);

        assertThat(prod).doesNotContain("profiles").doesNotContain("X-Dev-User");
        assertThat(prod).as("prod names no fallback pool: a Lambda without COGNITO_ISSUER must refuse to start")
            .contains("${COGNITO_ISSUER}");
    }
}
