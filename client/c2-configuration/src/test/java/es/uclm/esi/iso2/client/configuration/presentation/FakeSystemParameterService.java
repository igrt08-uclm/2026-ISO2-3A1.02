package es.uclm.esi.iso2.client.configuration.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.configuration.contracts.ISystemParameterService;
import es.uclm.esi.iso2.common.configuration.domain.AppointmentPolicy;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.Protocol;
import es.uclm.esi.iso2.common.configuration.domain.Schedule;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Test double of {@link ISystemParameterService} for the screens of UC17. It keeps the entries in memory and does
 * not validate them: the validation is the server's (Q6), so a test asks for it with {@link #rejectNextChange}. It
 * can also deny every call, as the server does when the user has no permission.
 */
class FakeSystemParameterService implements ISystemParameterService {
    private final Map<String, TestType> testTypes = new LinkedHashMap<>();
    private final Map<String, Protocol> protocols = new LinkedHashMap<>();
    private final Map<String, Schedule> schedules = new LinkedHashMap<>();
    private final Map<String, AppointmentPolicy> policies = new LinkedHashMap<>();
    private final Map<String, Priority> priorities = new LinkedHashMap<>();

    private boolean denyAll;
    private List<String> nextErrors;
    private int changes;
    private Object lastSaved;
    private Object lastRequest;
    private String lastCall;
    private String lastDeactivated;

    /** From now on every call is denied. */
    void denyAll() {
        denyAll = true;
    }

    /** The next create, update, save or deactivate is rejected with these errors. */
    void rejectNextChange(String... errors) {
        nextErrors = List.of(errors);
    }

    /** How many create, update, save and deactivate calls reached the service without being rejected. */
    int changes() {
        return changes;
    }

    /** The last entry that was saved without being rejected. */
    Object lastSaved() {
        return lastSaved;
    }

    /** The last entry sent to a create, update or save, even if it was rejected. */
    Object lastRequest() {
        return lastRequest;
    }

    /** The name of the last change method that was called, for example {@code "updateTestType"}. */
    String lastCall() {
        return lastCall;
    }

    /** The id of the last entry that was deactivated without being rejected. */
    String lastDeactivated() {
        return lastDeactivated;
    }

    void add(TestType testType) {
        testTypes.put(testType.getId(), testType);
    }

    void add(Protocol protocol) {
        protocols.put(protocol.getId(), protocol);
    }

    void add(Schedule schedule) {
        schedules.put(schedule.getResource(), schedule);
    }

    void add(AppointmentPolicy policy) {
        policies.put(policy.getId(), policy);
    }

    void add(Priority priority) {
        priorities.put(priority.getId(), priority);
    }

    private void check(String requester) throws AccessDeniedException {
        if (denyAll) {
            throw new AccessDeniedException(requester);
        }
    }

    private void checkChange() throws ValidationException {
        if (nextErrors != null) {
            List<String> errors = nextErrors;
            nextErrors = null;
            throw new ValidationException(errors);
        }
    }

    private <T> T save(String requester, Map<String, T> group, String id, T entry)
            throws AccessDeniedException, ValidationException {
        check(requester);
        lastRequest = entry;
        checkChange();
        group.put(id, entry);
        changes++;
        lastSaved = entry;
        return entry;
    }

    private <T> void deactivate(String requester, Map<String, T> group, String id, UnaryOperator<T> deactivated)
            throws AccessDeniedException, ValidationException {
        check(requester);
        checkChange();
        group.computeIfPresent(id, (key, entry) -> deactivated.apply(entry));
        changes++;
        lastDeactivated = id;
    }

    private <T> List<T> list(String requester, Map<String, T> group) throws AccessDeniedException {
        check(requester);
        return new ArrayList<>(group.values());
    }

    @Override
    public List<TestType> getTestTypes(String requester) throws AccessDeniedException {
        return list(requester, testTypes);
    }

    @Override
    public TestType getTestType(String requester, String testTypeId) throws AccessDeniedException {
        check(requester);
        return testTypes.get(testTypeId);
    }

    @Override
    public TestType createTestType(String requester, TestType testType)
            throws AccessDeniedException, ValidationException {
        lastCall = "createTestType";
        return save(requester, testTypes, testType.getId(), testType);
    }

    @Override
    public TestType updateTestType(String requester, String testTypeId, TestType testType)
            throws AccessDeniedException, ValidationException {
        lastCall = "updateTestType";
        return save(requester, testTypes, testTypeId, testType);
    }

    @Override
    public void deactivateTestType(String requester, String testTypeId)
            throws AccessDeniedException, ValidationException {
        deactivate(requester, testTypes, testTypeId, TestType::deactivated);
    }

    @Override
    public List<Protocol> getProtocols(String requester) throws AccessDeniedException {
        return list(requester, protocols);
    }

    @Override
    public Protocol saveProtocol(String requester, Protocol protocol)
            throws AccessDeniedException, ValidationException {
        return save(requester, protocols, protocol.getId(), protocol);
    }

    @Override
    public void deactivateProtocol(String requester, String protocolId)
            throws AccessDeniedException, ValidationException {
        deactivate(requester, protocols, protocolId, Protocol::deactivated);
    }

    @Override
    public List<Schedule> getSchedules(String requester) throws AccessDeniedException {
        return list(requester, schedules);
    }

    @Override
    public Schedule saveSchedule(String requester, Schedule schedule)
            throws AccessDeniedException, ValidationException {
        return save(requester, schedules, schedule.getResource(), schedule);
    }

    @Override
    public void deactivateSchedule(String requester, String resource)
            throws AccessDeniedException, ValidationException {
        deactivate(requester, schedules, resource, Schedule::deactivated);
    }

    @Override
    public List<AppointmentPolicy> getAppointmentPolicies(String requester) throws AccessDeniedException {
        return list(requester, policies);
    }

    @Override
    public AppointmentPolicy saveAppointmentPolicy(String requester, AppointmentPolicy policy)
            throws AccessDeniedException, ValidationException {
        return save(requester, policies, policy.getId(), policy);
    }

    @Override
    public void deactivateAppointmentPolicy(String requester, String policyId)
            throws AccessDeniedException, ValidationException {
        deactivate(requester, policies, policyId, AppointmentPolicy::deactivated);
    }

    @Override
    public List<Priority> getPriorities(String requester) throws AccessDeniedException {
        return list(requester, priorities);
    }

    @Override
    public Priority savePriority(String requester, Priority priority)
            throws AccessDeniedException, ValidationException {
        return save(requester, priorities, priority.getId(), priority);
    }

    @Override
    public void deactivatePriority(String requester, String priorityId)
            throws AccessDeniedException, ValidationException {
        deactivate(requester, priorities, priorityId, Priority::deactivated);
    }

}
