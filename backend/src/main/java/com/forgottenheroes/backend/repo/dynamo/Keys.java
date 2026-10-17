package com.forgottenheroes.backend.repo.dynamo;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * How the two labels of every item are spelled (architecture §5). One place, so a typo cannot split
 * a drawer in two, and so the order inside a drawer, the sort key, is designed here and nowhere else.
 *
 * <pre>
 *   PK                SK                            what
 *   USER#&lt;userId&gt;     PROFILE                       the player (Step 28-a)
 *   USER#&lt;userId&gt;     RUN#&lt;runId&gt;                   one run, whole state as JSON
 *   GHOST#T&lt;turn&gt;     &lt;crowns&gt;#&lt;time&gt;#&lt;runId&gt;       a band saved at the end of that turn
 *   RUN#&lt;runId&gt;       BATTLE#&lt;turn&gt;                 one battle of that run, for replay
 * </pre>
 */
final class Keys {

    static final String PROFILE = "PROFILE";

    /** Fixed width, so the text order of two times is their time order ("…26.700Z" before "…26.728Z"). */
    private static final DateTimeFormatter SORTABLE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private Keys() {
    }

    /** The drawer of one player: the profile, then every run they played. */
    static String user(String userId) {
        return "USER#" + userId;
    }

    /** A run: its place in the owner's drawer, and the label of the drawer that holds its battles. */
    static String run(String runId) {
        return "RUN#" + runId;
    }

    /** The drawer of every ghost saved at the end of one turn, whoever saved it. */
    static String ghostTurn(int turn) {
        return "GHOST#T" + turn;
    }

    /**
     * A ghost's place in its drawer: crowns first, two digits so that "07" sorts before "10", then
     * the time, then the run. One Query can then read "ghosts with 6 to 8 crowns" as a range of
     * the sort key (Step 30), and the newest come first within the same crowns.
     */
    static String ghostSort(int crowns, Instant savedAt, String runId) {
        return String.format("%02d#%s#%s", crowns, SORTABLE_TIME.format(savedAt), runId);
    }

    /** The lowest key a crowns window can hold: {@code "06#"} for 6 crowns (Step 30's Query reads from here). */
    static String ghostLowerBound(int crownsMin) {
        return String.format("%02d#", crownsMin);
    }

    /** The first key above the window: {@code "09#"} for 8 crowns; no real key equals it, so "between" stays exact. */
    static String ghostUpperBound(int crownsMax) {
        return String.format("%02d#", crownsMax + 1);
    }

    /** A battle's place in its run's drawer: three digits, so turn 2 stays before turn 10. */
    static String battle(int turn) {
        return String.format("BATTLE#%03d", turn);
    }
}
