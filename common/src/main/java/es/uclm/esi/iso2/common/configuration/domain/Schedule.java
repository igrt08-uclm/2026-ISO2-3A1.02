package es.uclm.esi.iso2.common.configuration.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

/** When a room or resource is available (UC17). It is deactivated, never deleted. */
public class Schedule {
    private final String resource;
    private final Set<DayOfWeek> daysOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final boolean active;

    /**
     * Creates a schedule.
     *
     * @param resource the id of the room or resource
     * @param daysOfWeek the days of the week when it is available
     * @param startTime the time when the availability starts
     * @param endTime the time when it ends, after the start
     * @param active whether it is applied
     */
    public Schedule(String resource, Set<DayOfWeek> daysOfWeek, LocalTime startTime, LocalTime endTime,
            boolean active) {
        this.resource = resource;
        this.daysOfWeek = daysOfWeek == null ? null : Set.copyOf(daysOfWeek);
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = active;
    }

    /** Returns the id of the room or resource. */
    public String getResource() {
        return resource;
    }

    /** Returns the days of the week when it is available. */
    public Set<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    /** Returns the time when the availability starts. */
    public LocalTime getStartTime() {
        return startTime;
    }

    /** Returns the time when the availability ends. */
    public LocalTime getEndTime() {
        return endTime;
    }

    /** Returns whether it is applied. */
    public boolean isActive() {
        return active;
    }

    /** Returns a copy of this schedule that is not active. */
    public Schedule deactivated() {
        return new Schedule(resource, daysOfWeek, startTime, endTime, false);
    }
}
