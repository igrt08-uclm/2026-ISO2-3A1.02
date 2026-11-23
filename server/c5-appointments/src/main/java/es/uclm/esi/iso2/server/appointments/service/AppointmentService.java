package es.uclm.esi.iso2.server.appointments.service;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.IAuditRecorder;
import es.uclm.esi.iso2.common.access.contracts.IRoleManagementService;
import es.uclm.esi.iso2.common.appointments.contracts.IAppointmentService;
import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.AppointmentStatus;
import es.uclm.esi.iso2.common.appointments.domain.PendingNeedView;
import es.uclm.esi.iso2.common.appointments.domain.TimeSlot;
import es.uclm.esi.iso2.common.configuration.contracts.ISystemParameterService;
import es.uclm.esi.iso2.common.configuration.domain.Schedule;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import es.uclm.esi.iso2.common.orders.contracts.IClinicalOrderService;
import es.uclm.esi.iso2.common.orders.domain.ClinicalOrder;
import es.uclm.esi.iso2.common.orders.domain.DiagnosticOrder;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeed;
import es.uclm.esi.iso2.common.patients.contracts.IPatientService;
import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.appointments.persistence.IAppointmentRepository;
import es.uclm.esi.iso2.server.appointments.persistence.InMemoryAppointmentRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Books, moves and cancels the appointments of the patients (UC13, Control_ManageAppointments). Every operation needs
 * {@code MANAGE_APPOINTMENTS}; a denied attempt is registered as {@code APPOINTMENT_ACCESS_DENIED} and the bookings,
 * moves and cancellations as {@code APPOINTMENT_BOOKED}, {@code APPOINTMENT_RESCHEDULED} and
 * {@code APPOINTMENT_CANCELLED}. The free slots come from the schedule of the room of the test type and from the
 * appointments that are not cancelled, and an appointment is only booked in a free slot of the room of its test type,
 * inside the schedule and not in the past, for an active patient. Booking marks the scheduling need as booked in C4;
 * cancelling leaves it pending again. Appointments are never deleted (D-022).
 */
public class AppointmentService implements IAppointmentService {
    private static final String MANAGE_APPOINTMENTS = "MANAGE_APPOINTMENTS";
    private static final String DENIED = "APPOINTMENT_ACCESS_DENIED";
    private static final String ACCESSED = "APPOINTMENT_ACCESSED";
    private static final String BOOKED = "APPOINTMENT_BOOKED";
    private static final String RESCHEDULED = "APPOINTMENT_RESCHEDULED";
    private static final String CANCELLED = "APPOINTMENT_CANCELLED";
    private static final String APPOINTMENTS = "appointments";
    private static final String APPOINTMENT = "APPOINTMENT:";
    private static final DateTimeFormatter MOMENT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final IAppointmentRepository appointments;
    private final IClinicalOrderService orderService;
    private final IPatientService patientService;
    private final ISystemParameterService systemParameterService;
    private final IRoleManagementService roleManagementService;
    private final IAuditRecorder auditRecorder;
    private final Clock clock;
    private int lastAppointmentNumber;

    /**
     * Creates the service with the given repository and clock.
     *
     * @param appointments where the appointments are kept
     * @param orderService the contract of C4, to read the needs and their orders and to mark them
     * @param patientService the contract of C3, to read the patient
     * @param systemParameterService the contract of C2, to read the test types and the schedules
     * @param roleManagementService used to know the permissions of the requester (contract of C1)
     * @param auditRecorder where the accesses, the bookings and the denied attempts are registered (contract of C1)
     * @param clock gives the current moment, to reject the slots of the past
     */
    public AppointmentService(IAppointmentRepository appointments, IClinicalOrderService orderService,
            IPatientService patientService, ISystemParameterService systemParameterService,
            IRoleManagementService roleManagementService, IAuditRecorder auditRecorder, Clock clock) {
        this.appointments = appointments;
        this.orderService = orderService;
        this.patientService = patientService;
        this.systemParameterService = systemParameterService;
        this.roleManagementService = roleManagementService;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    /**
     * Creates the service with an empty in-memory repository (D-030) and the clock of the system.
     *
     * @param orderService the contract of C4
     * @param patientService the contract of C3
     * @param systemParameterService the contract of C2
     * @param roleManagementService the contract of C1 for the permissions
     * @param auditRecorder the contract of C1 for the audit log
     */
    public AppointmentService(IClinicalOrderService orderService, IPatientService patientService,
            ISystemParameterService systemParameterService, IRoleManagementService roleManagementService,
            IAuditRecorder auditRecorder) {
        this(new InMemoryAppointmentRepository(), orderService, patientService, systemParameterService,
                roleManagementService, auditRecorder, Clock.systemDefaultZone());
    }

    /**
     * Loads three invented appointments, only if the system starts with no appointments at all. It is called when
     * the program starts, not by a user, so it does not need a permission and it is not registered in the audit log.
     */
    public void loadSeedData() {
        if (appointments.findAll().isEmpty()) {
            AppointmentSeedData.appointments(nextWorkingDay(LocalDate.now(clock))).forEach(appointments::save);
        }
    }

    /** The first working day (Monday to Friday) after {@code day}. */
    static LocalDate nextWorkingDay(LocalDate day) {
        LocalDate next = day.plusDays(1);
        while (next.getDayOfWeek() == DayOfWeek.SATURDAY || next.getDayOfWeek() == DayOfWeek.SUNDAY) {
            next = next.plusDays(1);
        }
        return next;
    }

    @Override
    public List<PendingNeedView> getPendingSchedulingNeeds(String requester)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, APPOINTMENTS);
        List<PendingNeedView> views = new ArrayList<>();
        for (SchedulingNeed need : orderService.getPendingSchedulingNeeds(requester)) {
            ClinicalOrder order = orderService.getOrder(requester, need.getOrderId());
            Patient patient = patientService.getPatient(requester, order.getPatientId());
            TestType testType = systemParameterService.getTestType(requester, order.getTestTypeId());
            String priorityId = order instanceof DiagnosticOrder diagnostic ? diagnostic.getPriorityId() : "";
            views.add(new PendingNeedView(need.getNeedId(), order.getOrderId(), order.getPatientId(),
                    patient.getFullName(), order.getTestTypeId(), testType.getName(), priorityId,
                    order.getCreationDate(), need.getDueDate()));
        }
        return views;
    }

    @Override
    public List<TimeSlot> getFreeSlots(String requester, String testTypeId, LocalDate fromDate, LocalDate toDate)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, APPOINTMENTS);
        TestType testType = readActiveTestType(requester, testTypeId);
        List<String> errors = new ArrayList<>();
        if (fromDate == null || toDate == null) {
            errors.add("The date range is required");
        } else if (toDate.isBefore(fromDate)) {
            errors.add("The last date cannot be before the first one");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        List<TimeSlot> free = new ArrayList<>();
        for (Schedule schedule : schedulesOf(testType.getRoom(), requester)) {
            LocalDate day = fromDate;
            while (!day.isAfter(toDate)) {
                addFreeSlotsOfTheDay(free, schedule, day, testType);
                day = day.plusDays(1);
            }
        }
        return free;
    }

    private void addFreeSlotsOfTheDay(List<TimeSlot> free, Schedule schedule, LocalDate day, TestType testType) {
        if (!schedule.getDaysOfWeek().contains(day.getDayOfWeek())) {
            return;
        }
        LocalDateTime start = LocalDateTime.of(day, schedule.getStartTime());
        LocalDateTime endOfDay = LocalDateTime.of(day, schedule.getEndTime());
        while (!start.plusMinutes(testType.getDurationMinutes()).isAfter(endOfDay)) {
            TimeSlot candidate = new TimeSlot(testType.getRoom(), start,
                    start.plusMinutes(testType.getDurationMinutes()));
            if (!start.isBefore(LocalDateTime.now(clock)) && isFree(candidate, null)) {
                free.add(candidate);
            }
            start = start.plusMinutes(testType.getDurationMinutes());
        }
    }

    @Override
    public Appointment bookAppointment(String requester, String needId, TimeSlot slot)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, APPOINTMENTS);
        List<String> errors = new ArrayList<>();
        if (blank(needId)) {
            errors.add("The scheduling need is required");
        }
        if (slot == null) {
            errors.add("The slot is required");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        SchedulingNeed need = findPendingNeed(requester, needId);
        ClinicalOrder order = orderService.getOrder(requester, need.getOrderId());
        TestType testType = readActiveTestType(requester, order.getTestTypeId());
        Patient patient = patientService.getPatient(requester, order.getPatientId());
        if (!patient.isActive()) {
            errors.add("The patient " + order.getPatientId() + " is not active");
        }
        errors.addAll(slotErrors(slot, testType, null, requester));
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        orderService.markSchedulingNeedBooked(requester, needId);
        Appointment created = new Appointment(nextAppointmentId(), needId, order.getPatientId(),
                order.getTestTypeId(), testType.getRoom(), order.getRequester(), slot.getStart(), slot.getEnd(),
                AppointmentStatus.BOOKED);
        appointments.save(created);
        auditRecorder.register(requester, BOOKED, APPOINTMENT + created.getAppointmentId());
        return created;
    }

    @Override
    public Appointment rescheduleAppointment(String requester, String appointmentId, TimeSlot newSlot)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, APPOINTMENTS);
        if (blank(appointmentId)) {
            throw new ValidationException(List.of("The appointment is required"));
        }
        if (newSlot == null) {
            throw new ValidationException(List.of("The slot is required"));
        }
        Appointment current = findAppointment(appointmentId);
        if (current.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ValidationException(List.of("The appointment " + appointmentId + " is cancelled"));
        }
        TestType testType = readActiveTestType(requester, current.getTestTypeId());
        List<String> errors = slotErrors(newSlot, testType, current.getAppointmentId(), requester);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        Appointment moved = new Appointment(current.getAppointmentId(), current.getNeedId(), current.getPatientId(),
                current.getTestTypeId(), testType.getRoom(), current.getPhysician(), newSlot.getStart(),
                newSlot.getEnd(), AppointmentStatus.RESCHEDULED);
        appointments.save(moved);
        auditRecorder.register(requester, RESCHEDULED, APPOINTMENT + moved.getAppointmentId());
        return moved;
    }

    @Override
    public Appointment cancelAppointment(String requester, String appointmentId)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, APPOINTMENTS);
        if (blank(appointmentId)) {
            throw new ValidationException(List.of("The appointment is required"));
        }
        Appointment current = findAppointment(appointmentId);
        if (current.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ValidationException(List.of("The appointment " + appointmentId + " is already cancelled"));
        }
        Appointment cancelled = new Appointment(current.getAppointmentId(), current.getNeedId(),
                current.getPatientId(), current.getTestTypeId(), current.getRoom(), current.getPhysician(),
                current.getStart(), current.getEnd(), AppointmentStatus.CANCELLED);
        try {
            orderService.markSchedulingNeedPending(requester, current.getNeedId());
        } catch (ValidationException alreadyPending) {
            // The need was already pending: nothing to do.
        }
        appointments.save(cancelled);
        auditRecorder.register(requester, CANCELLED, APPOINTMENT + cancelled.getAppointmentId());
        return cancelled;
    }

    @Override
    public List<Appointment> getAppointments(String requester) throws AccessDeniedException {
        checkPermission(requester, APPOINTMENTS);
        auditRecorder.register(requester, ACCESSED, APPOINTMENTS);
        return appointments.findAll();
    }

    @Override
    public List<Appointment> getAppointmentsOfPatient(String requester, String patientId)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, APPOINTMENTS);
        if (blank(patientId)) {
            throw new ValidationException(List.of("The patient is required"));
        }
        auditRecorder.register(requester, ACCESSED, "PATIENT:" + patientId);
        return appointments.findByPatient(patientId);
    }

    /** The requester must have MANAGE_APPOINTMENTS; if not, the attempt is registered and denied. */
    private void checkPermission(String requester, String resource) throws AccessDeniedException {
        if (!roleManagementService.hasPermission(requester, MANAGE_APPOINTMENTS)) {
            auditRecorder.register(requester, DENIED, resource);
            throw new AccessDeniedException(requester);
        }
    }

    private TestType readActiveTestType(String requester, String testTypeId) throws ValidationException,
            AccessDeniedException {
        if (blank(testTypeId)) {
            throw new ValidationException(List.of("The test type is required"));
        }
        TestType testType = systemParameterService.getTestType(requester, testTypeId);
        if (!testType.isActive()) {
            throw new ValidationException(List.of("The test type " + testTypeId + " is not active"));
        }
        return testType;
    }

    private SchedulingNeed findPendingNeed(String requester, String needId)
            throws AccessDeniedException, ValidationException {
        for (SchedulingNeed need : orderService.getPendingSchedulingNeeds(requester)) {
            if (needId.equals(need.getNeedId())) {
                return need;
            }
        }
        throw new ValidationException(List.of("The scheduling need " + needId + " is not pending"));
    }

    private Appointment findAppointment(String appointmentId) throws ValidationException {
        Appointment found = appointments.findById(appointmentId);
        if (found == null) {
            throw new ValidationException(List.of("There is no appointment with the id " + appointmentId));
        }
        return found;
    }

    /** Every reason why the slot cannot be used for that test type, ignoring the appointment that is being moved. */
    private List<String> slotErrors(TimeSlot slot, TestType testType, String ignoredAppointmentId,
            String requester) throws AccessDeniedException {
        List<String> errors = new ArrayList<>();
        if (slot.getStart() == null || slot.getEnd() == null) {
            errors.add("The start and the end of the slot are required");
            return errors;
        } else if (!slot.getEnd().isAfter(slot.getStart())) {
            errors.add("The end of the slot must be after its start");
        } else if (!slot.getStart().toLocalDate().equals(slot.getEnd().toLocalDate())) {
            errors.add("The slot must be inside a single day");
        } else if (slot.getEnd().minusMinutes(testType.getDurationMinutes()).isAfter(slot.getStart())
                || slot.getStart().plusMinutes(testType.getDurationMinutes()).isBefore(slot.getEnd())) {
            errors.add("The duration of the slot must match the duration of the test type "
                    + testType.getId());
        } else if (!startOnGrid(slot.getStart(), testType, requester)) {
            errors.add("The start of the slot must be on the grid of the schedule of the room "
                    + testType.getRoom());
        }
        if (!testType.getRoom().equals(slot.getRoom())) {
            errors.add("The room " + slot.getRoom() + " is not the room of the test type " + testType.getId());
        }
        if (slot.getStart().isBefore(LocalDateTime.now(clock))) {
            errors.add("The slot " + MOMENT.format(slot.getStart()) + " is in the past");
        }
        if (!insideSchedule(slot, testType.getRoom(), requester)) {
            errors.add("The slot " + MOMENT.format(slot.getStart()) + " is not inside the schedule of the room "
                    + testType.getRoom());
        }
        if (!isFree(slot, ignoredAppointmentId)) {
            errors.add("The slot " + MOMENT.format(slot.getStart()) + " is not free");
        }
        return errors;
    }

    /**
     * True if the start of the slot is a multiple of the duration of the test type from the start of the schedule of
     * the room, for every active schedule of that room.
     */
    /**
     * True if the start of the slot is a multiple of the duration of the test type from the start of the schedule of
     * the room, for every active schedule of that room. The requester is used to read the schedules of C2.
     *
     * @param start the start of the slot
     * @param testType the test type whose room and duration define the grid
     * @param requester the user asking for the schedules
     * @return {@code true} if the start is on the grid of some active schedule of the room
     */
    private boolean startOnGrid(LocalDateTime start, TestType testType, String requester) throws AccessDeniedException {
        for (Schedule schedule : schedulesOf(testType.getRoom(), requester)) {
            if (schedule.getDaysOfWeek().contains(start.getDayOfWeek())) {
                LocalDateTime gridStart = LocalDateTime.of(start.toLocalDate(), schedule.getStartTime());
                long minutesFromGrid = start.toLocalTime().toSecondOfDay() / 60
                        - gridStart.toLocalTime().toSecondOfDay() / 60;
                if (minutesFromGrid >= 0
                        && minutesFromGrid % testType.getDurationMinutes() == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean insideSchedule(TimeSlot slot, String room, String requester) throws AccessDeniedException {
        for (Schedule schedule : schedulesOf(room, requester)) {
            if (schedule.getDaysOfWeek().contains(slot.getStart().getDayOfWeek())
                    && !slot.getStart().toLocalTime().isBefore(schedule.getStartTime())
                    && !slot.getEnd().toLocalTime().isAfter(schedule.getEndTime())) {
                return true;
            }
        }
        return false;
    }

    private List<Schedule> schedulesOf(String room, String requester) throws AccessDeniedException {
        List<Schedule> schedules = systemParameterService.getSchedules(requester);
        return schedules.stream()
                .filter(schedule -> room.equals(schedule.getResource()) && schedule.isActive())
                .toList();
    }

    private boolean isFree(TimeSlot slot, String ignoredAppointmentId) {
        return appointments.findByDateAndRoom(slot.getStart().toLocalDate(), slot.getRoom()).stream()
                .filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELLED)
                .filter(appointment -> !appointment.getAppointmentId().equals(ignoredAppointmentId))
                .noneMatch(appointment -> overlaps(appointment, slot));
    }

    private static boolean overlaps(Appointment appointment, TimeSlot slot) {
        return appointment.getStart().isBefore(slot.getEnd()) && slot.getStart().isBefore(appointment.getEnd());
    }

    private String nextAppointmentId() {
        String id;
        do {
            lastAppointmentNumber++;
            id = String.format("APT-%04d", lastAppointmentNumber);
        } while (appointments.findById(id) != null);
        return id;
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }
}
