package com.forgottenheroes.backend.security.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.forgottenheroes.backend.security.SecurityContextCurrentUser;
import com.forgottenheroes.backend.service.ProfileService;
import com.forgottenheroes.backend.web.ProfileController;
import com.forgottenheroes.backend.web.dto.ProfileDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** The laptop rules: no token anywhere, the header names the player, no header means {@code dev}. */
@WebMvcTest(ProfileController.class)
@Import({LocalDevSecurityConfig.class, SecurityContextCurrentUser.class})
@ActiveProfiles("local")
class LocalDevSecurityTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ProfileService profiles;

    @Test
    void theHeaderNamesThePlayer() {
        given(profiles.me("bryan")).willReturn(new ProfileDto("Bryan", 3, 2, null));

        assertThat(mvc.get().uri("/api/me").header("X-Dev-User", "bryan"))
            .hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("{\"displayName\":\"Bryan\",\"bestCrowns\":3}");
    }

    @Test
    void noHeaderMeansDev() {
        given(profiles.me("dev")).willReturn(new ProfileDto("Nameless", 0, 0, null));

        assertThat(mvc.get().uri("/api/me")).hasStatusOk();
        then(profiles).should().me("dev");
    }
}
