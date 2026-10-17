package com.forgottenheroes.backend.repo.dynamo;

/** How the two labels of every item are spelled. One place, so a typo cannot split a drawer in two. */
final class Keys {

    static final String PROFILE = "PROFILE";

    private Keys() {
    }

    /** The drawer of one player: the profile now, every run they played from Step 28-b. */
    static String user(String userId) {
        return "USER#" + userId;
    }
}
