package es.uclm.esi.iso2.client.appointments.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.configuration.contracts.ISystemParameterService;
import es.uclm.esi.iso2.common.configuration.domain.AppointmentPolicy;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.Protocol;
import es.uclm.esi.iso2.common.configuration.domain.Schedule;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import java.util.List;

/**
 * Test double of {@link ISystemParameterService} for the screen of UC15: three test types and two priority tiers. The
 * screen only reads those two lists.
 */
class FakeParameterService implements ISystemParameterService {

    @Override
    public List<TestType> getTestTypes(String requester) throws AccessDeniedException {
        return List.of(testType("XRAY-CHEST"), testType("MRI-KNEE"), testType("CT-HEAD"));
    }

    @Override
    public List<Priority> getPriorities(String requester) {
        return List.of(new Priority("URGENT", "Urgent", "As soon as possible", true),
                new Priority("ROUTINE", "Routine", "In order", true));
    }

    private static TestType testType(String id) {
        return new TestType(id, id, "Imaging", "Room 1", 15, null, false, false, null, "ROUTINE", false, true);
    }

    private static UnsupportedOperationException unused() {
        return new UnsupportedOperationException("Not used by UC15");
    }

    @Override
    public TestType getTestType(String requester, String testTypeId) {
        throw unused();
    }

    @Override
    public TestType createTestType(String requester, TestType testType) {
        throw unused();
    }

    @Override
    public TestType updateTestType(String requester, String testTypeId, TestType testType) {
        throw unused();
    }

    @Override
    public void deactivateTestType(String requester, String testTypeId) {
        throw unused();
    }

    @Override
    public List<Protocol> getProtocols(String requester) {
        throw unused();
    }

    @Override
    public Protocol saveProtocol(String requester, Protocol protocol) {
        throw unused();
    }

    @Override
    public void deactivateProtocol(String requester, String protocolId) {
        throw unused();
    }

    @Override
    public List<Schedule> getSchedules(String requester) {
        throw unused();
    }

    @Override
    public Schedule saveSchedule(String requester, Schedule schedule) {
        throw unused();
    }

    @Override
    public void deactivateSchedule(String requester, String resource) {
        throw unused();
    }

    @Override
    public List<AppointmentPolicy> getAppointmentPolicies(String requester) {
        throw unused();
    }

    @Override
    public AppointmentPolicy saveAppointmentPolicy(String requester, AppointmentPolicy policy) {
        throw unused();
    }

    @Override
    public void deactivateAppointmentPolicy(String requester, String policyId) {
        throw unused();
    }

    @Override
    public Priority savePriority(String requester, Priority priority) {
        throw unused();
    }

    @Override
    public void deactivatePriority(String requester, String priorityId) {
        throw unused();
    }
}
