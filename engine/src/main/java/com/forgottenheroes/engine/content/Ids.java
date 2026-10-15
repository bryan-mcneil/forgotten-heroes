package com.forgottenheroes.engine.content;

import java.util.regex.Pattern;

/** Content ids are lower_snake_case ("plague_rat"), so they are safe in URLs, file names and JSON keys. */
final class Ids {

    private static final Pattern SHAPE = Pattern.compile("[a-z][a-z0-9_]*");

    private Ids() { }

    static void require(String id, String what) {
        if (id == null || !SHAPE.matcher(id).matches()) {
            throw new IllegalArgumentException(what + " id must be lower_snake_case, got '" + id + "'");
        }
    }
}
