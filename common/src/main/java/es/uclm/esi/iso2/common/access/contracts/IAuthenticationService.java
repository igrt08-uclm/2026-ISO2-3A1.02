package es.uclm.esi.iso2.common.access.contracts;

import es.uclm.esi.iso2.common.access.domain.UserAccount;

public interface IAuthenticationService {
    UserAccount login(String username, String password) throws AuthenticationException;
}
