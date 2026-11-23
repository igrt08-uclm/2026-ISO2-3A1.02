package es.uclm.esi.iso2.common.appointments.domain;

import java.time.LocalDateTime;

/**
 * A slot of a room (UC13, D40): the free ones are computed from the schedule of the room and the duration of the test
 * type, and they are never saved. A slot is identified by its room and its start, so two slots of the same room with the
 * same start are the same slot.
 */
public class TimeSlot {
    private final String room;
    private final LocalDateTime start;
    private final LocalDateTime end;

    /**
     * Creates the slot.
     *
     * @param room the room of the slot (C2)
     * @param start the instant when the slot starts
     * @param end the instant when the slot ends
     */
    public TimeSlot(String room, LocalDateTime start, LocalDateTime end) {
        this.room = room;
        this.start = start;
        this.end = end;
    }

    /** Returns the room of the slot. */
    public String getRoom() {
        return room;
    }

    /** Returns the instant when the slot starts. */
    public LocalDateTime getStart() {
        return start;
    }

    /** Returns the instant when the slot ends. */
    public LocalDateTime getEnd() {
        return end;
    }
}
