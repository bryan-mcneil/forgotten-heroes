package com.forgottenheroes.engine.content;

/** The content files are wrong: a schema error, a duplicate id, a reference to a token that does not exist. */
public class ContentException extends RuntimeException {

    public ContentException(String message) {
        super(message);
    }

    public ContentException(String message, Throwable cause) {
        super(message, cause);
    }
}
