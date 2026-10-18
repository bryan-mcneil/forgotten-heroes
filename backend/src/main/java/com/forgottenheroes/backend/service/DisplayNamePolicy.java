package com.forgottenheroes.backend.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The naming rules for the name other players see (Step 32): 3 to 16 characters, letters, digits and
 * single spaces, and not on the blocklist. One place for the rules, so the controller, the service and
 * the tests cannot disagree; a broken rule is an {@link InvalidRequestException} with code
 * {@code INVALID_DISPLAY_NAME}, which the handler turns into 400.
 */
public final class DisplayNamePolicy {

    public static final int MIN = 3;
    public static final int MAX = 16;
    static final String CODE = "INVALID_DISPLAY_NAME";

    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9 ]+");
    private static final Pattern RUNS_OF_SPACE = Pattern.compile("\\s+");
    private static final Set<String> BLOCKED = loadBlocklist();

    private DisplayNamePolicy() {
    }

    /** The name as it will be stored: trimmed, inner spaces collapsed to one, checked against every rule. */
    public static String clean(String requested) {
        String name = requested == null ? "" : RUNS_OF_SPACE.matcher(requested.trim()).replaceAll(" ");
        if (name.length() < MIN || name.length() > MAX) {
            throw new InvalidRequestException(CODE, "A display name has " + MIN + " to " + MAX + " characters.");
        }
        if (!ALLOWED.matcher(name).matches()) {
            throw new InvalidRequestException(CODE, "A display name uses letters, digits and spaces only.");
        }
        String lower = name.toLowerCase(Locale.ROOT);
        // whole words, and the name with its spaces removed: "Cassandra" passes, "a d m i n" does not
        boolean blocked = BLOCKED.contains(lower.replace(" ", ""))
            || Arrays.stream(lower.split(" ")).anyMatch(BLOCKED::contains);
        if (blocked) {
            throw new InvalidRequestException(CODE, "That display name is not available.");
        }
        return name;
    }

    private static Set<String> loadBlocklist() {
        try (InputStream in = DisplayNamePolicy.class.getResourceAsStream("/display-name-blocklist.txt")) {
            if (in == null) {
                throw new IllegalStateException("display-name-blocklist.txt is missing from the classpath");
            }
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> line.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
