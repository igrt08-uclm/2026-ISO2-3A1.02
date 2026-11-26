package es.uclm.esi.iso2.server.appointments.service;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.IAuditRecorder;
import es.uclm.esi.iso2.common.access.contracts.IRoleManagementService;
import es.uclm.esi.iso2.common.appointments.contracts.IAppointmentService;
import es.uclm.esi.iso2.common.appointments.contracts.IWaitlistService;
import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.TimeSlot;
import es.uclm.esi.iso2.common.appointments.domain.WaitlistEntry;
import es.uclm.esi.iso2.common.appointments.domain.WaitlistStatus;
import es.uclm.esi.iso2.common.configuration.contracts.ISystemParameterService;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.orders.contracts.IClinicalOrderService;
import es.uclm.esi.iso2.common.orders.domain.ClinicalOrder;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeed;
import es.uclm.esi.iso2.common.patients.contracts.IPatientService;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.appointments.persistence.IWaitlistRepository;
import es.uclm.esi.iso2.server.appointments.persistence.InMemoryWaitlistRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keeps the waitlist of the patients who wait for an appointment of a test type and fills the openings of the
 * calendar (UC15, Control_ManageWaitlist). Every operation needs {@code MANAGE_APPOINTMENTS}; a denied attempt is
 * registered as {@code WAITLIST_ACCESS_DENIED} and the changes as {@code WAITLIST_CHANGED},
 * {@code WAITLIST_ENTRY_REMOVED} and {@code OPENING_FILLED}. The waitlist is ordered by the index of the priority tier
 * in the configured priorities and then by the moment the patient was added. An entry is never deleted: its status
 * changes (D-022), and filling an opening books the appointment through UC13 (the include of D42).
 */
public class WaitlistService implements IWaitlistService {
    private static final String MANAGE_APPOINTMENTS = "MANAGE_APPOINTMENTS";
    private static final String DENIED = "WAITLIST_ACCESS_DENIED";
    private static final String ACCESSED = "WAITLIST_ACCESSED";
    private static final String CHANGED = "WAITLIST_CHANGED";
    private static final String REMOVED = "WAITLIST_ENTRY_REMOVED";
    private static final String OPENING_FILLED = "OPENING_FILLED";
    private static final String WAITLIST = "waitlist";
    private static final String ENTRY = "WAITLIST_ENTRY:";

    private final IWaitlistRepository waitlist;
    private final IAppointmentService appointmentService;
    private final IClinicalOrderService orderService;
    private final IPatientService patientService;
    private final ISystemParameterService systemParameterService;
    private final IRoleManagementService roleManagementService;
    private final IAuditRecorder auditRecorder;
    private final Clock clock;
    private final AtomicInteger lastEntryNumber = new AtomicInteger();

    /**
     * Creates the service with the given repository and clock.
     *
     * @param waitlist where the entries are kept
     * @param appointmentService the contract of UC13, to read the openings and to book the appointment
     * @param orderService the contract of C4, to read the pending needs and their orders
     * @param patientService the contract of C3, to know whether a patient is active
     * @param systemParameterService the contract of C2, to read the priority tiers
     * @param roleManagementService used to know the permissions of the requester (contract of C1)
     * @param auditRecorder where the accesses, the changes and the denied attempts are registered (contract of C1)
     * @param clock gives the current moment, which is the moment in which an entry is added
     */
    public WaitlistService(IWaitlistRepository waitlist, IAppointmentService appointmentService,
            IClinicalOrderService orderService, IPatientService patientService,
            ISystemParameterService systemParameterService, IRoleManagementService roleManagementService,
            IAuditRecorder auditRecorder, Clock clock) {
        this.waitlist = waitlist;
        this.appointmentService = appointmentService;
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
     * @param appointmentService the contract of UC13
     * @param orderService the contract of C4
     * @param patientService the contract of C3
     * @param systemParameterService the contract of C2
     * @param roleManagementService the contract of C1 for the permissions
     * @param auditRecorder the contract of C1 for the audit log
     */
    public WaitlistService(IAppointmentService appointmentService, IClinicalOrderService orderService,
            IPatientService patientService, ISystemParameterService systemParameterService,
            IRoleManagementService roleManagementService, IAuditRecorder auditRecorder) {
        this(new InMemoryWaitlistRepository(), appointmentService, orderService, patientService,
                systemParameterService, roleManagementService, auditRecorder, Clock.systemDefaultZone());
    }

    /**
     * Loads two invented entries, only if the system starts with no entry at all. It is called when the program
     * starts, not by a user, so it does not need a permission and it is not registered in the audit log.
     */
    public void loadSeedData() {
        if (waitlist.findAll().isEmpty()) {
            AppointmentSeedData.waitlistEntries(LocalDate.now(clock)).forEach(waitlist::save);
        }
    }

    @Override
    public List<WaitlistEntry> getWaitlist(String requester, String testTypeId)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, WAITLIST);
        if (blank(testTypeId)) {
            throw new ValidationException(List.of("The test type is required"));
        }
        auditRecorder.register(requester, ACCESSED, WAITLIST + ":" + testTypeId);
        return waitingOf(requester, testTypeId);
    }

    @Override
    public WaitlistEntry addToWaitlist(String requester, String needId, String priorityId)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, WAITLIST);
        List<String> errors = new ArrayList<>();
        if (blank(needId)) {
            errors.add("The scheduling need is required");
        }
        if (blank(priorityId)) {
            errors.add("The priority tier is required");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        Priority priority = null;
        try {
            priority = readPriority(requester, priorityId);
        } catch (ValidationException invalidPriority) {
            errors.addAll(invalidPriority.getErrors());
        }
        ClinicalOrder order = null;
        try {
            order = readPendingOrder(requester, needId);
        } catch (ValidationException invalidNeed) {
            errors.addAll(invalidNeed.getErrors());
        }
        if (waitlist.findWaitingByNeed(needId) != null) {
            errors.add("The scheduling need " + needId + " is already in the waitlist");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        WaitlistEntry entry = new WaitlistEntry(nextEntryId(), needId, order.getPatientId(),
                order.getTestTypeId(), priority.getId(), LocalDateTime.now(clock), WaitlistStatus.WAITING);
        waitlist.save(entry);
        auditRecorder.register(requester, CHANGED, ENTRY + entry.getEntryId());
        return entry;
    }

    @Override
    public WaitlistEntry changeWaitlistPriority(String requester, String entryId, String priorityId)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, WAITLIST);
        List<String> errors = new ArrayList<>();
        if (blank(entryId)) {
            errors.add("The entry is required");
        }
        if (blank(priorityId)) {
            errors.add("The priority tier is required");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        WaitlistEntry current = findWaiting(entryId);
        Priority priority = readPriority(requester, priorityId);
        WaitlistEntry changed = new WaitlistEntry(current.getEntryId(), current.getNeedId(),
                current.getPatientId(), current.getTestTypeId(), priority.getId(), current.getAddedAt(),
                WaitlistStatus.WAITING);
        waitlist.save(changed);
        auditRecorder.register(requester, CHANGED, ENTRY + changed.getEntryId());
        return changed;
    }

    @Override
    public WaitlistEntry removeFromWaitlist(String requester, String entryId)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, WAITLIST);
        if (blank(entryId)) {
            throw new ValidationException(List.of("The entry is required"));
        }
        WaitlistEntry current = findWaiting(entryId);
        WaitlistEntry removed = new WaitlistEntry(current.getEntryId(), current.getNeedId(),
                current.getPatientId(), current.getTestTypeId(), current.getPriorityId(), current.getAddedAt(),
                WaitlistStatus.REMOVED);
        waitlist.save(removed);
        auditRecorder.register(requester, REMOVED, ENTRY + removed.getEntryId());
        return removed;
    }

    @Override
    public List<TimeSlot> getCalendarOpenings(String requester, String testTypeId, LocalDate fromDate,
            LocalDate toDate) throws AccessDeniedException, ValidationException {
        checkPermission(requester, WAITLIST);
        if (blank(testTypeId)) {
            throw new ValidationException(List.of("The test type is required"));
        }
        List<TimeSlot> openings = appointmentService.getFreeSlots(requester, testTypeId, fromDate, toDate);
        auditRecorder.register(requester, ACCESSED, "openings:" + testTypeId);
        return openings;
    }

    @Override
    public WaitlistEntry proposePatientForOpening(String requester, String testTypeId, TimeSlot opening)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, WAITLIST);
        List<String> errors = new ArrayList<>();
        if (blank(testTypeId)) {
            errors.add("The test type is required");
        }
        if (opening == null) {
            errors.add("The opening is required");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        auditRecorder.register(requester, ACCESSED, WAITLIST + ":" + testTypeId);
        List<String> pendingNeeds = orderService.getPendingSchedulingNeeds(requester).stream()
                .map(SchedulingNeed::getNeedId)
                .toList();
        for (WaitlistEntry entry : waitingOf(requester, testTypeId)) {
            if (pendingNeeds.contains(entry.getNeedId())
                    && patientService.getPatient(requester, entry.getPatientId()).isActive()) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public Appointment fillOpening(String requester, String entryId, TimeSlot opening)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, WAITLIST);
        List<String> errors = new ArrayList<>();
        if (blank(entryId)) {
            errors.add("The entry is required");
        }
        if (opening == null) {
            errors.add("The opening is required");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        WaitlistEntry current = findWaiting(entryId);
        Appointment booked = appointmentService.bookAppointment(requester, current.getNeedId(), opening);
        WaitlistEntry filled = new WaitlistEntry(current.getEntryId(), current.getNeedId(),
                current.getPatientId(), current.getTestTypeId(), current.getPriorityId(), current.getAddedAt(),
                WaitlistStatus.BOOKED);
        waitlist.save(filled);
        auditRecorder.register(requester, OPENING_FILLED, ENTRY + filled.getEntryId());
        return booked;
    }

    /** The requester must have MANAGE_APPOINTMENTS; if not, the attempt is registered and denied. */
    private void checkPermission(String requester, String resource) throws AccessDeniedException {
        if (!roleManagementService.hasPermission(requester, MANAGE_APPOINTMENTS)) {
            auditRecorder.register(requester, DENIED, resource);
            throw new AccessDeniedException(requester);
        }
    }

    /**
     * The entries of a test type that still wait, ordered by the index of their tier in the configured priorities and
     * then by the moment they were added.
     */
    private List<WaitlistEntry> waitingOf(String requester, String testTypeId) throws AccessDeniedException {
        List<Priority> priorities = systemParameterService.getPriorities(requester);
        return waitlist.findWaitingByTestType(testTypeId).stream()
                .sorted(Comparator.comparingInt((WaitlistEntry entry) ->
                        indexOfPriority(priorities, entry.getPriorityId()))
                        .thenComparing(WaitlistEntry::getAddedAt))
                .toList();
    }

    private static int indexOfPriority(List<Priority> priorities, String priorityId) {
        for (int index = 0; index < priorities.size(); index++) {
            if (priorities.get(index).getId().equals(priorityId)) {
                return index;
            }
        }
        return Integer.MAX_VALUE;
    }

    /** The priority tier must be one of the configured priorities and must be active (alternative flow 4b). */
    private Priority readPriority(String requester, String priorityId)
            throws AccessDeniedException, ValidationException {
        for (Priority priority : systemParameterService.getPriorities(requester)) {
            if (priority.getId().equals(priorityId)) {
                if (!priority.isActive()) {
                    throw new ValidationException(List.of("The priority tier " + priorityId
                            + " is not active"));
                }
                return priority;
            }
        }
        throw new ValidationException(List.of("The priority tier " + priorityId
                + " is not a configured priority"));
    }

    /** The need must be one of the pending scheduling needs of C4, and its order gives the patient and the test. */
    private ClinicalOrder readPendingOrder(String requester, String needId)
            throws AccessDeniedException, ValidationException {
        for (SchedulingNeed need : orderService.getPendingSchedulingNeeds(requester)) {
            if (needId.equals(need.getNeedId())) {
                return orderService.getOrder(requester, need.getOrderId());
            }
        }
        throw new ValidationException(List.of("The scheduling need " + needId + " is not pending"));
    }

    private WaitlistEntry findWaiting(String entryId) throws ValidationException {
        WaitlistEntry found = waitlist.findById(entryId);
        if (found == null) {
            throw new ValidationException(List.of("There is no entry with the id " + entryId
                    + " in the waitlist"));
        }
        if (found.getStatus() != WaitlistStatus.WAITING) {
            throw new ValidationException(List.of("The entry " + entryId + " is not waiting"));
        }
        return found;
    }

    private String nextEntryId() {
        String id;
        do {
            id = String.format("WL-%04d", lastEntryNumber.incrementAndGet());
        } while (waitlist.findById(id) != null);
        return id;
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }
}
