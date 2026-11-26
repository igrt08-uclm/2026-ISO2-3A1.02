package es.uclm.esi.iso2.client.appointments.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.appointments.contracts.IWaitlistService;
import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.AppointmentStatus;
import es.uclm.esi.iso2.common.appointments.domain.TimeSlot;
import es.uclm.esi.iso2.common.appointments.domain.WaitlistEntry;
import es.uclm.esi.iso2.common.appointments.domain.WaitlistStatus;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Test double of {@link IWaitlistService} for the screen of UC15. It keeps the entries and the openings in memory and
 * records every call. It does not validate, so that a test can see that the screen sends nothing when its own checks
 * fail; a test asks for a rejection of the next change with {@link #rejectNext}, for the denied access with
 * {@link #denyAll}, for an unexpected failure with {@link #failAll} and for a failure of the reading of the waitlist
 * with {@link #failWaitlist}.
 */
class FakeWaitlistService implements IWaitlistService {
    private final List<WaitlistEntry> entries = new ArrayList<>();
    private final List<TimeSlot> openings = new ArrayList<>();
    private final List<String> calls = new ArrayList<>();

    private WaitlistEntry proposal;
    private List<String> nextErrors;
    private boolean denyAll;
    private RuntimeException failure;
    private RuntimeException waitlistFailure;

    /**
     * Two entries waiting for the chest X-ray (WL-0001 of P-0001 URGENT and WL-0002 of P-0002 ROUTINE), none for the
     * other test types, and two openings of the chest X-ray on 30/11/2026. The proposal is WL-0002.
     */
    static FakeWaitlistService withUsualData() {
        FakeWaitlistService service = new FakeWaitlistService();
        service.entries.add(entry("WL-0001", "NEED-0001", "P-0001", "XRAY-CHEST", "URGENT"));
        service.entries.add(entry("WL-0002", "NEED-0005", "P-0002", "XRAY-CHEST", "ROUTINE"));
        service.openings.add(FakeAppointmentService.slot("XRAY-1", 9, 0, 15));
        service.openings.add(FakeAppointmentService.slot("XRAY-1", 9, 15, 15));
        service.proposal = service.entries.get(1);
        return service;
    }

    private static WaitlistEntry entry(String entryId, String needId, String patientId, String testTypeId,
            String priorityId) {
        return new WaitlistEntry(entryId, needId, patientId, testTypeId, priorityId,
                FakeAppointmentService.DAY.minusDays(1).atTime(8, 0), WaitlistStatus.WAITING);
    }

    void proposeNobody() {
        proposal = null;
    }

    void keepOneOpening() {
        openings.remove(1);
    }

    void rejectNext(String... errors) {
        nextErrors = List.of(errors);
    }

    void denyAll() {
        denyAll = true;
    }

    void failAll(RuntimeException exception) {
        failure = exception;
    }

    void failWaitlist(RuntimeException exception) {
        waitlistFailure = exception;
    }

    List<String> calls() {
        return calls;
    }

    /** The queries of the waitlist, in order. */
    List<String> queries() {
        return calls.stream().filter(call -> call.startsWith("waitlist ")).toList();
    }

    /** The calls that change something. */
    List<String> changes() {
        return calls.stream().filter(call -> call.startsWith("add ") || call.startsWith("change ")
                || call.startsWith("remove ") || call.startsWith("fill ")).toList();
    }

    private void check(String requester, String call) throws AccessDeniedException {
        calls.add(call);
        if (denyAll) {
            throw new AccessDeniedException(requester);
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void rejectIfAsked() throws ValidationException {
        if (nextErrors != null) {
            List<String> errors = nextErrors;
            nextErrors = null;
            throw new ValidationException(errors);
        }
    }

    @Override
    public List<WaitlistEntry> getWaitlist(String requester, String testTypeId) throws AccessDeniedException {
        check(requester, "waitlist " + testTypeId);
        if (waitlistFailure != null) {
            throw waitlistFailure;
        }
        return entries.stream().filter(entry -> entry.getTestTypeId().equals(testTypeId)).toList();
    }

    @Override
    public WaitlistEntry addToWaitlist(String requester, String needId, String priorityId)
            throws AccessDeniedException, ValidationException {
        check(requester, "add " + needId + " " + priorityId);
        rejectIfAsked();
        WaitlistEntry added = entry("WL-0003", needId, "P-0001", "XRAY-CHEST", priorityId);
        entries.add(added);
        return added;
    }

    @Override
    public WaitlistEntry changeWaitlistPriority(String requester, String entryId, String priorityId)
            throws AccessDeniedException, ValidationException {
        check(requester, "change " + entryId + " " + priorityId);
        rejectIfAsked();
        int index = indexOf(entryId);
        WaitlistEntry old = entries.get(index);
        WaitlistEntry changed = entry(entryId, old.getNeedId(), old.getPatientId(), old.getTestTypeId(), priorityId);
        entries.set(index, changed);
        return changed;
    }

    @Override
    public WaitlistEntry removeFromWaitlist(String requester, String entryId)
            throws AccessDeniedException, ValidationException {
        check(requester, "remove " + entryId);
        rejectIfAsked();
        return entries.remove(indexOf(entryId));
    }

    @Override
    public List<TimeSlot> getCalendarOpenings(String requester, String testTypeId, LocalDate fromDate,
            LocalDate toDate) throws AccessDeniedException, ValidationException {
        check(requester, "openings " + testTypeId + " " + fromDate + " " + toDate);
        rejectIfAsked();
        return openings.stream().filter(slot -> slot.getStart().toLocalDate().equals(fromDate)).toList();
    }

    @Override
    public WaitlistEntry proposePatientForOpening(String requester, String testTypeId, TimeSlot opening)
            throws AccessDeniedException, ValidationException {
        check(requester, "propose " + testTypeId + " " + opening.getStart());
        rejectIfAsked();
        return proposal;
    }

    @Override
    public Appointment fillOpening(String requester, String entryId, TimeSlot opening)
            throws AccessDeniedException, ValidationException {
        check(requester, "fill " + entryId + " " + opening.getStart());
        rejectIfAsked();
        WaitlistEntry filled = entries.remove(indexOf(entryId));
        return new Appointment("APT-0009", filled.getNeedId(), filled.getPatientId(), filled.getTestTypeId(),
                opening.getRoom(), "mgarcia", opening.getStart(), opening.getEnd(), AppointmentStatus.BOOKED);
    }

    private int indexOf(String entryId) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).getEntryId().equals(entryId)) {
                return index;
            }
        }
        throw new IllegalStateException("No entry " + entryId);
    }
}
