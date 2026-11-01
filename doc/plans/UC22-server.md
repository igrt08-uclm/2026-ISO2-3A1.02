# Plan – UC22 – Server (#13)

| | |
|---|---|
| **Sub-issue** | #13 *UC22 – Server (module v1.0.0)* · branch `feature/UC22-server` |
| **Use case** | #9 *UC22 – Log in (module v1.0.0)* · FR22 · `server` and `common` `1.0.0` |
| **Specification** | #9, #13 and the wiki page *Requirements and Use Cases* (FR22) |
| **Design** | `doc/design/C1-server-design.png`, `doc/design/UC22-login-communication.png` · contracts: section 6 of #8 *C1. Access and security* |
| **Plan approved by** | Javier, 31/10/2026 |

## Questions and answers
Answered in the comments of #13 by David (Requirements), Denis (Design) and Adam (Testing), and by Javier on 31/10/2026.
- **Q:** What does `login` return? **A:** The `UserAccount` with its roles (David).
- **Q:** How is an error reported? **A:** `AuthenticationException` with an `AuthenticationFailureReason`: `UNKNOWN_USER`,
  `WRONG_PASSWORD` or `INACTIVE_USER` (Denis).
- **Q:** Is an inactive user rejected? **A:** Yes, even if the SSO accepts the password (David, D-022).
- **Q:** Audit? **A:** A simulated `IAuditRecorder` (D-023), `AuditRecorderSimulator`; successful and failed logins are
  registered as `LOGIN_OK` and `LOGIN_FAILED` (David, Denis).
- **Q:** Who implements `IUserAccountRepository`? **A:** `InMemoryUserAccountRepository` (Denis).
- **Q:** Which classes go in `common` for UC22? **A:** `IAuthenticationService`, `UserAccount`, `Role`,
  `AuthenticationException` and `AuthenticationFailureReason` (Denis).
- **Q:** Java packages? **A:** `es.uclm.esi.iso2.common.access.contracts` and `.domain`;
  `es.uclm.esi.iso2.server.access.service`, `.integration` and `.persistence` (Denis).
- **Q:** `pass` or `password`? **A:** `password` (David).
- **Q:** Change of password? **A:** Out of UC22 in Iteration 1 (David, D-009, D01 of the analysis).
- **Q:** Order of the checks in `login`? **A:** `findByUsername`, then the SSO, then `active` (proposed by Denis, confirmed
  by Javier). `doc/design/UC22-login-communication.png` shows the SSO first; this order replaces it.
- **Q:** Test users? **A:** The five users in Adam's comment in #13, preloaded (proposed by Denis, confirmed by Javier).
- **Q:** Where are the passwords? **A:** In `AuthenticatorSystemSimulator`; `UserAccount` has no password, and the
  repository keeps the accounts (Javier).
- **Q:** Values of `Role`? **A:** `name` from Adam's table, `id` such as `PHYSICIAN`, and a short `description` (Javier).
- **Q:** `targetResource` in the audit? **A:** `authentication` for a successful login and `authentication:<reason>` for a
  failed one, e.g. `authentication:WRONG_PASSWORD` (Javier).
- **Q:** Type of `AuthenticationException`? **A:** Checked: `extends Exception`, so the client has to handle it (Javier).
- **Q:** Null or empty values? **A:** No special case: a null or empty username gives `UNKNOWN_USER`, and a null or empty
  password gives `WRONG_PASSWORD` (Javier).
- **Q:** Who writes the tests? **A:** Adam designs and writes them after the implementation, on this branch; the agent
  writes no JUnit tests (Javier).
- **Q:** What does `save` do with an account without a username? **A:** It rejects it, because it left the repository
  unusable (found by Adam, fix approved by Javier). As Adam's laptop is being repaired, Javier writes the test of this
  fix for now.

## Files
| File | Action | What |
|---|---|---|
| `common/src/main/java/es/uclm/esi/iso2/common/access/domain/Role.java` | create | `id`, `name`, `description` |
| `common/src/main/java/es/uclm/esi/iso2/common/access/domain/UserAccount.java` | create | `username`, `fullName`, `email`, `active` and its roles |
| `common/src/main/java/es/uclm/esi/iso2/common/access/contracts/AuthenticationFailureReason.java` | create | the three reasons |
| `common/src/main/java/es/uclm/esi/iso2/common/access/contracts/AuthenticationException.java` | create | checked exception with its `reason` |
| `common/src/main/java/es/uclm/esi/iso2/common/access/contracts/IAuthenticationService.java` | create | `login(username, password) : UserAccount` |
| `server/src/main/java/es/uclm/esi/iso2/server/access/integration/IAuthenticatorSystem.java` | create | `validateCredentials(username, password) : boolean` |
| `server/src/main/java/es/uclm/esi/iso2/server/access/integration/AuthenticatorSystemSimulator.java` | create | simulated SSO with the passwords of the test users |
| `server/src/main/java/es/uclm/esi/iso2/server/access/persistence/IUserAccountRepository.java` | create | `findByUsername`, which returns `null` if the user is missing, and `save` |
| `server/src/main/java/es/uclm/esi/iso2/server/access/persistence/InMemoryUserAccountRepository.java` | create | `users : List<UserAccount>`, preloaded with the test users and their roles |
| `server/src/main/java/es/uclm/esi/iso2/server/access/service/IAuditRecorder.java` | create | `register(author, action, targetResource)` |
| `server/src/main/java/es/uclm/esi/iso2/server/access/service/AuditRecorderSimulator.java` | create | writes each entry to the standard output; replaced by `AuditService` in UC19 (D-023) |
| `server/src/main/java/es/uclm/esi/iso2/server/access/service/AuthenticationService.java` | create | implements `IAuthenticationService`; receives its three dependencies in the constructor |

No `pom.xml` changes: `server` and `common` stay at `1.0.0-SNAPSHOT`.

## Steps
Tick each step with the hash of its commit (green `mvn -B verify`).
- [x] 1. This plan. `90a8239`
- [x] 2. `common`: `Role` and `UserAccount`. `a261cb7`
- [x] 3. `common`: `AuthenticationFailureReason`, `AuthenticationException` and `IAuthenticationService`. `9bfe29a`
- [x] 4. `server`: `IAuthenticatorSystem` and `AuthenticatorSystemSimulator`. `f77b8ca`
- [x] 5. `server`: `IUserAccountRepository` and `InMemoryUserAccountRepository`. `a742dce`
- [x] 6. `server`: `IAuditRecorder` and `AuditRecorderSimulator`. `49b7f25`
- [x] 7. `server`: `AuthenticationService`, with the checks in the agreed order. `773d21b`
- [x] 8. Check the five users and an unknown user with a temporary program outside the repository (not committed), and
      write the result here. Result on 31/10/2026, with 12 logins, all as expected and each one registered in the audit:
      the four active users log in with their roles (`mgarcia` with two); `pnavarro` with his password gives
      `INACTIVE_USER`, and with a wrong one `WRONG_PASSWORD`; an unknown, null or empty username gives `UNKNOWN_USER`;
      a wrong, null or empty password gives `WRONG_PASSWORD`.

## How it works and how to try it
`AuthenticationService.login(username, password)` makes three checks, in this order:
1. `IUserAccountRepository.findByUsername`: if the user does not exist, `UNKNOWN_USER`.
2. `IAuthenticatorSystem.validateCredentials`: if the SSO rejects the password, `WRONG_PASSWORD`.
3. `UserAccount.isActive`: if the account is inactive, `INACTIVE_USER`.

If they pass, `login` returns the `UserAccount` with its roles. Otherwise it throws `AuthenticationException` with the
reason. Each attempt is registered through `IAuditRecorder`: `LOGIN_OK` with `authentication`, or `LOGIN_FAILED` with
`authentication:<reason>`. `AuditRecorderSimulator` writes it to the standard output.

To try it:
- `mvn -B verify` builds everything and runs the tests.
- Create the service with its three dependencies and call `login` from a small `main` or a test, with
  `common/target/classes` and `server/target/classes` in the classpath:
  `new AuthenticationService(new AuthenticatorSystemSimulator(), new InMemoryUserAccountRepository(), new AuditRecorderSimulator())`.
- The test users and their passwords are in Adam's comment in #13. For a test, pass your own `IAuditRecorder` to the
  constructor to check what is registered.

## Verification
- [x] `mvn -B verify` is green.
- [x] The SSO validates the username and password, and `login` returns the `UserAccount` or an error (#13).
- [x] The interfaces match the contracts: `login(username, password)` of section 6 of #8.
- [x] Each failed and successful login is registered in the audit.
- [x] Adam's unit and integration tests are in `server/src/test/java/` and `common/src/test/java/` and pass.
      **The sub-issue is not done before this.**
- [ ] Code reviewed by a member, in the pull request.

## Status
- **In progress:** nothing
- **Next step:** Javier adds the test of the fix in `save` to `InMemoryUserAccountRepositoryTest`. Then open the pull
  request to `develop` with `Closes #13` when the member asks (rule 8), for a member's review.
- **Open questions:** none for the implementation. For #12 *UC22 – Client (module v1.0.0)*: who creates
  `AuthenticationService`, because `client` only depends on `common`.
- **Last update:** 01/11/2026, Javier
