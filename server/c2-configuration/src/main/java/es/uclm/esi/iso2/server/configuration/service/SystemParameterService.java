package es.uclm.esi.iso2.server.configuration.service;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.IAuditRecorder;
import es.uclm.esi.iso2.common.access.contracts.IRoleManagementService;
import es.uclm.esi.iso2.common.configuration.contracts.ISystemParameterService;
import es.uclm.esi.iso2.common.configuration.domain.AppointmentPolicy;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.Protocol;
import es.uclm.esi.iso2.common.configuration.domain.Schedule;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.configuration.persistence.IConfigurationRepository;
import es.uclm.esi.iso2.server.configuration.persistence.InMemoryConfigurationRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Manages the examination catalogue and the other parameters of the system (UC17). Every operation checks that the
 * requester is an active user with the permission {@code CONFIGURE_SYSTEM}, validates the data, deactivates instead
 * of deleting, and registers every change in the audit log.
 */
public class SystemParameterService implements ISystemParameterService {
    private static final String CONFIGURE_SYSTEM = "CONFIGURE_SYSTEM";
    private static final String CREATED = "PARAMETER_CREATED";
    private static final String UPDATED = "PARAMETER_UPDATED";
    private static final String DEACTIVATED = "PARAMETER_DEACTIVATED";
    private static final String ACCESS_DENIED = "PARAMETER_ACCESS_DENIED";
    private static final String TEST_TYPE = "TEST_TYPE";
    private static final String PROTOCOL = "PROTOCOL";
    private static final String SCHEDULE = "SCHEDULE";
    private static final String APPOINTMENT_POLICY = "APPOINTMENT_POLICY";
    private static final String PRIORITY = "PRIORITY";

    private final IConfigurationRepository<TestType> testTypes;
    private final IConfigurationRepository<Protocol> protocols;
    private final IConfigurationRepository<Schedule> schedules;
    private final IConfigurationRepository<AppointmentPolicy> policies;
    private final IConfigurationRepository<Priority> priorities;
    private final IRoleManagementService roleManagementService;
    private final IAuditRecorder auditRecorder;

    /**
     * Creates the service with the given repositories.
     *
     * @param testTypes where the test types are kept
     * @param protocols where the protocols are kept
     * @param schedules where the schedules are kept
     * @param policies where the appointment policies are kept
     * @param priorities where the priorities are kept
     * @param roleManagementService used to know the permissions of the requester (contract of C1)
     * @param auditRecorder where every change is registered (contract of C1)
     */
    public SystemParameterService(IConfigurationRepository<TestType> testTypes,
            IConfigurationRepository<Protocol> protocols, IConfigurationRepository<Schedule> schedules,
            IConfigurationRepository<AppointmentPolicy> policies, IConfigurationRepository<Priority> priorities,
            IRoleManagementService roleManagementService, IAuditRecorder auditRecorder) {
        this.testTypes = testTypes;
        this.protocols = protocols;
        this.schedules = schedules;
        this.policies = policies;
        this.priorities = priorities;
        this.roleManagementService = roleManagementService;
        this.auditRecorder = auditRecorder;
    }

    /**
     * Creates the service with empty in-memory repositories (D-030).
     *
     * @param roleManagementService used to know the permissions of the requester (contract of C1)
     * @param auditRecorder where every change is registered (contract of C1)
     */
    public SystemParameterService(IRoleManagementService roleManagementService, IAuditRecorder auditRecorder) {
        this(new InMemoryConfigurationRepository<>(TestType::getId),
                new InMemoryConfigurationRepository<>(Protocol::getId),
                new InMemoryConfigurationRepository<>(Schedule::getResource),
                new InMemoryConfigurationRepository<>(AppointmentPolicy::getId),
                new InMemoryConfigurationRepository<>(Priority::getId), roleManagementService, auditRecorder);
    }

    @Override
    public List<TestType> getTestTypes(String requester) throws AccessDeniedException {
        checkPermission(requester, TEST_TYPE);
        return testTypes.findAll();
    }

    @Override
    public TestType getTestType(String requester, String testTypeId) throws AccessDeniedException, ValidationException {
        checkPermission(requester, TEST_TYPE);
        return existing(testTypes, testTypeId, "test type");
    }

    @Override
    public TestType createTestType(String requester, TestType testType)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, TEST_TYPE);
        List<String> errors = validate(testType);
        if (testType != null && !blank(testType.getId()) && testTypes.findById(testType.getId()) != null) {
            errors.add("A test type with the id " + testType.getId() + " already exists");
        }
        return save(requester, TEST_TYPE, testType == null ? null : testType.getId(), testType, errors, testTypes);
    }

    @Override
    public TestType updateTestType(String requester, String testTypeId, TestType testType)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, TEST_TYPE);
        existing(testTypes, testTypeId, "test type");
        List<String> errors = validate(testType);
        if (testType != null && !testTypeId.equals(testType.getId())) {
            errors.add("The id of a test type cannot be changed");
        }
        return save(requester, TEST_TYPE, testTypeId, testType, errors, testTypes);
    }

    @Override
    public void deactivateTestType(String requester, String testTypeId)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, TEST_TYPE);
        deactivate(requester, TEST_TYPE, testTypeId, testTypes, "test type", TestType::deactivated,
                TestType::isActive);
    }

    @Override
    public List<Protocol> getProtocols(String requester) throws AccessDeniedException {
        checkPermission(requester, PROTOCOL);
        return protocols.findAll();
    }

    @Override
    public Protocol saveProtocol(String requester, Protocol protocol)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, PROTOCOL);
        List<String> errors = new ArrayList<>();
        if (protocol == null) {
            errors.add("The protocol is required");
        } else {
            requireText(errors, protocol.getId(), "The id");
            requireText(errors, protocol.getName(), "The name");
        }
        return save(requester, PROTOCOL, protocol == null ? null : protocol.getId(), protocol, errors, protocols);
    }

    @Override
    public void deactivateProtocol(String requester, String protocolId)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, PROTOCOL);
        deactivate(requester, PROTOCOL, protocolId, protocols, "protocol", Protocol::deactivated, Protocol::isActive);
    }

    @Override
    public List<Schedule> getSchedules(String requester) throws AccessDeniedException {
        checkPermission(requester, SCHEDULE);
        return schedules.findAll();
    }

    @Override
    public Schedule saveSchedule(String requester, Schedule schedule)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, SCHEDULE);
        List<String> errors = new ArrayList<>();
        if (schedule == null) {
            errors.add("The schedule is required");
        } else {
            requireText(errors, schedule.getResource(), "The resource");
            if (schedule.getDaysOfWeek() == null || schedule.getDaysOfWeek().isEmpty()) {
                errors.add("At least one day of the week is required");
            }
            if (schedule.getStartTime() == null || schedule.getEndTime() == null) {
                errors.add("The start and the end time are required");
            } else if (!schedule.getEndTime().isAfter(schedule.getStartTime())) {
                errors.add("The end time must be after the start time");
            }
        }
        return save(requester, SCHEDULE, schedule == null ? null : schedule.getResource(), schedule, errors,
                schedules);
    }

    @Override
    public void deactivateSchedule(String requester, String resource)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, SCHEDULE);
        deactivate(requester, SCHEDULE, resource, schedules, "schedule", Schedule::deactivated, Schedule::isActive);
    }

    @Override
    public List<AppointmentPolicy> getAppointmentPolicies(String requester) throws AccessDeniedException {
        checkPermission(requester, APPOINTMENT_POLICY);
        return policies.findAll();
    }

    @Override
    public AppointmentPolicy saveAppointmentPolicy(String requester, AppointmentPolicy policy)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, APPOINTMENT_POLICY);
        List<String> errors = new ArrayList<>();
        if (policy == null) {
            errors.add("The appointment policy is required");
        } else {
            requireText(errors, policy.getId(), "The id");
            requireText(errors, policy.getName(), "The name");
        }
        return save(requester, APPOINTMENT_POLICY, policy == null ? null : policy.getId(), policy, errors, policies);
    }

    @Override
    public void deactivateAppointmentPolicy(String requester, String policyId)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, APPOINTMENT_POLICY);
        deactivate(requester, APPOINTMENT_POLICY, policyId, policies, "appointment policy",
                AppointmentPolicy::deactivated, AppointmentPolicy::isActive);
    }

    @Override
    public List<Priority> getPriorities(String requester) throws AccessDeniedException {
        checkPermission(requester, PRIORITY);
        return priorities.findAll();
    }

    @Override
    public Priority savePriority(String requester, Priority priority)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, PRIORITY);
        List<String> errors = new ArrayList<>();
        if (priority == null) {
            errors.add("The priority is required");
        } else {
            requireText(errors, priority.getId(), "The id");
            requireText(errors, priority.getName(), "The name");
        }
        return save(requester, PRIORITY, priority == null ? null : priority.getId(), priority, errors, priorities);
    }

    @Override
    public void deactivatePriority(String requester, String priorityId)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, PRIORITY);
        deactivate(requester, PRIORITY, priorityId, priorities, "priority", Priority::deactivated,
                Priority::isActive);
    }

    /** The requester must exist, be active and have CONFIGURE_SYSTEM; if not, it is registered and denied. */
    private void checkPermission(String requester, String group) throws AccessDeniedException {
        if (!canConfigure(requester)) {
            auditRecorder.register(requester, ACCESS_DENIED, group);
            throw new AccessDeniedException(requester);
        }
    }

    private boolean canConfigure(String requester) {
        if (requester == null) {
            return false;
        }
        boolean activeUser = roleManagementService.getUsers().stream()
                .anyMatch(user -> requester.equals(user.getUsername()) && user.isActive());
        return activeUser && roleManagementService.getPermissions(requester).stream()
                .anyMatch(permission -> CONFIGURE_SYSTEM.equals(permission.getCode()));
    }

    private List<String> validate(TestType testType) {
        List<String> errors = new ArrayList<>();
        if (testType == null) {
            errors.add("The test type is required");
            return errors;
        }
        requireText(errors, testType.getId(), "The id");
        requireText(errors, testType.getName(), "The name");
        requireText(errors, testType.getModality(), "The modality");
        requireText(errors, testType.getRoom(), "The room");
        requireText(errors, testType.getPriority(), "The priority");
        if (testType.getDurationMinutes() <= 0) {
            errors.add("The duration must be greater than zero");
        }
        return errors;
    }

    private static void requireText(List<String> errors, String value, String field) {
        if (blank(value)) {
            errors.add(field + " is required");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private <T> T existing(IConfigurationRepository<T> repository, String id, String kind)
            throws ValidationException {
        T entry = id == null ? null : repository.findById(id);
        if (entry == null) {
            throw new ValidationException(List.of("There is no " + kind + " with the id " + id));
        }
        return entry;
    }

    /** Saves the entry if there are no errors, and registers whether it was created or changed. */
    private <T> T save(String requester, String group, String id, T entry, List<String> errors,
            IConfigurationRepository<T> repository) throws ValidationException {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        boolean isNew = repository.findById(id) == null;
        repository.save(entry);
        auditRecorder.register(requester, isNew ? CREATED : UPDATED, group + ":" + id);
        return entry;
    }

    /** Replaces the entry by its deactivated copy; an entry that is already inactive does not change. */
    private <T> void deactivate(String requester, String group, String id, IConfigurationRepository<T> repository,
            String kind, Function<T, T> deactivated, Predicate<T> isActive) throws ValidationException {
        T entry = existing(repository, id, kind);
        if (isActive.test(entry)) {
            repository.save(deactivated.apply(entry));
            auditRecorder.register(requester, DEACTIVATED, group + ":" + id);
        }
    }
}
