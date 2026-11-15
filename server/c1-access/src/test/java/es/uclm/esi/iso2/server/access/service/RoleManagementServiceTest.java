package es.uclm.esi.iso2.server.access.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import es.uclm.esi.iso2.server.access.service.RecordingAuditRecorder.AuditEntry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests of {@link RoleManagementService}: the queries, the four changes, who can make them, and the last active
 * administrator. The repositories are hand-written test doubles, so no real test data is used.
 */
@DisplayName("RoleManagementService (unit)")
class RoleManagementServiceTest {
    private static final Permission MANAGE_ROLES = new Permission("MANAGE_ROLES", "Test manage roles permission");
    private static final Permission OTHER = new Permission("TEST_OTHER", "Test other permission");
    private static final Role ADMINISTRATOR =
            new Role("SYSTEM_ADMINISTRATOR", "System administrator", "Test role", List.of(MANAGE_ROLES));
    private static final Role PHYSICIAN = new Role("PHYSICIAN", "Physician", "Test role", List.of(OTHER));
    private static final Role PATIENT = new Role("PATIENT", "Patient", "Test role");
    private static final Role ADMISSIONS_STAFF =
            new Role("ADMISSIONS_STAFF", "Admissions staff", "Test role", List.of(OTHER));

    private FakeRoleRepository roles;
    private FakeUserAccountRepository users;
    private RecordingAuditRecorder audit;
    private RoleManagementService service;

    @BeforeEach
    void setUp() {
        roles = new FakeRoleRepository(ADMINISTRATOR, PHYSICIAN, PATIENT, ADMISSIONS_STAFF);
        users = new FakeUserAccountRepository(
                user("ana", true, ADMINISTRATOR),
                user("bob", true, PHYSICIAN),
                user("cid", true, PATIENT),
                user("old", false, ADMINISTRATOR));
        audit = new RecordingAuditRecorder();
        service = new RoleManagementService(roles, new FakePermissionRepository(MANAGE_ROLES, OTHER), users, audit);
    }

    private static UserAccount user(String username, boolean active, Role... userRoles) {
        return new UserAccount(username, username + " Test", username + "@test.test", active, List.of(userRoles));
    }

    private static List<String> ids(List<Role> list) {
        return list.stream().map(Role::getId).toList();
    }

    private static List<String> codes(List<Permission> list) {
        return list.stream().map(Permission::getCode).toList();
    }

    /** A change of roles or permissions, which the service can reject. */
    private interface Change {
        void run() throws AccessDeniedException;
    }

    /** Runs one of the four operations of change with the given values. */
    private Change change(String operation, String requester, Role role, UserAccount user, Permission permission) {
        return switch (operation) {
            case "assignRole" -> () -> service.assignRole(requester, role, user);
            case "revokeRole" -> () -> service.revokeRole(requester, role, user);
            case "assignPermission" -> () -> service.assignPermission(requester, role, permission);
            case "revokePermission" -> () -> service.revokePermission(requester, role, permission);
            default -> throw new IllegalArgumentException(operation);
        };
    }

    private void assertNothingChanged() {
        assertAll(
                () -> assertEquals(0, users.saveCalls()),
                () -> assertEquals(0, roles.saveCalls()),
                () -> assertTrue(audit.entries().isEmpty()));
    }

    // --- Queries ---

    // The account keeps a copy of its roles: a change of permissions is in the repository of roles.
    @Test
    @DisplayName("getRoles returns the permissions saved in the repository of roles")
    void getRolesReturnsTheSavedPermissions() {
        // Arrange
        roles.add(new Role("PHYSICIAN", "Physician", "Test role", List.of(MANAGE_ROLES)));

        // Act
        List<Role> bobRoles = service.getRoles("bob");

        // Assert
        assertAll(
                () -> assertEquals(List.of("PHYSICIAN"), ids(bobRoles)),
                () -> assertEquals(List.of("MANAGE_ROLES"), codes(bobRoles.get(0).getPermissions())));
    }

    @ParameterizedTest(name = "username = {0}")
    @NullSource
    @ValueSource(strings = "ghost")
    @DisplayName("getRoles and getPermissions reject an unknown or null user")
    void queriesRejectAnUnknownUser(String username) {
        // Act and Assert
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> service.getRoles(username)),
                () -> assertThrows(IllegalArgumentException.class, () -> service.getPermissions(username)));
    }

    @Test
    @DisplayName("getPermissions returns each permission once, even if two roles share it")
    void getPermissionsReturnsEachPermissionOnce() {
        // Arrange
        users.add(user("dan", true, PHYSICIAN, ADMISSIONS_STAFF, ADMINISTRATOR));

        // Act
        List<Permission> permissions = service.getPermissions("dan");

        // Assert
        assertEquals(List.of("TEST_OTHER", "MANAGE_ROLES"), codes(permissions));
    }

    @Test
    @DisplayName("getAvailablePermissions returns the permissions of the repository")
    void getAvailablePermissionsReturnsThePermissionsOfTheRepository() {
        // Act and Assert
        assertEquals(List.of("MANAGE_ROLES", "TEST_OTHER"), codes(service.getAvailablePermissions()));
    }

    // The client shows the users and the roles in lists (#16).
    @Test
    @DisplayName("getUsers returns every user ordered by username")
    void getUsersReturnsTheUsersOrdered() {
        // Arrange
        users.add(user("zoe", true, PATIENT));
        users.add(user("abel", true, PATIENT));

        // Act
        List<String> usernames = service.getUsers().stream().map(UserAccount::getUsername).toList();

        // Assert
        assertEquals(List.of("abel", "ana", "bob", "cid", "old", "zoe"), usernames);
    }

    @Test
    @DisplayName("getAvailableRoles returns every role ordered by name")
    void getAvailableRolesReturnsTheRolesOrdered() {
        // Act
        List<String> names = service.getAvailableRoles().stream().map(Role::getName).toList();

        // Assert
        assertEquals(List.of("Admissions staff", "Patient", "Physician", "System administrator"), names);
    }

    // --- The four changes ---

    @Test
    @DisplayName("assignRole saves the account with the new role and registers it")
    void assignRoleSavesAndRegisters() throws AccessDeniedException {
        // Act
        service.assignRole("ana", PATIENT, users.findByUsername("bob"));

        // Assert
        assertAll(
                () -> assertEquals(List.of("PHYSICIAN", "PATIENT"), ids(users.findByUsername("bob").getRoles())),
                () -> assertEquals(1, users.saveCalls()),
                () -> assertEquals(List.of(new AuditEntry("ana", "ROLE_ASSIGNED", "user:bob/role:PATIENT")),
                        audit.entries()));
    }

    @Test
    @DisplayName("revokeRole saves the account without the role and registers it")
    void revokeRoleSavesAndRegisters() throws AccessDeniedException {
        // Act
        service.revokeRole("ana", PHYSICIAN, users.findByUsername("bob"));

        // Assert
        assertAll(
                () -> assertTrue(users.findByUsername("bob").getRoles().isEmpty()),
                () -> assertEquals(1, users.saveCalls()),
                () -> assertEquals(List.of(new AuditEntry("ana", "ROLE_REVOKED", "user:bob/role:PHYSICIAN")),
                        audit.entries()));
    }

    @Test
    @DisplayName("assignPermission saves the role with the new permission and registers it")
    void assignPermissionSavesAndRegisters() throws AccessDeniedException {
        // Act
        service.assignPermission("ana", PATIENT, MANAGE_ROLES);

        // Assert
        assertAll(
                () -> assertEquals(List.of("MANAGE_ROLES"), codes(roles.findById("PATIENT").getPermissions())),
                () -> assertEquals(1, roles.saveCalls()),
                () -> assertEquals(
                        List.of(new AuditEntry("ana", "PERMISSION_ASSIGNED", "role:PATIENT/permission:MANAGE_ROLES")),
                        audit.entries()));
    }

    @Test
    @DisplayName("revokePermission saves the role without the permission and registers it")
    void revokePermissionSavesAndRegisters() throws AccessDeniedException {
        // Act
        service.revokePermission("ana", PHYSICIAN, OTHER);

        // Assert
        assertAll(
                () -> assertTrue(roles.findById("PHYSICIAN").getPermissions().isEmpty()),
                () -> assertEquals(1, roles.saveCalls()),
                () -> assertEquals(
                        List.of(new AuditEntry("ana", "PERMISSION_REVOKED", "role:PHYSICIAN/permission:TEST_OTHER")),
                        audit.entries()));
    }

    @ParameterizedTest(name = "{0} of {1}")
    @CsvSource({
        "assignRole,       PHYSICIAN, the role bob already has",
        "revokeRole,       PATIENT,   a role bob does not have",
        "assignPermission, PHYSICIAN, a permission the role already has",
        "revokePermission, PATIENT,   a permission the role does not have"
    })
    @DisplayName("A change that is already done saves and registers nothing")
    void aChangeAlreadyDoneDoesNothing(String operation, String roleId, String description) {
        // Arrange
        Role role = roles.findById(roleId);

        // Act
        assertDoesNotThrow(() -> change(operation, "ana", role, users.findByUsername("bob"), OTHER).run());

        // Assert
        assertNothingChanged();
    }

    // --- Who can make a change ---

    // "old" is inactive and "bob" has no MANAGE_ROLES (#17).
    @ParameterizedTest(name = "requester = {0}")
    @NullSource
    @ValueSource(strings = {"ghost", "old", "bob"})
    @DisplayName("A requester that cannot manage roles is rejected in the four operations")
    void aRequesterThatCannotManageRolesIsRejected(String requester) {
        // Arrange
        UserAccount cid = users.findByUsername("cid");
        List<String> operations = List.of("assignRole", "revokeRole", "assignPermission", "revokePermission");

        // Act and Assert
        for (String operation : operations) {
            // Each one would be a real change if the requester were allowed.
            Role role = switch (operation) {
                case "assignRole", "revokePermission" -> PHYSICIAN;
                default -> PATIENT;
            };
            Permission permission = operation.equals("assignPermission") ? MANAGE_ROLES : OTHER;
            AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                    () -> change(operation, requester, role, cid, permission).run(), operation);
            assertEquals(requester, exception.getRequester());
        }
        AuditEntry denied = new AuditEntry(requester, "ROLE_CHANGE_DENIED", "roles");
        assertAll(
                () -> assertEquals(List.of(denied, denied, denied, denied), audit.entries()),
                () -> assertEquals(0, users.saveCalls()),
                () -> assertEquals(0, roles.saveCalls()),
                () -> assertEquals(List.of("PATIENT"), ids(users.findByUsername("cid").getRoles())));
    }

    // The check uses the permission, not the role (#17).
    @Test
    @DisplayName("A requester with MANAGE_ROLES through another role can make a change")
    void aRequesterWithThePermissionThroughAnotherRoleCanMakeAChange() throws AccessDeniedException {
        // Arrange
        roles.add(new Role("PHYSICIAN", "Physician", "Test role", List.of(MANAGE_ROLES)));

        // Act
        service.assignRole("bob", ADMISSIONS_STAFF, users.findByUsername("cid"));

        // Assert
        assertEquals(List.of(new AuditEntry("bob", "ROLE_ASSIGNED", "user:cid/role:ADMISSIONS_STAFF")),
                audit.entries());
    }

    // A change of permissions takes effect at once (#17).
    @Test
    @DisplayName("A permission given to a role lets its users make changes at once")
    void aPermissionGivenToARoleTakesEffectAtOnce() throws AccessDeniedException {
        // Arrange
        assertThrows(AccessDeniedException.class,
                () -> service.assignRole("bob", ADMISSIONS_STAFF, users.findByUsername("cid")));

        // Act
        service.assignPermission("ana", PHYSICIAN, MANAGE_ROLES);

        // Assert
        assertDoesNotThrow(() -> service.assignRole("bob", ADMISSIONS_STAFF, users.findByUsername("cid")));
    }

    // --- Values that do not exist ---

    @ParameterizedTest(name = "{0} with {1}")
    @CsvSource({
        "assignRole,       a null user",
        "assignRole,       an unknown user",
        "assignRole,       a null role",
        "revokeRole,       an unknown role",
        "assignPermission, a null role",
        "assignPermission, a null permission",
        "revokePermission, an unknown permission"
    })
    @DisplayName("An unknown or null value is rejected and nothing changes")
    void anUnknownValueIsRejected(String operation, String value) {
        // Arrange
        UserAccount user = switch (value) {
            case "a null user" -> null;
            case "an unknown user" -> user("ghost", true, PATIENT);
            default -> users.findByUsername("cid");
        };
        Role role = switch (value) {
            case "a null role" -> null;
            case "an unknown role" -> new Role("GHOST", "Ghost", "Test role");
            default -> PATIENT;
        };
        Permission permission = switch (value) {
            case "a null permission" -> null;
            case "an unknown permission" -> new Permission("GHOST", "Test permission");
            default -> MANAGE_ROLES;
        };

        // Act and Assert
        assertThrows(IllegalArgumentException.class, () -> change(operation, "ana", role, user, permission).run());
        assertNothingChanged();
    }

    // --- The last active administrator ---

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"revokeRole", "revokePermission"})
    @DisplayName("The only active user with MANAGE_ROLES cannot lose it")
    void theOnlyAdministratorCannotLoseManageRoles(String operation) {
        // Arrange
        users = new FakeUserAccountRepository(user("ana", true, ADMINISTRATOR), user("bob", true, PHYSICIAN));
        service = new RoleManagementService(roles, new FakePermissionRepository(MANAGE_ROLES, OTHER), users, audit);

        // Act and Assert
        assertThrows(IllegalStateException.class,
                () -> change(operation, "ana", ADMINISTRATOR, users.findByUsername("ana"), MANAGE_ROLES).run());
        assertAll(
                () -> assertNothingChanged(),
                () -> assertEquals(List.of("SYSTEM_ADMINISTRATOR"), ids(users.findByUsername("ana").getRoles())),
                () -> assertEquals(List.of("MANAGE_ROLES"),
                        codes(roles.findById("SYSTEM_ADMINISTRATOR").getPermissions())));
    }

    @Test
    @DisplayName("An administrator can lose the role if another active one remains")
    void anAdministratorCanLoseTheRoleIfAnotherRemains() throws AccessDeniedException {
        // Arrange
        users.add(user("eva", true, ADMINISTRATOR));

        // Act
        service.revokeRole("ana", ADMINISTRATOR, users.findByUsername("ana"));

        // Assert
        assertTrue(users.findByUsername("ana").getRoles().isEmpty());
    }

    @Test
    @DisplayName("A role can lose MANAGE_ROLES if an active user keeps it through another role")
    void aRoleCanLoseManageRolesIfAnotherRoleKeepsIt() throws AccessDeniedException {
        // Arrange
        roles.add(new Role("PHYSICIAN", "Physician", "Test role", List.of(MANAGE_ROLES)));

        // Act
        service.revokePermission("ana", ADMINISTRATOR, MANAGE_ROLES);

        // Assert
        assertTrue(roles.findById("SYSTEM_ADMINISTRATOR").getPermissions().isEmpty());
    }

    // "old" is a System administrator, but inactive, so it cannot manage the roles.
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"revokeRole", "revokePermission"})
    @DisplayName("An inactive administrator does not count as the one that remains")
    void anInactiveAdministratorDoesNotCount(String operation) {
        // Act and Assert
        assertThrows(IllegalStateException.class,
                () -> change(operation, "ana", ADMINISTRATOR, users.findByUsername("ana"), MANAGE_ROLES).run());
        assertNothingChanged();
    }
}
