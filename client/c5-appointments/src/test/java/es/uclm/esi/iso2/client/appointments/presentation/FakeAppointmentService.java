package es.uclm.esi.iso2.client.appointments.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.appointments.contracts.IAppointmentService;
import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.AppointmentStatus;
import es.uclm.esi.iso2.common.appointments.domain.PendingNeedView;
import es.uclm.esi.iso2.common.appointments.domain.TimeSlot;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test double of {@link IAppointmentService} for the screen of UC13. It keeps the pending needs, the free slots of each
 * test type and the appointments in memory, and gives the ids {@code APT-0004}, {@code APT-0005}… to the new
 * appointments. It does not validate, so that a test can see that the screen sends nothing when its own checks fail;
 * a test asks for a rejection of the next change with {@link #rejectNext}, for a rejection of the slots with
 * {@link #rejectSlots}, for a failure of the reading with {@link #failReads}, and for the denied access with
 * {@link #denyAll} or {@link #denyAppointments}. The operation {@code getAppointmentsOfPatient} is not used by UC13.
 */
class FakeAppointmentService implements IAppointmentService {
    /** The day of the free slots of the usual data, 30/11/2026. */
    static final LocalDate DAY = LocalDate.of(2026, 11, 30);

    private final List<PendingNeedView> needs = new ArrayList<>();
    private final Map<String, List<TimeSlot>> freeSlots = new LinkedHashMap<>();
    private final List<Appointment> appointments = new ArrayList<>();
    private final List<String> calls = new ArrayList<>();
    private final List<String> requesters = new ArrayList<>();
    private final List<String> slotQueries = new ArrayList<>();

    private boolean denyAll;
    private boolean denyAppointments;
    private List<String> nextErrors;
    private List<String> slotErrors;
    private Exception readFailure;
    private int nextNumber = 4;

    /**
     * The usual data: two pending needs (Lucía, chest X-ray, URGENT, ordered on 20/11/2026 and due on 27/11/2026;
     * Mario, knee MRI, ROUTINE, ordered on 21/11/2026 without a due date), two free slots of the chest X-ray and one of
     * the knee MRI and of the head CT on 30/11/2026, and three appointments (APT-0001 BOOKED of a chest X-ray at 08:00,
     * APT-0002 CANCELLED of a knee MRI at 11:00 and APT-0003 BOOKED of a head CT at 12:30).
     */
    static FakeAppointmentService withUsualData() {
        FakeAppointmentService service = new FakeAppointmentService();
        service.needs.add(new PendingNeedView("NEED-0001", "ORD-0001", "P-0001", "Lucía Ejemplo Prueba", "XRAY-CHEST",
                "Chest X-ray", "URGENT", LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 27)));
        service.needs.add(new PendingNeedView("NEED-0002", "ORD-0003", "P-0002", "Mario Demo Simulado", "MRI-KNEE",
                "Knee MRI", "ROUTINE", LocalDate.of(2026, 11, 21), null));
        service.freeSlots.put("XRAY-CHEST", List.of(slot("XRAY-1", 9, 0, 15), slot("XRAY-1", 9, 15, 15)));
        service.freeSlots.put("MRI-KNEE", List.of(slot("MRI-1", 10, 0, 45)));
        service.freeSlots.put("CT-HEAD", List.of(slot("CT-1", 12, 0, 30)));
        service.appointments.add(new Appointment("APT-0001", "NEED-0009", "P-0003", "XRAY-CHEST", "XRAY-1", "mgarcia",
                DAY.atTime(8, 0), DAY.atTime(8, 15), AppointmentStatus.BOOKED));
        service.appointments.add(new Appointment("APT-0002", "NEED-0008", "P-0002", "MRI-KNEE", "MRI-1", "mgarcia",
                DAY.atTime(11, 0), DAY.atTime(11, 45), AppointmentStatus.CANCELLED));
        service.appointments.add(new Appointment("APT-0003", "NEED-0007", "P-0004", "CT-HEAD", "CT-1", "mgarcia",
                DAY.atTime(12, 30), DAY.atTime(13, 0), AppointmentStatus.BOOKED));
        return service;
    }

    /** A slot of {@link #DAY} in a room, starting at the given hour and minute and lasting some minutes. */
    static TimeSlot slot(String room, int hour, int minute, int minutes) {
        LocalDateTime start = DAY.atTime(hour, minute);
        return new TimeSlot(room, start, start.plusMinutes(minutes));
    }

    /** From now on every call is denied. */
    void denyAll() {
        denyAll = true;
    }

    /** From now on only the reading of the appointments is denied (the needs are read). */
    void denyAppointments() {
        denyAppointments = true;
    }

    /** The next booking, move or cancellation is rejected with these errors. */
    void rejectNext(String... errors) {
        nextErrors = List.of(errors);
    }

    /** Every request of free slots is rejected with these errors. */
    void rejectSlots(String... errors) {
        slotErrors = List.of(errors);
    }

    /** Every reading of the pending needs fails with this exception (a {@link ValidationException} or a
     *  {@link RuntimeException}). */
    void failReads(Exception failure) {
        readFailure = failure;
    }

    /** Every call, in order, as {@code operation arguments} (the reads included). */
    List<String> calls() {
        return calls;
    }

    /** The requester of every call, in order. */
    List<String> requesters() {
        return requesters;
    }

    /** Every request of free slots, as {@code testTypeId from to}. */
    List<String> slotQueries() {
        return slotQueries;
    }

    /** The calls that change something: bookings, moves and cancellations. */
    List<String> changes() {
        return calls.stream().filter(call -> call.startsWith("book") || call.startsWith("reschedule")
                || call.startsWith("cancel")).toList();
    }

    private void check(String requester, String call) throws AccessDeniedException {
        requesters.add(requester);
        calls.add(call);
        if (denyAll) {
            throw new AccessDeniedException(requester);
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
    public List<PendingNeedView> getPendingSchedulingNeeds(String requester)
            throws AccessDeniedException, ValidationException {
        check(requester, "needs");
        if (readFailure instanceof ValidationException validation) {
            throw validation;
        }
        if (readFailure instanceof RuntimeException runtime) {
            throw runtime;
        }
        return List.copyOf(needs);
    }

    @Override
    public List<TimeSlot> getFreeSlots(String requester, String testTypeId, LocalDate fromDate, LocalDate toDate)
            throws AccessDeniedException, ValidationException {
        check(requester, "slots " + testTypeId);
        slotQueries.add(testTypeId + " " + fromDate + " " + toDate);
        if (slotErrors != null) {
            throw new ValidationException(slotErrors);
        }
        return freeSlots.getOrDefault(testTypeId, List.of()).stream()
                .filter(slot -> !slot.getStart().toLocalDate().isBefore(fromDate)
                        && !slot.getStart().toLocalDate().isAfter(toDate))
                .toList();
    }

    @Override
    public Appointment bookAppointment(String requester, String needId, TimeSlot slot)
            throws AccessDeniedException, ValidationException {
        check(requester, "book " + needId + " " + slot.getRoom() + " " + slot.getStart() + " " + slot.getEnd());
        rejectIfAsked();
        PendingNeedView need = needs.stream().filter(n -> n.getNeedId().equals(needId)).findFirst().orElseThrow();
        Appointment created = new Appointment(String.format("APT-%04d", nextNumber++), needId, need.getPatientId(),
                need.getTestTypeId(), slot.getRoom(), "mgarcia", slot.getStart(), slot.getEnd(),
                AppointmentStatus.BOOKED);
        needs.remove(need);
        appointments.add(created);
        return created;
    }

    @Override
    public Appointment rescheduleAppointment(String requester, String appointmentId, TimeSlot newSlot)
            throws AccessDeniedException, ValidationException {
        check(requester, "reschedule " + appointmentId + " " + newSlot.getRoom() + " " + newSlot.getStart());
        rejectIfAsked();
        return replace(appointmentId, newSlot, AppointmentStatus.RESCHEDULED);
    }

    @Override
    public Appointment cancelAppointment(String requester, String appointmentId)
            throws AccessDeniedException, ValidationException {
        check(requester, "cancel " + appointmentId);
        rejectIfAsked();
        return replace(appointmentId, null, AppointmentStatus.CANCELLED);
    }

    private Appointment replace(String appointmentId, TimeSlot slot, AppointmentStatus status) {
        for (int index = 0; index < appointments.size(); index++) {
            Appointment old = appointments.get(index);
            if (old.getAppointmentId().equals(appointmentId)) {
                Appointment changed = new Appointment(appointmentId, old.getNeedId(), old.getPatientId(),
                        old.getTestTypeId(), slot == null ? old.getRoom() : slot.getRoom(), old.getPhysician(),
                        slot == null ? old.getStart() : slot.getStart(), slot == null ? old.getEnd() : slot.getEnd(),
                        status);
                appointments.set(index, changed);
                return changed;
            }
        }
        throw new IllegalStateException("No appointment " + appointmentId);
    }

    @Override
    public List<Appointment> getAppointments(String requester) throws AccessDeniedException {
        check(requester, "appointments");
        if (denyAppointments) {
            throw new AccessDeniedException(requester);
        }
        return List.copyOf(appointments);
    }

    @Override
    public List<Appointment> getAppointmentsOfPatient(String requester, String patientId) {
        throw new UnsupportedOperationException("Not used by UC13");
    }
}
