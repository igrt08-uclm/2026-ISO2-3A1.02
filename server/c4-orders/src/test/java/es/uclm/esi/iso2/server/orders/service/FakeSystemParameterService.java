package es.uclm.esi.iso2.server.orders.service;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.configuration.contracts.ISystemParameterService;
import es.uclm.esi.iso2.common.configuration.domain.AppointmentPolicy;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.Protocol;
import es.uclm.esi.iso2.common.configuration.domain.Schedule;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.util.List;

/**
 * Test double of {@link ISystemParameterService} with the catalogue of C2: the active test types {@code TT-01} and
 * {@code TT-02}, the deactivated {@code TT-03}, and the active priorities {@code URGENT}, {@code PREFERENTIAL} and
 * {@code ROUTINE} plus the deactivated {@code OLD}. It counts the calls, so the tests can check when C2 is not
 * reached.
 */
class FakeSystemParameterService implements ISystemParameterService {
    private static final List<TestType> TEST_TYPES = List.of(
            new TestType("TT-01", "Chest X-ray", "X-ray", "Room 1", 10, null, false, false, null, "ROUTINE", false,
                    true),
            new TestType("TT-02", "Blood test", "Laboratory", "Room 2", 5, null, false, false, null, "ROUTINE", true,
                    true),
            new TestType("TT-03", "Old scan", "CT", "Room 3", 20, null, false, false, null, "ROUTINE", false, false));

    private static final List<Priority> PRIORITIES = List.of(
            new Priority("URGENT", "Urgent", "As soon as possible", true),
            new Priority("PREFERENTIAL", "Preferential", "Before the routine ones", true),
            new Priority("ROUTINE", "Routine", "Normal waiting time", true),
            new Priority("OLD", "Old tier", "Deactivated tier", false));

    private int calls;

    @Override
    public TestType getTestType(String requester, String testTypeId) throws ValidationException {
        calls++;
        return TEST_TYPES.stream()
                .filter(testType -> testType.getId().equals(testTypeId))
                .findFirst()
                .orElseThrow(() -> new ValidationException(List.of("There is no test type with the id " + testTypeId)));
    }

    @Override
    public List<Priority> getPriorities(String requester) {
        calls++;
        return PRIORITIES;
    }

    /** How many times the service asked C2 for a parameter. */
    int calls() {
        return calls;
    }

    @Override
    public List<TestType> getTestTypes(String requester) throws AccessDeniedException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public TestType createTestType(String requester, TestType testType)
            throws AccessDeniedException, ValidationException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public TestType updateTestType(String requester, String testTypeId, TestType testType)
            throws AccessDeniedException, ValidationException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public void deactivateTestType(String requester, String testTypeId)
            throws AccessDeniedException, ValidationException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public List<Protocol> getProtocols(String requester) throws AccessDeniedException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public Protocol saveProtocol(String requester, Protocol protocol)
            throws AccessDeniedException, ValidationException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public void deactivateProtocol(String requester, String protocolId)
            throws AccessDeniedException, ValidationException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public List<Schedule> getSchedules(String requester) throws AccessDeniedException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public Schedule saveSchedule(String requester, Schedule schedule)
            throws AccessDeniedException, ValidationException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public void deactivateSchedule(String requester, String resource)
            throws AccessDeniedException, ValidationException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public List<AppointmentPolicy> getAppointmentPolicies(String requester) throws AccessDeniedException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public AppointmentPolicy saveAppointmentPolicy(String requester, AppointmentPolicy policy)
            throws AccessDeniedException, ValidationException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public void deactivateAppointmentPolicy(String requester, String policyId)
            throws AccessDeniedException, ValidationException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public Priority savePriority(String requester, Priority priority)
            throws AccessDeniedException, ValidationException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public void deactivatePriority(String requester, String priorityId)
            throws AccessDeniedException, ValidationException {
        throw new UnsupportedOperationException("Not used by C4");
    }
}
