package com.forgottenheroes.backend.security;

import com.forgottenheroes.backend.web.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.json.JsonMapper;

/**
 * What a request without a valid card gets: 401 in the same RFC 9457 ProblemDetail shape as every other
 * error, with a {@code code} the client can switch on. {@code UNAUTHENTICATED} means no token at all
 * (send the player to sign in); {@code INVALID_TOKEN} means a token that failed a check (expired, another
 * pool, the id token instead of the access token: refresh it and retry). Spring Security's own entry
 * point answers with an empty body, which a browser cannot tell from a dropped connection. Never 403:
 * that status belongs to CloudFront's country block (architecture §1).
 */
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /** The decoder wraps every reason in this sentence; the reason alone is enough. */
    private static final String DECODE_PREFIX = "An error occurred while attempting to decode the Jwt: ";

    private final JsonMapper json;

    public ProblemDetailAuthenticationEntryPoint(JsonMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        boolean rejectedCard = ex instanceof OAuth2AuthenticationException;
        String code = rejectedCard ? "INVALID_TOKEN" : "UNAUTHENTICATED";
        String title = rejectedCard ? "Token rejected" : "Sign in required";
        String detail = ex instanceof OAuth2AuthenticationException oauth
            ? describe(oauth)
            : "Send a Cognito access token in the Authorization header: Bearer <token>.";

        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", GlobalExceptionHandler.TYPE_BASE + code.toLowerCase(Locale.ROOT).replace('_', '-'));
        problem.put("title", title);
        problem.put("status", HttpStatus.UNAUTHORIZED.value());
        problem.put("detail", detail);
        problem.put("code", code);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        // RFC 6750: tell the client which kind of credential this door expects
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, rejectedCard ? "Bearer error=\"invalid_token\"" : "Bearer");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.writeValueAsString(problem));
    }

    private static String describe(OAuth2AuthenticationException ex) {
        String description = ex.getError().getDescription();
        if (description == null || description.isBlank()) {
            return "The token failed a check.";
        }
        return description.startsWith(DECODE_PREFIX) ? description.substring(DECODE_PREFIX.length()) : description;
    }
}
