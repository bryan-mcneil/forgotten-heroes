package com.forgottenheroes.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.forgottenheroes.backend.service.ContentService;
import com.forgottenheroes.backend.service.ProfileService;
import com.forgottenheroes.backend.web.ContentController;
import com.forgottenheroes.backend.web.ProfileController;
import com.forgottenheroes.backend.web.dto.ContentDto;
import com.forgottenheroes.backend.web.dto.ProfileDto;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The doorman in front of two doors, one public and one not. The decoder is a mock, so the test says
 * what each token means instead of fetching Cognito's keys; everything after the decoder is the real
 * chain: the Bearer header is read, the subject becomes {@code CurrentUser.id()}, failures become 401.
 */
@WebMvcTest({ProfileController.class, ContentController.class})
@Import({SecurityConfig.class, SecurityContextCurrentUser.class})
class SecurityConfigTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private JwtDecoder decoder;

    @MockitoBean
    private ProfileService profiles;

    @MockitoBean
    private ContentService content;

    @Test
    void noTokenAnswers401AsAProblemDetail() {
        assertThat(mvc.get().uri("/api/me"))
            .hasStatus(401)
            .hasHeader("WWW-Authenticate", "Bearer")
            .hasContentTypeCompatibleWith("application/problem+json")
            .bodyJson()
            .isLenientlyEqualTo("{\"status\":401,\"code\":\"UNAUTHENTICATED\",\"title\":\"Sign in required\"}");
        then(profiles).shouldHaveNoInteractions();
    }

    @Test
    void aRejectedTokenAnswers401WithTheReason() {
        given(decoder.decode("stale")).willThrow(new BadJwtException("Jwt expired at 2026-10-17T20:00:00Z"));

        assertThat(mvc.get().uri("/api/me").header("Authorization", "Bearer stale"))
            .hasStatus(401)
            .hasHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"")
            .bodyJson()
            .isLenientlyEqualTo("{\"status\":401,\"code\":\"INVALID_TOKEN\",\"detail\":\"Jwt expired at 2026-10-17T20:00:00Z\"}");
        then(profiles).shouldHaveNoInteractions();
    }

    @Test
    void aGoodTokenOpensTheDoorAsItsSubject() {
        given(decoder.decode("good")).willReturn(Jwt.withTokenValue("good")
            .header("alg", "RS256")
            .subject("7f3a9c2e")
            .claim("token_use", "access")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build());
        given(profiles.me("7f3a9c2e")).willReturn(new ProfileDto("Nameless", 0, 0, null));

        assertThat(mvc.get().uri("/api/me").header("Authorization", "Bearer good"))
            .hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("{\"displayName\":\"Nameless\"}");
        then(profiles).should().me("7f3a9c2e");
    }

    @Test
    void contentIsPublic() {
        given(content.content()).willReturn(new ContentDto(List.of(), List.of(), List.of()));

        assertThat(mvc.get().uri("/api/content")).hasStatusOk();
        then(decoder).shouldHaveNoInteractions();
    }
}
