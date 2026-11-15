package es.uclm.esi.iso2.server.access.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import es.uclm.esi.iso2.server.access.persistence.InMemoryPermissionRepository;
import es.uclm.esi.iso2.server.access.persistence.InMemoryRoleRepository;
import es.uclm.esi.iso2.server.access.persistence.InMemoryUserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Integration tests of {@code hasPermission} with the real {@link RoleManagementService} and the seed data of C1
 * (patch 1.2.2, #112): the new permissions reach the users through their roles, as C2 and C3 will use them.
 */
@DisplayName("RoleManagementService.hasPermission (integration)")
class RoleManagementServiceHasPermissionIntegrationTest {
    private RoleManagementService service;

    @BeforeEach
    void setUp() {
        service = new RoleManagementService(new InMemoryRoleRepository(), new InMemoryPermissionRepository(),
                new InMemoryUserAccountRepository(), new RecordingAuditRecorder());
    }

    @ParameterizedTest(name = "{0} has {1}: {2}")
    @CsvSource({
        "admin,      CONFIGURE_INTEGRATIONS, true",
        "admin,      CONFIGURE_SYSTEM,       true",
        "admin,      MANAGE_ROLES,           true",
        "admin,      MANAGE_PATIENTS,        false",
        "lmartin,    MANAGE_PATIENTS,        true",
        "lmartin,    CONFIGURE_INTEGRATIONS, false",
        "mgarcia,    MANAGE_PATIENTS,        false",
        "jfernandez, CONFIGURE_SYSTEM,       false",
        "pnavarro,   MANAGE_PATIENTS,        false"
    })
    @DisplayName("TC-P-11: the permissions of the seed roles reach their users, and only them")
    void seedPermissionsReachTheirUsers(String username, String permission, boolean expected) {
        assertEquals(expected, service.hasPermission(username, permission));
    }
}
