package es.uclm.esi.iso2.client.orders.presentation;

import es.uclm.esi.iso2.common.configuration.contracts.ISystemParameterService;
import es.uclm.esi.iso2.common.configuration.domain.AppointmentPolicy;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.Protocol;
import es.uclm.esi.iso2.common.configuration.domain.Schedule;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import java.util.ArrayList;
import java.util.List;

/**
 * Test double of {@link ISystemParameterService} (C2) for the screen of UC06: it only gives the test types and the
 * priorities that a test puts in it, in that order. The other operations of C2 are not used by UC06.
 */
class FakeSystemParameterService implements ISystemParameterService {
    private final List<TestType> testTypes = new ArrayList<>();
    private final List<Priority> priorities = new ArrayList<>();
    private RuntimeException testTypeReadFailure;

    /**
     * The usual data: the six active test types and the three active priorities of the seed data of C2, plus the
     * invented deactivated test type {@code XRAY-OLD} and the invented deactivated priority {@code OLD-TIER}.
     */
    static FakeSystemParameterService withSeedData() {
        FakeSystemParameterService service = new FakeSystemParameterService();
        service.testTypes.add(testType("XRAY-CHEST", "Chest X-ray", true));
        service.testTypes.add(testType("CT-HEAD", "Head CT", true));
        service.testTypes.add(testType("MRI-KNEE", "Knee MRI", true));
        service.testTypes.add(testType("XRAY-OLD", "Old chest X-ray", false));
        service.testTypes.add(testType("US-ABDOMEN", "Abdominal ultrasound", true));
        service.testTypes.add(testType("MAMMO-BILATERAL", "Bilateral mammography", true));
        service.testTypes.add(testType("DEXA-SPINE", "Bone densitometry", true));
        service.priorities.add(new Priority("URGENT", "Urgent", "Within hours", true));
        service.priorities.add(new Priority("OLD-TIER", "Old tier", "No longer used", false));
        service.priorities.add(new Priority("PREFERENTIAL", "Preferential", "Within a few days", true));
        service.priorities.add(new Priority("ROUTINE", "Routine", "Within a few weeks", true));
        return service;
    }

    /** A test type with only the data that the screen reads. */
    static TestType testType(String id, String name, boolean active) {
        return new TestType(id, name, "Modality", "Room 1", 10, "", false, false, "", "ROUTINE", false, active);
    }

    /** The test types that the service gives, to change them in a test. */
    List<TestType> testTypes() {
        return testTypes;
    }

    /** The priorities that the service gives, to change them in a test. */
    List<Priority> priorities() {
        return priorities;
    }

    void failTestTypeReads(RuntimeException failure) {
        testTypeReadFailure = failure;
    }

    @Override
    public List<TestType> getTestTypes(String requester) {
        if (testTypeReadFailure != null) {
            throw testTypeReadFailure;
        }
        return List.copyOf(testTypes);
    }

    @Override
    public List<Priority> getPriorities(String requester) {
        return List.copyOf(priorities);
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

    private static UnsupportedOperationException unused() {
        return new UnsupportedOperationException("Not used by UC06");
    }
}
