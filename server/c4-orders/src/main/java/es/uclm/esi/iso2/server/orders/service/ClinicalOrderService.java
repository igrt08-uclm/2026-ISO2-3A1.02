package es.uclm.esi.iso2.server.orders.service;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.IAuditRecorder;
import es.uclm.esi.iso2.common.access.contracts.IRoleManagementService;
import es.uclm.esi.iso2.common.configuration.contracts.ISystemParameterService;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import es.uclm.esi.iso2.common.orders.contracts.IClinicalOrderService;
import es.uclm.esi.iso2.common.orders.domain.ClinicalOrder;
import es.uclm.esi.iso2.common.orders.domain.DiagnosticOrder;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeed;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeedStatus;
import es.uclm.esi.iso2.common.patients.contracts.IPatientService;
import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.orders.persistence.IClinicalOrderRepository;
import es.uclm.esi.iso2.server.orders.persistence.ISchedulingNeedRepository;
import es.uclm.esi.iso2.server.orders.persistence.InMemoryClinicalOrderRepository;
import es.uclm.esi.iso2.server.orders.persistence.InMemorySchedulingNeedRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Orders diagnostic procedures and keeps their scheduling needs for C5 (UC06, Control_OrderProcedure). Ordering
 * needs an active user with the permission {@code ORDER_PROCEDURES} (D29). The order is only created for an active
 * patient (C3) and an active test type (C2), with one of the active priorities configured in C2, and with its
 * clinical indication and justification; every error is returned together. Each created order gets one pending
 * scheduling need, due on the day the order is created. A denied attempt is registered as
 * {@code ORDER_ACCESS_DENIED} (D30) and a created order as {@code ORDER_CREATED}. The server gives the ids
 * ({@code ORD-0001}, {@code NEED-0001}...). Orders are never edited, cancelled or deleted (D-039, D-022).
 */
public class ClinicalOrderService implements IClinicalOrderService {
    private static final String ORDER_PROCEDURES = "ORDER_PROCEDURES";
    private static final String CREATED = "ORDER_CREATED";
    private static final String ACCESS_DENIED = "ORDER_ACCESS_DENIED";
    private static final String ORDERS = "orders";
    private static final String ORDER = "ORDER:";

    private final IClinicalOrderRepository orders;
    private final ISchedulingNeedRepository needs;
    private final IRoleManagementService roleManagementService;
    private final IAuditRecorder auditRecorder;
    private final ISystemParameterService systemParameterService;
    private final IPatientService patientService;
    private final Clock clock;
    private int lastOrderNumber;
    private int lastNeedNumber;

    /**
     * Creates the service with the given repositories and clock.
     *
     * @param orders where the orders are kept
     * @param needs where the scheduling needs are kept
     * @param roleManagementService used to know the permissions of the requester (contract of C1)
     * @param auditRecorder where the created orders and the denied attempts are registered (contract of C1)
     * @param systemParameterService used to read the test types and the priorities (contract of C2)
     * @param patientService used to read the patient (contract of C3)
     * @param clock gives today's date, the creation date of the orders
     */
    public ClinicalOrderService(IClinicalOrderRepository orders, ISchedulingNeedRepository needs,
            IRoleManagementService roleManagementService, IAuditRecorder auditRecorder,
            ISystemParameterService systemParameterService, IPatientService patientService, Clock clock) {
        this.orders = orders;
        this.needs = needs;
        this.roleManagementService = roleManagementService;
        this.auditRecorder = auditRecorder;
        this.systemParameterService = systemParameterService;
        this.patientService = patientService;
        this.clock = clock;
    }

    /**
     * Creates the service with empty in-memory repositories (D-030) and the clock of the system.
     *
     * @param roleManagementService used to know the permissions of the requester (contract of C1)
     * @param auditRecorder where the created orders and the denied attempts are registered (contract of C1)
     * @param systemParameterService used to read the test types and the priorities (contract of C2)
     * @param patientService used to read the patient (contract of C3)
     */
    public ClinicalOrderService(IRoleManagementService roleManagementService, IAuditRecorder auditRecorder,
            ISystemParameterService systemParameterService, IPatientService patientService) {
        this(new InMemoryClinicalOrderRepository(), new InMemorySchedulingNeedRepository(), roleManagementService,
                auditRecorder, systemParameterService, patientService, Clock.systemDefaultZone());
    }

    @Override
    public DiagnosticOrder orderDiagnosticProcedure(String requester, DiagnosticOrder order)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, ORDERS);
        List<String> errors = validate(requester, order);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        DiagnosticOrder created = new DiagnosticOrder(nextOrderId(), order.getPatientId(), order.getTestTypeId(),
                requester, order.getIndication(), order.getJustification(), LocalDate.now(clock),
                order.getPriorityId());
        orders.save(created);
        createSchedulingNeeds(created);
        auditRecorder.register(requester, CREATED, ORDER + created.getOrderId());
        return created;
    }

    /** The requester must be an active user with ORDER_PROCEDURES; if not, it is registered and denied. */
    private void checkPermission(String requester, String resource) throws AccessDeniedException {
        if (!roleManagementService.hasPermission(requester, ORDER_PROCEDURES)) {
            deny(requester, resource);
        }
    }

    /** Registers the denied attempt on the resource and denies the access. */
    private void deny(String requester, String resource) throws AccessDeniedException {
        auditRecorder.register(requester, ACCESS_DENIED, resource);
        throw new AccessDeniedException(requester);
    }

    /**
     * Every reason why the order cannot be created, in this order: order, patient, test type, priority, clinical
     * indication and justification. The requester is needed to read the patient (C3) and the parameters (C2).
     */
    private List<String> validate(String requester, DiagnosticOrder order) throws AccessDeniedException {
        List<String> errors = new ArrayList<>();
        if (order == null) {
            errors.add("The order is required");
            return errors;
        }
        checkPatient(requester, order.getPatientId(), errors);
        checkTestType(requester, order.getTestTypeId(), errors);
        checkPriority(requester, order.getPriorityId(), errors);
        if (blank(order.getIndication())) {
            errors.add("The clinical indication is required");
        }
        if (blank(order.getJustification())) {
            errors.add("The justification is required");
        }
        return errors;
    }

    /** The patient must be given, exist in C3 and be active. */
    private void checkPatient(String requester, String patientId, List<String> errors)
            throws AccessDeniedException {
        if (blank(patientId)) {
            errors.add("The patient is required");
            return;
        }
        try {
            Patient patient = patientService.getPatient(requester, patientId);
            if (!patient.isActive()) {
                errors.add("The patient " + patientId + " is not active");
            }
        } catch (ValidationException unknown) {
            errors.addAll(unknown.getErrors());
        }
    }

    /** The test type must be given, exist in the catalogue of C2 and be active. */
    private void checkTestType(String requester, String testTypeId, List<String> errors)
            throws AccessDeniedException {
        if (blank(testTypeId)) {
            errors.add("The test type is required");
            return;
        }
        try {
            TestType testType = systemParameterService.getTestType(requester, testTypeId);
            if (!testType.isActive()) {
                errors.add("The test type " + testTypeId + " is not active");
            }
        } catch (ValidationException unknown) {
            errors.addAll(unknown.getErrors());
        }
    }

    /** The priority must be given, be one of the priorities configured in C2 and be active. */
    private void checkPriority(String requester, String priorityId, List<String> errors)
            throws AccessDeniedException {
        if (blank(priorityId)) {
            errors.add("The priority is required");
            return;
        }
        Priority priority = systemParameterService.getPriorities(requester).stream()
                .filter(configured -> priorityId.equals(configured.getId()))
                .findFirst()
                .orElse(null);
        if (priority == null) {
            errors.add("The priority " + priorityId + " is not one of the configured priorities");
        } else if (!priority.isActive()) {
            errors.add("The priority " + priorityId + " is not active");
        }
    }

    /** A diagnostic order generates one pending scheduling need, due on the day the order is created (D33). */
    private List<SchedulingNeed> createSchedulingNeeds(ClinicalOrder order) {
        SchedulingNeed need = new SchedulingNeed(nextNeedId(), order.getOrderId(), order.getCreationDate(),
                SchedulingNeedStatus.PENDING);
        needs.save(need);
        return List.of(need);
    }

    /** The next free id {@code ORD-0001}, {@code ORD-0002}...; it skips the ids that the repository already has. */
    private String nextOrderId() {
        String id;
        do {
            lastOrderNumber++;
            id = String.format("ORD-%04d", lastOrderNumber);
        } while (orders.findById(id) != null);
        return id;
    }

    /** The next free id {@code NEED-0001}, {@code NEED-0002}...; it skips the ids that the repository already has. */
    private String nextNeedId() {
        String id;
        do {
            lastNeedNumber++;
            id = String.format("NEED-%04d", lastNeedNumber);
        } while (needs.findById(id) != null);
        return id;
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }
}
