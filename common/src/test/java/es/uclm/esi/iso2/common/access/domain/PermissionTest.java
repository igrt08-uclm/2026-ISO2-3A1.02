package es.uclm.esi.iso2.common.access.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link Permission}: it keeps the data it was created with. */
@DisplayName("Permission")
class PermissionTest {

    @Test
    @DisplayName("A permission keeps its code and its description")
    void permissionKeepsItsData() {
        // Act
        Permission permission = new Permission("TEST_PERMISSION", "Test permission");

        // Assert
        assertAll(
                () -> assertEquals("TEST_PERMISSION", permission.getCode()),
                () -> assertEquals("Test permission", permission.getDescription()));
    }
}
