package com.forgottenheroes.backend.web;

import com.forgottenheroes.backend.service.ActiveRunException;
import com.forgottenheroes.backend.service.NotFoundException;
import com.forgottenheroes.backend.repo.VersionConflictException;
import com.forgottenheroes.engine.resolver.RuleViolation;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Every error leaves the API as an RFC 9457 ProblemDetail with a stable {@code code} the client can
 * switch on (architecture §4.2). One class, one table:
 * <pre>
 *   RuleViolation (engine)          → 422  code = the engine's Code      "you may not do that"
 *   validation / unreadable JSON    → 400  VALIDATION_FAILED / MALFORMED_JSON
 *   NotFoundException               → 404  NOT_FOUND
 *   VersionConflictException        → 409  VERSION_CONFLICT (+ currentVersion)
 *   ActiveRunException              → 409  RUN_ALREADY_ACTIVE (+ activeRunId)
 *   UnsupportedOperationException   → 501  NOT_IMPLEMENTED (Step 26 stubs)
 *   anything else                   → 500  INTERNAL_ERROR, logged, no stack trace to the client
 * </pre>
 * 403 is never produced here: CloudFront owns it for the geo-block (architecture §1).
 * Extending {@link ResponseEntityExceptionHandler} gives us Spring's own mappings for the web-layer
 * exceptions (unknown route, wrong method, bad path variable …) and lets us add a {@code code} to them.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String TYPE_BASE = "https://heroes.bryanmcneil.pro/errors/";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuleViolation.class)
    ProblemDetail ruleViolation(RuleViolation ex) {
        String prefix = ex.code() + ": ";
        String message = ex.getMessage();
        String detail = message.startsWith(prefix) ? message.substring(prefix.length()) : message;
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "rule-violation", ex.code().name(), titleOf(ex.code().name()), detail);
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "NOT_FOUND", "Not found", ex.getMessage());
    }

    @ExceptionHandler(VersionConflictException.class)
    ProblemDetail versionConflict(VersionConflictException ex) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "version-conflict", "VERSION_CONFLICT", "Stale version", ex.getMessage());
        problem.setProperty("currentVersion", ex.actual());
        return problem;
    }

    @ExceptionHandler(ActiveRunException.class)
    ProblemDetail activeRun(ActiveRunException ex) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "run-already-active", "RUN_ALREADY_ACTIVE", "A run is in progress", ex.getMessage());
        problem.setProperty("activeRunId", ex.activeRunId());
        return problem;
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    ProblemDetail notImplemented(UnsupportedOperationException ex) {
        return problem(HttpStatus.NOT_IMPLEMENTED, "not-implemented", "NOT_IMPLEMENTED", "Not implemented yet", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception ex) {
        log.error("unhandled error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal", "INTERNAL_ERROR", "Something went wrong",
            "The server hit an unexpected error. It has been logged.");
    }

    /** A request body that parsed but broke a constraint ({@code @NotNull}, {@code @PositiveOrZero} …). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
            .toList();
        String detail = ex.getBindingResult().getFieldErrors().stream()
            .map(GlobalExceptionHandler::describe)
            .sorted()
            .reduce((a, b) -> a + "; " + b)
            .orElse("Invalid request");
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation", "VALIDATION_FAILED", "Invalid request", detail);
        problem.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(problem);
    }

    /** A body that is not JSON, names an unknown field, or an unknown {@code type}. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "malformed-json", "MALFORMED_JSON", "Unreadable request body",
            firstLine(ex.getMostSpecificCause().getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(problem);
    }

    /** Spring's own ProblemDetails (unknown route, wrong method, bad path variable …) get a {@code code} too. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        // the parent builds the ProblemDetail itself when body is null (unknown route, wrong method …), so look afterwards
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem
            && (problem.getProperties() == null || !problem.getProperties().containsKey("code"))) {
            HttpStatus status = HttpStatus.resolve(statusCode.value());
            String code = status == null ? "HTTP_" + statusCode.value() : status.name();
            problem.setType(URI.create(TYPE_BASE + code.toLowerCase(Locale.ROOT).replace('_', '-')));
            problem.setProperty("code", code);
        }
        return response;
    }

    private static ProblemDetail problem(HttpStatus status, String typeSlug, String code, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_BASE + typeSlug));
        problem.setTitle(title);
        problem.setProperty("code", code);
        return problem;
    }

    /** {@code NOT_ENOUGH_GOLD} → {@code Not enough gold}. */
    static String titleOf(String code) {
        String words = code.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private static String describe(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "The request body could not be read";
        }
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }
}
