package com.forgottenheroes.backend.repo.dynamo;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

/**
 * Time to live (architecture §5): items that carry an {@code expiresAt} number are deleted by DynamoDB
 * itself, for free, some time after that moment. The number is seconds since 1970, the only form the
 * sweeper reads; an item without the attribute is kept for ever.
 */
final class Ttl {

    /** The attribute name the table's TTL setting points at (switched on by the create script and the tests). */
    static final String ATTRIBUTE = "expiresAt";

    static final Duration RUN = Duration.ofDays(90);
    static final Duration GHOST = Duration.ofDays(30);
    static final Duration BATTLE_LOG = Duration.ofDays(7);

    private Ttl() {
    }

    static long expiresAt(Instant from, Duration after) {
        return from.plus(after).getEpochSecond();
    }

    /** A season's rows live until the end of the following month: "last month's board" stays readable, older ones go. */
    static long endOfNextSeason(YearMonth season) {
        return season.plusMonths(2).atDay(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
    }
}
