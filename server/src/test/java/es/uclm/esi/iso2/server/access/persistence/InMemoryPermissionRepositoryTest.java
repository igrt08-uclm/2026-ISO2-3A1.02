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

/** Tests of {@link InMemoryPermissionRepository}: C1 only has the permission {@code MANAGE_ROLES}. */
@DisplayName("InMemoryPermissionRepository")
class InMemoryPermissionRepositoryTest {
    private InMemoryPermissionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPermissionRepository();
    }

    // VIEW_AUDIT_LOG was removed on 06/11/2026, so that UC18 does not change UC19.
    @Test
    @DisplayName("findAll returns only MANAGE_ROLES")
    void findAllReturnsOnlyManageRoles() {
        // Act
        List<Permission> permissions = repository.findAll();

        // Assert
        assertAll(
                () -> assertEquals(1, permissions.size()),
                () -> assertEquals("MANAGE_ROLES", permissions.get(0).getCode()));
    }

    @Test
    @DisplayName("findByCode finds MANAGE_ROLES")
    void findByCodeFindsManageRoles() {
        // Act
        Permission permission = repository.findByCode("MANAGE_ROLES");

        // Assert
        assertSame(InMemoryPermissionRepository.MANAGE_ROLES, permission);
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
