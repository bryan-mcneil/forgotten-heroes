package com.forgottenheroes.engine.event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The ordered list of facts one action (or one whole turn) produced. {@link #append} stamps each
 * event with the next sequence number, so a log that starts where the previous one ended keeps the
 * run's numbering contiguous.
 */
public final class EventLog {

    private final List<GameEvent> events = new ArrayList<>();
    private int lastSeq;

    public EventLog() {
        this(0);
    }

    /** A log that continues numbering after {@code lastSeq}. */
    public EventLog(int lastSeq) {
        if (lastSeq < 0) {
            throw new IllegalArgumentException("sequence cannot be negative: " + lastSeq);
        }
        this.lastSeq = lastSeq;
    }

    /** Stores the event with the next number and returns that numbered copy. */
    @SuppressWarnings("unchecked")
    public <E extends GameEvent> E append(E event) {
        E numbered = (E) event.withSeq(++lastSeq);
        events.add(numbered);
        return numbered;
    }

    /** Appends every event in order, renumbering each one. */
    public void appendAll(List<? extends GameEvent> more) {
        for (GameEvent event : more) {
            append(event);
        }
    }

    public List<GameEvent> events() {
        return List.copyOf(events);
    }

    /** The events numbered after {@code seq}; the client asks for these after a reconnect. */
    public List<GameEvent> since(int seq) {
        return events.stream().filter(event -> event.seq() > seq).toList();
    }

    /** Only the events of one type, in order; handy in tests. */
    public <E extends GameEvent> List<E> of(Class<E> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }

    public Stream<GameEvent> stream() {
        return events.stream();
    }

    public int lastSeq() {
        return lastSeq;
    }

    public int size() {
        return events.size();
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    /** One line per event, for reading a battle like a story. */
    public String story() {
        StringBuilder text = new StringBuilder();
        for (GameEvent event : events) {
            text.append(event).append('\n');
        }
        return text.toString();
    }
}
