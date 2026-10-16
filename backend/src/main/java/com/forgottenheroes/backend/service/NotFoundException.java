package com.forgottenheroes.backend.service;

/** Something the client named does not exist (or is not theirs): answered with 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String what, String id) {
        super(what + " " + id + " was not found");
    }
}
