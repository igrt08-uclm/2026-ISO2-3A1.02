package es.uclm.esi.iso2.client.appointments.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.appointments.contracts.IDailyScheduleService;
import es.uclm.esi.iso2.common.appointments.domain.ScheduleEntry;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test double of {@link IDailyScheduleService} for the screen of UC14. It returns the lines that the test gives it and
 * records every call. A test asks for a rejection with {@link #rejectWith}, for the denied access with
 * {@link #denyAll} and for an unexpected failure with {@link #failWith}.
 */
class FakeDailyScheduleService implements IDailyScheduleService {
    private final Map<String, List<ScheduleEntry>> schedule = new LinkedHashMap<>();
    private final List<ScheduleEntry> worklist = new ArrayList<>();
    private final List<String> calls = new ArrayList<>();

    private List<String> errors;
    private boolean denied;
    private RuntimeException failure;

    /** The lines that the daily schedule returns for a room, in this order. */
    void putRoom(String room, ScheduleEntry... lines) {
        schedule.put(room, List.of(lines));
    }

    /** The lines that both worklists return, in this order. */
    void useWorklist(ScheduleEntry... lines) {
        worklist.clear();
        worklist.addAll(List.of(lines));
    }

    /** From now on every reading is rejected with these errors. */
    void rejectWith(String... reasons) {
        errors = List.of(reasons);
    }

    /** From now on every reading is denied. */
    void denyAll() {
        denied = true;
    }

    /** From now on every reading fails. */
    void failWith(String message) {
        failure = new IllegalStateException(message);
    }

    List<String> calls() {
        return calls;
    }

    @Override
    public Map<String, List<ScheduleEntry>> getDailySchedule(String requester, LocalDate date)
            throws AccessDeniedException, ValidationException {
        read(requester, "schedule " + date);
        return schedule;
    }

    @Override
    public List<ScheduleEntry> getRoomWorklist(String requester, LocalDate date, String room)
            throws AccessDeniedException, ValidationException {
        read(requester, "room " + date + " [" + room + "]");
        return worklist;
    }

    @Override
    public List<ScheduleEntry> getPhysicianWorklist(String requester, LocalDate date, String physician)
            throws AccessDeniedException, ValidationException {
        read(requester, "physician " + date + " [" + physician + "]");
        return worklist;
    }

    private void read(String requester, String call) throws AccessDeniedException, ValidationException {
        calls.add(call);
        if (denied) {
            throw new AccessDeniedException(requester);
        }
        if (errors != null) {
            throw new ValidationException(errors);
        }
        if (failure != null) {
            throw failure;
        }
    }
}
