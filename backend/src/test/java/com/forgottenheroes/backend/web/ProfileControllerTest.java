package com.forgottenheroes.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.forgottenheroes.backend.security.CurrentUser;
import com.forgottenheroes.backend.security.SecurityConfig;
import com.forgottenheroes.backend.service.InvalidRequestException;
import com.forgottenheroes.backend.service.ProfileService;
import com.forgottenheroes.backend.web.dto.ProfileDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** The two doors of {@code /api/me}: the profile out, the display name in, and the two kinds of 400 a bad name gets. */
@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
@WithMockUser
class ProfileControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ProfileService profiles;

    @MockitoBean
    private CurrentUser user;

    @BeforeEach
    void theCallerIsDev() {
        given(user.id()).willReturn("dev");
    }

    @Test
    void renameAnswersTheUpdatedProfile() {
        given(profiles.rename("dev", "Aoi the Brave")).willReturn(new ProfileDto("Aoi the Brave", 2, 5, null));

        assertThat(mvc.put().uri("/api/me").contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"Aoi the Brave\"}"))
            .hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("{\"displayName\":\"Aoi the Brave\",\"bestCrowns\":2,\"runsPlayed\":5}");
    }

    @Test
    void aNameTooShortIsRefusedBeforeTheServiceIsAsked() {
        assertThat(mvc.put().uri("/api/me").contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"ab\"}"))
            .hasStatus(400)
            .bodyJson()
            .isLenientlyEqualTo("{\"status\":400,\"code\":\"VALIDATION_FAILED\"}");
        then(profiles).shouldHaveNoInteractions();
    }

    @Test
    void aNameTheRulesRefuseIs400WithItsOwnCode() {
        given(profiles.rename("dev", "Admin")).willThrow(new InvalidRequestException("INVALID_DISPLAY_NAME", "That display name is not available."));

        assertThat(mvc.put().uri("/api/me").contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"Admin\"}"))
            .hasStatus(400)
            .bodyJson()
            .isLenientlyEqualTo("{\"status\":400,\"code\":\"INVALID_DISPLAY_NAME\",\"title\":\"Invalid display name\","
                + "\"detail\":\"That display name is not available.\"}");
    }
}
