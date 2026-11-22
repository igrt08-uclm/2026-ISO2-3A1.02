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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests of {@link InMemoryPermissionRepository}: the seven permissions of C1 and {@code findByCode}. */
@DisplayName("InMemoryPermissionRepository")
class InMemoryPermissionRepositoryTest {
    private InMemoryPermissionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPermissionRepository();
    }

    // VIEW_AUDIT_LOG was removed on 06/11/2026, so that UC18 does not change UC19.
    @Test
    @DisplayName("findAll returns the seven permissions of C1, in their order")
    void findAllReturnsTheSevenPermissionsInOrder() {
        // Act
        List<Permission> permissions = repository.findAll();

        // Assert
        assertAll(
                () -> assertEquals(7, permissions.size()),
                () -> assertEquals("MANAGE_ROLES", permissions.get(0).getCode()),
                () -> assertEquals("CONFIGURE_SYSTEM", permissions.get(1).getCode()),
                () -> assertEquals("CONFIGURE_INTEGRATIONS", permissions.get(2).getCode()),
                () -> assertEquals("MANAGE_PATIENTS", permissions.get(3).getCode()),
                () -> assertEquals("CONSULT_AUDIT", permissions.get(4).getCode()),
                () -> assertEquals("ORDER_PROCEDURES", permissions.get(5).getCode()),
                () -> assertEquals("MANAGE_APPOINTMENTS", permissions.get(6).getCode()));
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
    @DisplayName("TC19S-09a: CONSULT_AUDIT is the fifth permission, with its description")
    void tc19s09aConsultAuditIsTheFifthPermissionWithItsDescription() {
        // Act
        List<Permission> permissions = repository.findAll();

        // Assert
        Permission consultAudit = permissions.get(4);
        assertAll(
                () -> assertEquals(7, permissions.size()),
                () -> assertEquals("CONSULT_AUDIT", consultAudit.getCode()),
                () -> assertEquals("Consult the audit log of the system activity", consultAudit.getDescription()));
    }

    @Test
    @DisplayName("TC18S-01: ORDER_PROCEDURES and MANAGE_APPOINTMENTS are last, with their descriptions")
    void tc18s01NewPermissionsOfC4AndC5AreLastWithTheirDescriptions() {
        // Act
        List<Permission> permissions = repository.findAll();

        // Assert
        Permission orderProcedures = permissions.get(5);
        Permission manageAppointments = permissions.get(6);
        assertAll(
                () -> assertEquals("ORDER_PROCEDURES", orderProcedures.getCode()),
                () -> assertEquals("Order diagnostic and periodic follow-up procedures",
                        orderProcedures.getDescription()),
                () -> assertEquals("MANAGE_APPOINTMENTS", manageAppointments.getCode()),
                () -> assertEquals("Manage appointments, waitlists and daily schedules",
                        manageAppointments.getDescription()));
    }

    @Test
    @DisplayName("TC18S-02: findByCode finds ORDER_PROCEDURES and MANAGE_APPOINTMENTS")
    void tc18s02FindByCodeFindsTheNewPermissions() {
        // Act
        Permission orderProcedures = repository.findByCode("ORDER_PROCEDURES");
        Permission manageAppointments = repository.findByCode("MANAGE_APPOINTMENTS");

        // Assert
        assertAll(
                () -> assertSame(InMemoryPermissionRepository.ORDER_PROCEDURES, orderProcedures),
                () -> assertSame(InMemoryPermissionRepository.MANAGE_APPOINTMENTS, manageAppointments));
    }

    @Test
    @DisplayName("TC19S-09b: findByCode finds CONSULT_AUDIT")
    void tc19s09bFindByCodeFindsConsultAudit() {
        // Act
        Permission permission = repository.findByCode("CONSULT_AUDIT");

        // Assert
        assertSame(InMemoryPermissionRepository.CONSULT_AUDIT, permission);
    }

    // Each use of the class "existing code": every code is found, from the first to the last of the list
    // (boundary values of the loop of findByCode).
    @ParameterizedTest(name = "{0} is at position {1}")
    @CsvSource({
        "MANAGE_ROLES,           0",
        "CONFIGURE_SYSTEM,       1",
        "CONFIGURE_INTEGRATIONS, 2",
        "MANAGE_PATIENTS,        3",
        "CONSULT_AUDIT,          4",
        "ORDER_PROCEDURES,       5",
        "MANAGE_APPOINTMENTS,    6"
    })
    @DisplayName("TC18S-05: findByCode finds each of the seven permissions (each use)")
    void tc18s05FindByCodeFindsEachPermission(String code, int position) {
        // Act
        Permission permission = repository.findByCode(code);

        // Assert
        assertSame(repository.findAll().get(position), permission);
    }

    // Error guessing: the code is compared exactly, without trimming spaces or ignoring case.
    @ParameterizedTest(name = "\"{0}\" is not found")
    @ValueSource(strings = {" ORDER_PROCEDURES", "MANAGE_APPOINTMENTS ", "order_procedures", "ORDER PROCEDURES"})
    @DisplayName("TC18S-06: findByCode does not find a new code with spaces or in lower case")
    void tc18s06FindByCodeIsExact(String code) {
        // Act
        Permission permission = repository.findByCode(code);

        // Assert
        assertNull(permission);
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
