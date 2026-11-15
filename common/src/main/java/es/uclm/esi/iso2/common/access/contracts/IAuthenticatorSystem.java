package es.uclm.esi.iso2.server.access.integration;

public interface IAuthenticatorSystem {
    boolean validateCredentials(String username, String password);
}
