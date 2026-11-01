package es.uclm.esi.iso2.server.access.integration;

import java.util.Map;

/** Simulates the corporate SSO (D-016). The test users are the ones agreed in #13. */
public class AuthenticatorSystemSimulator implements IAuthenticatorSystem {
    private final Map<String, String> credentials = Map.of(
            "admin", "Admin.2026",
            "mgarcia", "Physician.2026",
            "lmartin", "Admissions.2026",
            "jfernandez", "Patient.2026",
            "pnavarro", "Inactive.2026");

    @Override
    public boolean validateCredentials(String username, String password) {
        return username != null && password != null && password.equals(credentials.get(username));
    }
}
