package es.uclm.esi.iso2.server.access.service;

import es.uclm.esi.iso2.common.access.contracts.AuthenticationException;
import es.uclm.esi.iso2.common.access.contracts.AuthenticationFailureReason;
import es.uclm.esi.iso2.common.access.contracts.IAuthenticationService;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import es.uclm.esi.iso2.server.access.integration.IAuthenticatorSystem;
import es.uclm.esi.iso2.server.access.persistence.IUserAccountRepository;

public class AuthenticationService implements IAuthenticationService {
    private static final String LOGIN_OK = "LOGIN_OK";
    private static final String LOGIN_FAILED = "LOGIN_FAILED";
    private static final String TARGET_RESOURCE = "authentication";

    private final IAuthenticatorSystem authenticatorSystem;
    private final IUserAccountRepository userAccountRepository;
    private final IAuditRecorder auditRecorder;

    public AuthenticationService(IAuthenticatorSystem authenticatorSystem,
            IUserAccountRepository userAccountRepository, IAuditRecorder auditRecorder) {
        this.authenticatorSystem = authenticatorSystem;
        this.userAccountRepository = userAccountRepository;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public UserAccount login(String username, String password) throws AuthenticationException {
        UserAccount user = userAccountRepository.findByUsername(username);
        if (user == null) {
            throw failure(username, AuthenticationFailureReason.UNKNOWN_USER);
        }
        if (!authenticatorSystem.validateCredentials(username, password)) {
            throw failure(username, AuthenticationFailureReason.WRONG_PASSWORD);
        }
        if (!user.isActive()) {
            throw failure(username, AuthenticationFailureReason.INACTIVE_USER);
        }
        auditRecorder.register(username, LOGIN_OK, TARGET_RESOURCE);
        return user;
    }

    private AuthenticationException failure(String username, AuthenticationFailureReason reason) {
        auditRecorder.register(username, LOGIN_FAILED, TARGET_RESOURCE + ":" + reason);
        return new AuthenticationException(reason);
    }
}
