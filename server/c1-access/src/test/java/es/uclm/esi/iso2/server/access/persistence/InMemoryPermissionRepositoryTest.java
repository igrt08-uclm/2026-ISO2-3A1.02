package es.uclm.esi.iso2.server.access.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import es.uclm.esi.iso2.common.access.domain.Permission;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests of {@link InMemoryPermissionRepository}: the five permissions of C1 and {@code findByCode}. */
@DisplayName("InMemoryPermissionRepository")
class InMemoryPermissionRepositoryTest {
    private InMemoryPermissionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPermissionRepository();
    }

    // VIEW_AUDIT_LOG was removed on 06/11/2026, so that UC18 does not change UC19.
    @Test
    @DisplayName("findAll returns the five permissions of C1, with CONSULT_AUDIT last")
    void findAllReturnsManageRolesConfigureSystemConfigureIntegrationsManagePatientsAndConsultAudit() {
        // Act
        List<Permission> permissions = repository.findAll();

        // Assert
        assertAll(
                () -> assertEquals(5, permissions.size()),
                () -> assertEquals("MANAGE_ROLES", permissions.get(0).getCode()),
                () -> assertEquals("CONFIGURE_SYSTEM", permissions.get(1).getCode()),
                () -> assertEquals("CONFIGURE_INTEGRATIONS", permissions.get(2).getCode()),
                () -> assertEquals("MANAGE_PATIENTS", permissions.get(3).getCode()),
                () -> assertEquals("CONSULT_AUDIT", permissions.get(4).getCode()));
    }

    @Test
    @DisplayName("findByCode finds MANAGE_ROLES")
    void findByCodeFindsManageRoles() {
        // Act
        Permission permission = repository.findByCode("MANAGE_ROLES");

        // Assert
        assertSame(InMemoryPermissionRepository.MANAGE_ROLES, permission);
    }

    @Test
    @DisplayName("TC19S-09a: CONSULT_AUDIT is the last permission, with its description")
    void tc19s09aConsultAuditIsTheLastPermissionWithItsDescription() {
        // Act
        List<Permission> permissions = repository.findAll();

        // Assert
        Permission last = permissions.get(permissions.size() - 1);
        assertAll(
                () -> assertEquals(5, permissions.size()),
                () -> assertEquals("CONSULT_AUDIT", last.getCode()),
                () -> assertEquals("Consult the audit log of the system activity", last.getDescription()));
    }

    @Test
    @DisplayName("TC19S-09b: findByCode finds CONSULT_AUDIT")
    void tc19s09bFindByCodeFindsConsultAudit() {
        // Act
        Permission permission = repository.findByCode("CONSULT_AUDIT");

        // Assert
        assertSame(InMemoryPermissionRepository.CONSULT_AUDIT, permission);
    }

    // "manage_roles" shows that the lookup is case sensitive.
    @ParameterizedTest(name = "\"{0}\" is not found")
    @NullAndEmptySource
    @ValueSource(strings = {"VIEW_AUDIT_LOG", "manage_roles"})
    @DisplayName("findByCode returns null for an unknown, null or empty code")
    void findByCodeReturnsNullForUnknownCode(String code) {
        // Act
        Permission permission = repository.findByCode(code);

        // Assert
        assertNull(permission);
    }
}
