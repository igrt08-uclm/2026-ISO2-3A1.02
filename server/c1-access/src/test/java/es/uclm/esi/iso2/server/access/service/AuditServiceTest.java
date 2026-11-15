package es.uclm.esi.iso2.server.access.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.domain.AuditLog;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests of {@link AuditService}: what is registered and who can consult the log. The repositories are
 * hand-written test doubles, so no real test data is used.
 */
@DisplayName("AuditService (unit)")
class AuditServiceTest {
    private static final Role ADMINISTRATOR = new Role("SYSTEM_ADMINISTRATOR", "System administrator", "Test role");
    private static final Role PHYSICIAN = new Role("PHYSICIAN", "Physician", "Test role");

    private FakeAuditLogRepository auditLog;
    private AuditService service;

    @BeforeEach
    void setUp() {
        auditLog = new FakeAuditLogRepository();
        FakeUserAccountRepository users = new FakeUserAccountRepository(
                new UserAccount("ana", "Ana Test", "ana@test.test", true, List.of(ADMINISTRATOR)),
                new UserAccount("eva", "Eva Test", "eva@test.test", true, List.of(PHYSICIAN, ADMINISTRATOR)),
                new UserAccount("bob", "Bob Test", "bob@test.test", true, List.of(PHYSICIAN)),
                new UserAccount("old", "Old Test", "old@test.test", false, List.of(ADMINISTRATOR)));
        service = new AuditService(auditLog, users);
    }

    @Test
    @DisplayName("register saves one entry with its data and the current date")
    void registerSavesOneEntryWithItsData() {
        // Act
        service.register("ana", "LOGIN_OK", "authentication");

        // Assert
        AuditLog entry = auditLog.entries().get(0);
        assertAll(
                () -> assertEquals(1, auditLog.entries().size()),
                () -> assertEquals("ana", entry.getAuthor()),
                () -> assertEquals("LOGIN_OK", entry.getAction()),
                () -> assertEquals("authentication", entry.getTargetResource()),
                () -> assertNotNull(entry.getTimestamp()));
    }

    @Test
    @DisplayName("Each entry gets a different id")
    void eachEntryGetsADifferentId() {
        // Act
        service.register("ana", "LOGIN_OK", "authentication");
        service.register("ana", "LOGIN_OK", "authentication");

        // Assert
        assertNotEquals(auditLog.entries().get(0).getId(), auditLog.entries().get(1).getId());
    }

    // A failed login has to be registered even if the user does not exist.
    @ParameterizedTest(name = "author = {0}")
    @NullSource
    @ValueSource(strings = {"ghost"})
    @DisplayName("register saves entries of users that do not exist")
    void registerSavesEntriesOfUsersThatDoNotExist(String author) {
        // Act
        service.register(author, "LOGIN_FAILED", "authentication:UNKNOWN_USER");

        // Assert
        assertEquals(author, auditLog.entries().get(0).getAuthor());
    }

    @ParameterizedTest(name = "{0} is an active administrator")
    @ValueSource(strings = {"ana", "eva"})
    @DisplayName("An active administrator gets the result of the repository")
    void activeAdministratorGetsTheResultOfTheRepository(String requester) throws AccessDeniedException {
        // Arrange
        service.register("bob", "VIEW_REPORT", "PAT-1");

        // Act
        List<AuditLog> entries = service.getActivityLog(requester, "author=bob");

        // Assert
        assertAll(
                () -> assertEquals("VIEW_REPORT", entries.get(0).getAction()),
                () -> assertTrue(auditLog.receivedFilters().contains("author=bob")));
    }

    @ParameterizedTest(name = "requester = {0}")
    @NullSource
    @ValueSource(strings = {"ghost", "old", "bob"})
    @DisplayName("Unknown, null, inactive or non-administrator requesters are rejected")
    void otherRequestersAreRejected(String requester) {
        // Act
        AccessDeniedException exception =
                assertThrows(AccessDeniedException.class, () -> service.getActivityLog(requester, null));

        // Assert: the repository is only read to number the new entry, never with the filters of the requester.
        assertAll(
                () -> assertEquals(requester, exception.getRequester()),
                () -> assertTrue(auditLog.receivedFilters().stream().allMatch(filters -> filters == null)));
    }

    @Test
    @DisplayName("A rejected requester never reaches the repository search")
    void rejectedRequesterNeverReachesTheSearch() {
        // Act
        assertThrows(AccessDeniedException.class, () -> service.getActivityLog("bob", "author=ana"));

        // Assert
        assertFalse(auditLog.receivedFilters().contains("author=ana"));
    }

    // D12, option (a): the server knows who asks, so the consultation is audited too.
    @Test
    @DisplayName("An allowed consultation is registered, but not in its own results")
    void allowedConsultationIsRegisteredButNotInItsOwnResults() throws AccessDeniedException {
        // Act
        List<AuditLog> entries = service.getActivityLog("ana", null);

        // Assert
        AuditLog consultation = auditLog.entries().get(0);
        assertAll(
                () -> assertTrue(entries.isEmpty()),
                () -> assertEquals("ana", consultation.getAuthor()),
                () -> assertEquals("AUDIT_CONSULTED", consultation.getAction()),
                () -> assertEquals("audit", consultation.getTargetResource()));
    }

    @Test
    @DisplayName("A rejected consultation registers who tried")
    void rejectedConsultationRegistersWhoTried() {
        // Act
        assertThrows(AccessDeniedException.class, () -> service.getActivityLog("bob", null));

        // Assert
        AuditLog attempt = auditLog.entries().get(0);
        assertAll(
                () -> assertEquals("bob", attempt.getAuthor()),
                () -> assertEquals("AUDIT_ACCESS_DENIED", attempt.getAction()),
                () -> assertEquals("audit", attempt.getTargetResource()));
    }

    @Test
    @DisplayName("Wrong filters give an error and no consultation is registered")
    void wrongFiltersGiveAnErrorAndNoConsultationIsRegistered() {
        // Arrange
        FakeAuditLogRepository rejectingRepository = new FakeAuditLogRepository() {
            @Override
            public List<AuditLog> find(String filters) {
                throw new IllegalArgumentException("Wrong filters");
            }
        };
        AuditService serviceWithWrongFilters = new AuditService(rejectingRepository,
                new FakeUserAccountRepository(new UserAccount("ana", "Ana Test", "ana@test.test", true,
                        List.of(ADMINISTRATOR))));

        // Act and Assert
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> serviceWithWrongFilters.getActivityLog("ana", "colour=red")),
                () -> assertTrue(rejectingRepository.entries().isEmpty()));
    }
}
