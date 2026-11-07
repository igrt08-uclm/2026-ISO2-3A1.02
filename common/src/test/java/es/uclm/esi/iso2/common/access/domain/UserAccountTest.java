package es.uclm.esi.iso2.common.access.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link UserAccount}: the account keeps its own unmodifiable copy of the roles. */
@DisplayName("UserAccount")
class UserAccountTest {
    private Role physician;
    private Role patient;

    @BeforeEach
    void setUp() {
        physician = new Role("PHYSICIAN", "Physician", "Test physician role");
        patient = new Role("PATIENT", "Patient", "Test patient role");
    }

    @Test
    @DisplayName("The roles list cannot be modified")
    void rolesListCannotBeModified() {
        // Arrange
        UserAccount account = new UserAccount("ana", "Ana Test", "ana@test.test", true, List.of(physician));

        // Act and Assert
        assertThrows(UnsupportedOperationException.class, () -> account.getRoles().add(patient));
    }

    @Test
    @DisplayName("Later changes to the source list do not affect the account")
    void laterChangesToSourceListDoNotAffectAccount() {
        // Arrange
        List<Role> source = new ArrayList<>(List.of(physician));
        UserAccount account = new UserAccount("ana", "Ana Test", "ana@test.test", true, source);

        // Act
        source.add(patient);

        // Assert
        assertAll(
                () -> assertEquals(1, account.getRoles().size()),
                () -> assertEquals("PHYSICIAN", account.getRoles().get(0).getId()));
    }
}
