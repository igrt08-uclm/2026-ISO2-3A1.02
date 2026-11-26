package es.uclm.esi.iso2.server.appointments.service;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.IAuditRecorder;
import es.uclm.esi.iso2.common.access.contracts.IRoleManagementService;
import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.configuration.contracts.ISystemParameterService;
import es.uclm.esi.iso2.common.configuration.domain.AppointmentPolicy;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.Protocol;
import es.uclm.esi.iso2.common.configuration.domain.Schedule;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import es.uclm.esi.iso2.common.orders.contracts.IClinicalOrderService;
import es.uclm.esi.iso2.common.orders.domain.ClinicalOrder;
import es.uclm.esi.iso2.common.orders.domain.DiagnosticOrder;
import es.uclm.esi.iso2.common.orders.domain.FollowUpOrder;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeed;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeedStatus;
import es.uclm.esi.iso2.common.patients.contracts.IPatientService;
import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.patients.domain.Sex;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.appointments.persistence.InMemoryAppointmentRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The test doubles of the tests of {@link AppointmentService} (UC13, #131): C1 (roles and audit), C2 (test types
 * and schedules), C3 (patients) and C4 (pending needs and their orders). They only implement what C5 uses and they
 * count the calls, so a test can check when a service of another component is not reached.
 */
final class TestDoubles {
    /** Stores appointments normally until a test requests a save failure. */
    static final class AppointmentRepositoryFake extends InMemoryAppointmentRepository {
        private boolean rejectSaves;

        void rejectSaves() {
            rejectSaves = true;
        }

        /** {@inheritDoc} */
        @Override
        public void save(Appointment appointment) {
            if (rejectSaves) {
                throw new IllegalStateException("Appointment storage unavailable");
            }
            super.save(appointment);
        }
    }

    private TestDoubles() {
    }

    /** C1: {@code lmartin} has MANAGE_APPOINTMENTS; the others do not. */
    static final class FakeRoleManagementService implements IRoleManagementService {
        private static final Permission MANAGE_APPOINTMENTS =
                new Permission("MANAGE_APPOINTMENTS", "Manage appointments, waitlists and daily schedules");

        private static final List<UserAccount> USERS = List.of(
                new UserAccount("lmartin", "Lucía Martín Pérez", "lucia.martin@sescam.test", true, List.of()),
                new UserAccount("admin", "Laura Sánchez Ruiz", "laura.sanchez@sescam.test", true, List.of()),
                new UserAccount("mgarcia", "Miguel García López", "miguel.garcia@sescam.test", true, List.of()),
                new UserAccount("pnavarro", "Pedro Navarro Díaz", "pedro.navarro@sescam.test", false, List.of()));

        private static final Map<String, List<Permission>> PERMISSIONS =
                Map.of("lmartin", List.of(MANAGE_APPOINTMENTS));

        @Override
        public List<UserAccount> getUsers() {
            return USERS;
        }

        @Override
        public List<Permission> getPermissions(String username) {
            return PERMISSIONS.getOrDefault(username, List.of());
        }

        @Override
        public List<Role> getRoles(String username) {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public List<Permission> getAvailablePermissions() {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public List<Role> getAvailableRoles() {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public void assignRole(String requester, Role role, UserAccount user) throws AccessDeniedException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public void revokeRole(String requester, Role role, UserAccount user) throws AccessDeniedException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public void assignPermission(String requester, Role role, Permission permission)
                throws AccessDeniedException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public void revokePermission(String requester, Role role, Permission permission)
                throws AccessDeniedException {
            throw new UnsupportedOperationException("Not used by C5");
        }
    }

    /** C1: keeps every audit entry. */
    static final class RecordingAuditRecorder implements IAuditRecorder {
        record AuditEntry(String author, String action, String targetResource) {
        }

        private final List<AuditEntry> entries = new ArrayList<>();

        @Override
        public void register(String author, String action, String targetResource) {
            entries.add(new AuditEntry(author, action, targetResource));
        }

        List<AuditEntry> entries() {
            return Collections.unmodifiableList(entries);
        }

        void clear() {
            entries.clear();
        }
    }

    /** C3: {@code P-0001}, {@code P-0002} and {@code P-0003} are active, {@code P-0004} is not. */
    static final class FakePatientService implements IPatientService {
        private final List<Patient> patients = new ArrayList<>();
        private int getPatientCalls;

        FakePatientService() {
            patients.add(new Patient("P-0001", "HC-0000001", null, "Lucía Ejemplo Prueba", LocalDate.of(1980, 1, 1),
                    Sex.FEMALE, null, null, null, true));
            patients.add(new Patient("P-0002", "HC-0000002", null, "Mario Demo Simulado", LocalDate.of(1975, 6, 15),
                    Sex.MALE, null, null, null, true));
            patients.add(new Patient("P-0004", "HC-0000004", null, "Paciente Dado De Alta", LocalDate.of(1960, 9, 9),
                    Sex.UNSPECIFIED, null, null, null, false));
        }

        @Override
        public Patient getPatient(String requester, String patientId) throws ValidationException {
            getPatientCalls++;
            return patients.stream()
                    .filter(patient -> patient.getPatientId().equals(patientId))
                    .findFirst()
                    .orElseThrow(() -> new ValidationException(
                            List.of("There is no patient with the id " + patientId)));
        }

        int getPatientCalls() {
            return getPatientCalls;
        }

        @Override
        public List<Patient> searchPatients(String requester, String filters) throws AccessDeniedException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public Patient registerPatient(String requester, Patient patient)
                throws AccessDeniedException, ValidationException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public Patient modifyPatient(String requester, String patientId, Patient patient)
                throws AccessDeniedException, ValidationException {
            throw new UnsupportedOperationException("Not used by C5");
        }
    }

    /**
     * C2: four test types and the schedules of their rooms: Room 1 ({@code TT-01}, 15 minutes) on Tuesdays from 09:00
     * to 11:00, Room 2 ({@code TT-02}, 30 minutes) on Tuesdays from 09:00 to 13:00, and Room 4 ({@code TT-04}), whose
     * only schedule is inactive. {@code TT-03} is inactive. C2 keeps one schedule per room (its key is the resource).
     */
    static final class FakeSystemParameterService implements ISystemParameterService {
        static final LocalDate TUESDAY = LocalDate.of(2026, 11, 24);
        private static final List<TestType> TEST_TYPES = List.of(
                new TestType("TT-01", "Chest X-ray", "X-ray", "Room 1", 15, null, false, false, null, "ROUTINE",
                        false, true),
                new TestType("TT-02", "Blood test", "Laboratory", "Room 2", 30, null, false, false, null, "ROUTINE",
                        false, true),
                new TestType("TT-03", "Old scan", "CT", "Room 3", 20, null, false, false, null, "ROUTINE", false,
                        false),
                new TestType("TT-04", "Bone scan", "Nuclear medicine", "Room 4", 15, null, false, false, null,
                        "ROUTINE", false, true));
        private static final List<Schedule> SCHEDULES = List.of(
                new Schedule("Room 1", Set.of(DayOfWeek.TUESDAY), LocalTime.of(9, 0), LocalTime.of(11, 0), true),
                new Schedule("Room 2", Set.of(DayOfWeek.TUESDAY), LocalTime.of(9, 0), LocalTime.of(13, 0), true),
                new Schedule("Room 4", Set.of(DayOfWeek.TUESDAY), LocalTime.of(9, 0), LocalTime.of(11, 0), false));

        private final List<TestType> testTypes = new ArrayList<>(TEST_TYPES);
        private final Set<String> deniedRequesters = new HashSet<>();
        private boolean deniesSchedules;
        private int calls;
        private List<Priority> priorities =
                List.of(new Priority("URGENT", "Urgent", "As soon as possible", true));

        /** Replaces the priority tiers that C2 returns, in their order. */
        void usePriorities(Priority... tiers) {
            priorities = List.of(tiers);
        }

        void addTestType(TestType type) {
            testTypes.add(type);
        }

        @Override
        public TestType getTestType(String requester, String testTypeId) throws ValidationException {
            calls++;
            return testTypes.stream()
                    .filter(testType -> testType.getId().equals(testTypeId))
                    .findFirst()
                    .orElseThrow(() -> new ValidationException(
                            List.of("There is no test type with the id " + testTypeId)));
        }

        @Override
        public List<Priority> getPriorities(String requester) {
            calls++;
            return priorities;
        }

        int calls() {
            return calls;
        }

        @Override
        public List<Schedule> getSchedules(String requester) throws AccessDeniedException {
            calls++;
            if (deniesSchedules && (deniedRequesters.isEmpty() || deniedRequesters.contains(requester))) {
                throw new AccessDeniedException(requester);
            }
            return SCHEDULES;
        }

        /**
         * From now on {@link #getSchedules(String)} throws {@code AccessDeniedException} for the given requesters, as
         * C2 does for a requester without permission; with no requester it denies every one.
         */
        void denySchedulesTo(String... requesters) {
            deniesSchedules = true;
            deniedRequesters.addAll(Arrays.asList(requesters));
        }

        @Override
        public List<TestType> getTestTypes(String requester) throws AccessDeniedException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public TestType createTestType(String requester, TestType testType)
                throws AccessDeniedException, ValidationException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public TestType updateTestType(String requester, String testTypeId, TestType testType)
                throws AccessDeniedException, ValidationException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public void deactivateTestType(String requester, String testTypeId)
                throws AccessDeniedException, ValidationException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public List<Protocol> getProtocols(String requester) throws AccessDeniedException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public Protocol saveProtocol(String requester, Protocol protocol)
                throws AccessDeniedException, ValidationException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public void deactivateProtocol(String requester, String protocolId)
                throws AccessDeniedException, ValidationException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public Schedule saveSchedule(String requester, Schedule schedule)
                throws AccessDeniedException, ValidationException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public void deactivateSchedule(String requester, String resource)
                throws AccessDeniedException, ValidationException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public List<AppointmentPolicy> getAppointmentPolicies(String requester) throws AccessDeniedException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public AppointmentPolicy saveAppointmentPolicy(String requester, AppointmentPolicy policy)
                throws AccessDeniedException, ValidationException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public void deactivateAppointmentPolicy(String requester, String policyId)
                throws AccessDeniedException, ValidationException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public Priority savePriority(String requester, Priority priority)
                throws AccessDeniedException, ValidationException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public void deactivatePriority(String requester, String priorityId)
                throws AccessDeniedException, ValidationException {
            throw new UnsupportedOperationException("Not used by C5");
        }
    }

    /**
     * C4: an order that is not a {@link DiagnosticOrder}, as a follow-up order of UC07, which has no priority. It only
     * exists in the tests, because the follow-up order of C4 1.1.0 is not in this branch.
     */
    static final class OrderWithoutPriority extends ClinicalOrder {
        OrderWithoutPriority(String orderId, String patientId, String testTypeId, String requester,
                LocalDate creationDate) {
            super(orderId, patientId, testTypeId, requester, "Control of the nodule", "Follow-up of a known lesion",
                    creationDate);
        }
    }

    /** C4: the pending needs and their orders, kept in memory so the tests can check what C5 asks for. */
    static final class FakeClinicalOrderService implements IClinicalOrderService {
        private final Map<String, SchedulingNeed> needs = new LinkedHashMap<>();
        private final Map<String, ClinicalOrder> orders = new LinkedHashMap<>();
        private int pendingCalls;
        private ValidationException pendingRejection;

        void rejectPendingWith(ValidationException rejection) {
            pendingRejection = rejection;
        }

        void addNeed(SchedulingNeed need) {
            needs.put(need.getNeedId(), need);
        }

        void addOrder(ClinicalOrder order) {
            orders.put(order.getOrderId(), order);
        }

        SchedulingNeed need(String needId) {
            return needs.get(needId);
        }

        int pendingCalls() {
            return pendingCalls;
        }

        /**
         * Puts a need in a status directly, as if another use case of C4 had changed it, so that a test can build the
         * state in which C4 answers that the need is already in that status.
         *
         * @param needId the id of the need
         * @param status its new status
         */
        void forceStatus(String needId, SchedulingNeedStatus status) {
            SchedulingNeed need = needs.get(needId);
            needs.put(needId, new SchedulingNeed(needId, need.getOrderId(), need.getDueDate(), status));
        }

        @Override
        public List<SchedulingNeed> getPendingSchedulingNeeds(String requester) throws AccessDeniedException {
            pendingCalls++;
            return needs.values().stream()
                    .filter(need -> need.getStatus() == SchedulingNeedStatus.PENDING)
                    .toList();
        }

        @Override
        public ClinicalOrder getOrder(String requester, String orderId)
                throws AccessDeniedException, ValidationException {
            ClinicalOrder order = orders.get(orderId);
            if (order == null) {
                throw new ValidationException(List.of("There is no order with the id " + orderId));
            }
            return order;
        }

        @Override
        public void markSchedulingNeedBooked(String requester, String needId)
                throws AccessDeniedException, ValidationException {
            changeStatus(needId, SchedulingNeedStatus.BOOKED, "booked");
        }

        @Override
        public void markSchedulingNeedPending(String requester, String needId)
                throws AccessDeniedException, ValidationException {
            if (pendingRejection != null) {
                throw pendingRejection;
            }
            changeStatus(needId, SchedulingNeedStatus.PENDING, "pending");
        }

        private void changeStatus(String needId, SchedulingNeedStatus status, String text) throws ValidationException {
            SchedulingNeed need = needs.get(needId);
            if (need == null) {
                throw new ValidationException(List.of("There is no scheduling need with the id " + needId));
            }
            if (need.getStatus() == status) {
                throw new ValidationException(
                        List.of("The scheduling need " + needId + " is already " + text));
            }
            needs.put(needId, new SchedulingNeed(needId, need.getOrderId(), need.getDueDate(), status));
        }

        @Override
        public DiagnosticOrder orderDiagnosticProcedure(String requester, DiagnosticOrder order)
                throws AccessDeniedException, ValidationException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public FollowUpOrder orderFollowUp(String requester, FollowUpOrder order)
                throws AccessDeniedException, ValidationException {
            throw new UnsupportedOperationException("Not used by C5");
        }

        @Override
        public List<ClinicalOrder> getOrdersOfPatient(String requester, String patientId)
                throws AccessDeniedException, ValidationException {
            throw new UnsupportedOperationException("Not used by C5");
        }
    }
}
