package es.uclm.esi.iso2.server.appointments.service;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.IAuditRecorder;
import es.uclm.esi.iso2.common.access.contracts.IRoleManagementService;
import es.uclm.esi.iso2.common.appointments.contracts.IDailyScheduleService;
import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.AppointmentStatus;
import es.uclm.esi.iso2.common.appointments.domain.ScheduleEntry;
import es.uclm.esi.iso2.common.configuration.contracts.ISystemParameterService;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import es.uclm.esi.iso2.common.patients.contracts.IPatientService;
import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.appointments.persistence.IAppointmentRepository;
import es.uclm.esi.iso2.server.appointments.persistence.InMemoryAppointmentRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the daily schedule and the worklists of the admissions staff (UC14, Control_DailySchedule). Only the booked
 * appointments of the day are shown, in time order, with the patient, the test type and its preparation; a cancelled
 * appointment is never shown (D43). Every access needs {@code MANAGE_APPOINTMENTS}; a denied attempt is registered as
 * {@code SCHEDULE_ACCESS_DENIED}, the schedule as {@code SCHEDULE_ACCESSED} and the worklists as
 * {@code WORKLIST_ACCESSED}. It only reads: it changes no data.
 */
public class DailyScheduleService implements IDailyScheduleService {
    private static final String MANAGE_APPOINTMENTS = "MANAGE_APPOINTMENTS";
    private static final String DENIED = "SCHEDULE_ACCESS_DENIED";
    private static final String SCHEDULE_ACCESSED = "SCHEDULE_ACCESSED";
    private static final String WORKLIST_ACCESSED = "WORKLIST_ACCESSED";
    private static final String SCHEDULE = "schedule";

    private final IAppointmentRepository appointments;
    private final IPatientService patientService;
    private final ISystemParameterService systemParameterService;
    private final IRoleManagementService roleManagementService;
    private final IAuditRecorder auditRecorder;

    /**
     * Creates the service.
     *
     * @param appointments where the appointments are kept; it must be the same repository that UC13 uses
     * @param patientService the contract of C3, to read the name of the patient
     * @param systemParameterService the contract of C2, to read the name and the preparation of the test type
     * @param roleManagementService used to know the permissions of the requester (contract of C1)
     * @param auditRecorder where the accesses and the denied attempts are registered (contract of C1)
     */
    public DailyScheduleService(IAppointmentRepository appointments, IPatientService patientService,
            ISystemParameterService systemParameterService, IRoleManagementService roleManagementService,
            IAuditRecorder auditRecorder) {
        this.appointments = appointments;
        this.patientService = patientService;
        this.systemParameterService = systemParameterService;
        this.roleManagementService = roleManagementService;
        this.auditRecorder = auditRecorder;
    }

    /**
     * Creates the service with an empty in-memory repository (D-030). It is used when the schedule has its own
     * repository, not the one of UC13.
     *
     * @param patientService the contract of C3
     * @param systemParameterService the contract of C2
     * @param roleManagementService the contract of C1 for the permissions
     * @param auditRecorder the contract of C1 for the audit log
     */
    public DailyScheduleService(IPatientService patientService, ISystemParameterService systemParameterService,
            IRoleManagementService roleManagementService, IAuditRecorder auditRecorder) {
        this(new InMemoryAppointmentRepository(), patientService, systemParameterService, roleManagementService,
                auditRecorder);
    }

    @Override
    public Map<String, List<ScheduleEntry>> getDailySchedule(String requester, LocalDate date)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, SCHEDULE);
        if (date == null) {
            throw new ValidationException(List.of("The day is required"));
        }
        Map<String, List<ScheduleEntry>> schedule = new LinkedHashMap<>();
        for (Appointment appointment : bookedOf(appointments.findByDate(date))) {
            schedule.computeIfAbsent(appointment.getRoom(), room -> new ArrayList<>())
                    .add(entryOf(requester, appointment));
        }
        auditRecorder.register(requester, SCHEDULE_ACCESSED, SCHEDULE + ":" + date);
        return schedule;
    }

    @Override
    public List<ScheduleEntry> getRoomWorklist(String requester, LocalDate date, String room)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, SCHEDULE);
        List<String> errors = new ArrayList<>();
        if (date == null) {
            errors.add("The day is required");
        }
        if (blank(room)) {
            errors.add("The room is required");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        List<ScheduleEntry> worklist = entriesOf(requester, bookedOf(appointments.findByDateAndRoom(date, room)));
        auditRecorder.register(requester, WORKLIST_ACCESSED, "room:" + room + "@" + date);
        return worklist;
    }

    @Override
    public List<ScheduleEntry> getPhysicianWorklist(String requester, LocalDate date, String physician)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, SCHEDULE);
        List<String> errors = new ArrayList<>();
        if (date == null) {
            errors.add("The day is required");
        }
        if (blank(physician)) {
            errors.add("The physician is required");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        List<ScheduleEntry> worklist = entriesOf(requester,
                bookedOf(appointments.findByDateAndPhysician(date, physician)));
        auditRecorder.register(requester, WORKLIST_ACCESSED, "physician:" + physician + "@" + date);
        return worklist;
    }

    /** The requester must have MANAGE_APPOINTMENTS; if not, the attempt is registered and denied. */
    private void checkPermission(String requester, String resource) throws AccessDeniedException {
        if (!roleManagementService.hasPermission(requester, MANAGE_APPOINTMENTS)) {
            auditRecorder.register(requester, DENIED, resource);
            throw new AccessDeniedException(requester);
        }
    }

    /** The appointments that are shown: the booked and the moved ones, in time order, never the cancelled ones. */
    private static List<Appointment> bookedOf(List<Appointment> read) {
        return read.stream()
                .filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELLED)
                .sorted(Comparator.comparing(Appointment::getStart))
                .toList();
    }

    private List<ScheduleEntry> entriesOf(String requester, List<Appointment> read)
            throws AccessDeniedException, ValidationException {
        List<ScheduleEntry> entries = new ArrayList<>();
        for (Appointment appointment : read) {
            entries.add(entryOf(requester, appointment));
        }
        return entries;
    }

    /** Reads the name of the patient and the name and the preparation of the test type of an appointment. */
    private ScheduleEntry entryOf(String requester, Appointment appointment)
            throws AccessDeniedException, ValidationException {
        Patient patient = patientService.getPatient(requester, appointment.getPatientId());
        TestType testType = systemParameterService.getTestType(requester, appointment.getTestTypeId());
        return new ScheduleEntry(appointment, patient.getFullName(), testType.getName(),
                testType.getPreparation());
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }
}
