package es.uclm.esi.iso2.common.access.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link Role}: the role keeps its data and its own unmodifiable copy of the permissions. */
@DisplayName("Role")
class RoleTest {
    private Permission read;
    private Permission write;

    @BeforeEach
    void setUp() {
        read = new Permission("TEST_READ", "Test read permission");
        write = new Permission("TEST_WRITE", "Test write permission");
    }

    @Test
    @DisplayName("A role created with three arguments keeps its data and has no permissions")
    void roleWithoutPermissionsKeepsItsData() {
        // Act
        Role role = new Role("TEST_ROLE", "Test role", "Role used in the tests");

        // Assert
        assertAll(
                () -> assertEquals("TEST_ROLE", role.getId()),
                () -> assertEquals("Test role", role.getName()),
                () -> assertEquals("Role used in the tests", role.getDescription()),
                () -> assertTrue(role.getPermissions().isEmpty()));
    }

    @Test
    @DisplayName("A role created with permissions keeps them in the same order")
    void roleKeepsItsPermissionsInOrder() {
        // Act
        Role role = new Role("TEST_ROLE", "Test role", "Role used in the tests", List.of(write, read));

        // Assert
        assertAll(
                () -> assertEquals(2, role.getPermissions().size()),
                () -> assertEquals("TEST_WRITE", role.getPermissions().get(0).getCode()),
                () -> assertEquals("TEST_READ", role.getPermissions().get(1).getCode()));
    }

    @Test
    @DisplayName("Later changes to the source list do not affect the role")
    void laterChangesToSourceListDoNotAffectRole() {
        // Arrange
        List<Permission> source = new ArrayList<>(List.of(read));
        Role role = new Role("TEST_ROLE", "Test role", "Role used in the tests", source);

        // Act
        source.add(write);

        // Assert
        assertAll(
                () -> assertEquals(1, role.getPermissions().size()),
                () -> assertEquals("TEST_READ", role.getPermissions().get(0).getCode()));
    }

    @Test
    @DisplayName("The permissions list cannot be modified")
    void permissionsListCannotBeModified() {
        // Arrange
        Role role = new Role("TEST_ROLE", "Test role", "Role used in the tests", List.of(read));

        // Act and Assert
        assertThrows(UnsupportedOperationException.class, () -> role.getPermissions().add(write));
    }
}
