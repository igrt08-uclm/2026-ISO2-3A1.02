package es.uclm.esi.iso2.server.access.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.AuthenticationException;
import es.uclm.esi.iso2.common.access.domain.AuditLog;
import es.uclm.esi.iso2.server.access.integration.AuthenticatorSystemSimulator;
import es.uclm.esi.iso2.server.access.persistence.InMemoryAuditLogRepository;
import es.uclm.esi.iso2.server.access.persistence.InMemoryUserAccountRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests of {@link AuditService} with the real repositories, {@link AuthenticationService} and the SSO
 * simulator, using the test users agreed in #13.
 */
@DisplayName("AuditService with the real login and repositories")
class AuditServiceIntegrationTest {
    private AuditService auditService;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        InMemoryUserAccountRepository users = new InMemoryUserAccountRepository();
        auditService = new AuditService(new InMemoryAuditLogRepository(), users);
        authenticationService = new AuthenticationService(new AuthenticatorSystemSimulator(), users, auditService);
    }

    private static List<String> actionsAndAuthors(List<AuditLog> entries) {
        return entries.stream().map(entry -> entry.getAction() + " " + entry.getAuthor()).toList();
    }

    @Test
    @DisplayName("admin can consult the successful and failed logins")
    void adminCanConsultTheLogins() throws AuthenticationException, AccessDeniedException {
        // Arrange
        authenticationService.login("admin", "Admin.2026");
        assertThrows(AuthenticationException.class, () -> authenticationService.login("nobody", "any"));

        // Act
        List<AuditLog> logins = auditService.getActivityLog("admin", "target=authentication");

        // Assert
        assertAll(
                () -> assertEquals(List.of("LOGIN_FAILED nobody", "LOGIN_OK admin"), actionsAndAuthors(logins)),
                () -> assertEquals("authentication:UNKNOWN_USER", logins.get(0).getTargetResource()));
    }

    // mgarcia is active, but not an administrator; pnavarro has the role Physician and is inactive.
    @Test
    @DisplayName("Other users are rejected, and admin sees their attempts")
    void otherUsersAreRejectedAndAdminSeesTheirAttempts() throws AccessDeniedException {
        // Act
        assertThrows(AccessDeniedException.class, () -> auditService.getActivityLog("mgarcia", null));
        assertThrows(AccessDeniedException.class, () -> auditService.getActivityLog("pnavarro", null));
        List<AuditLog> attempts = auditService.getActivityLog("admin", "action=AUDIT_ACCESS_DENIED");

        // Assert
        assertEquals(List.of("AUDIT_ACCESS_DENIED pnavarro", "AUDIT_ACCESS_DENIED mgarcia"),
                actionsAndAuthors(attempts));
    }
}
