# Plan – UC22 – Client (#12)

| | |
|---|---|
| **Sub-issue** | #12 *UC22 – Client (module v1.0.0)* · branch `feature/UC22-client`, to be created by a member from #12, with `feature/UC22-server` as its source |
| **Use case** | #9 *UC22 – Log in (module v1.0.0)* · FR22 · `client` `1.0.0` |
| **Specification** | #9, #12 and the wiki page *Requirements and Use Cases* (FR22) |
| **Design** | `doc/design/C1-client-design.png`, `doc/design/UC22-login-communication.png` · contracts: section 6 of #8 *C1. Access and security* |
| **Usability guide** | IPO I: *Heuristicas_Usabilidad* (Nielsen, Shneiderman), *Tema2_FactorHumano_1* (Gestalt), *Tema3_1_Metodos_Prototipado*, *Tema3_2_Principios_Estandares_Guias* |
| **Plan approved by** | Javier, 31/10/2026 |

## Questions and answers
Answered by Javier on 31/10/2026.
- **Q:** Branch? **A:** `feature/UC22-client` from `feature/UC22-server`, because the classes of `common` are not on `develop`
  yet. Its pull request is opened when #13 *UC22 – Server (module v1.0.0)* is merged.
- **Q:** GUI technology? **A:** Swing for now, as it comes with Java 21 and adds no dependency (rule 6). The final
  technology is a pending decision for Meeting 6.
- **Q:** Who creates `AuthenticationService`, if `client` only depends on `common`? **A:** `client` depends on `server`,
  and a `Main` class creates the service. `Main` is the only client class that imports `server`: the screens only use
  `IAuthenticationService` from `common`.
- **Q:** What is shown after a successful login? **A:** A welcome screen with the name and the roles, and a button to log
  out. The interface of each role comes with its use cases.
- **Q:** Language of the GUI? **A:** English (D-016).
- **Q:** Error messages? **A:** One message per reason.
- **Q:** Who writes the tests? **A:** Adam, after the implementation and on this branch, as in #13.
- **Q:** Recover password? **A:** Out of UC22 in Iteration 1 (David in #13, D01 of the analysis); Design removed
  `clickRecoverPassword()` from `IU_Login`. There is no link in the screen.

## Screens (low-fidelity prototype)
Login:
```
+---------------------------------------------------+
| Radiodiagnostics System - Log in              _ X |
+---------------------------------------------------+
|        Radiodiagnostics and Medical Imaging       |
|                 Management System                 |
|                                                   |
|  Log in with your corporate username and password |
|                                                   |
|  Username                                         |
|  [_____________________________________________]  |
|                                                   |
|  Password                                         |
|  [_____________________________________________]  |
|                                                   |
|  (!) The password is not correct. Try again.      |
|                                                   |
|                                       [ Log in ]  |
+---------------------------------------------------+
```

Welcome:
```
+---------------------------------------------------+
| Radiodiagnostics System                       _ X |
+---------------------------------------------------+
|  Welcome, Miguel García López                     |
|                                                   |
|  You are logged in as: Physician, Patient         |
|                                                   |
|  The functions of your role will appear here.     |
|                                                   |
|                                      [ Log out ]  |
+---------------------------------------------------+
```

Error messages:
| Reason | Message |
|---|---|
| `UNKNOWN_USER` | This username does not exist. Check it and try again. |
| `WRONG_PASSWORD` | The password is not correct. Try again. |
| `INACTIVE_USER` | This account is inactive. Contact the system administrator. |

## Usability decisions
| Principle (IPO I) | Decision |
|---|---|
| Visibility of the system status, feedback | The error appears in the screen itself; a successful login changes the screen and shows the user's name |
| User's language | Plain messages that say what to do; no reason codes or technical terms |
| Control and freedom | *Log out* goes back to the login; the window can be closed at any moment |
| Consistency and standards | Native look and feel of the operating system; usual positions and names; the main button at the bottom right |
| Error prevention | *Log in* is disabled until both fields have text, so empty fields never reach the server |
| Recognition rather than recall | Labels always visible; after an error, the username stays in its field |
| Flexibility and shortcuts | Enter logs in, Tab moves between fields, and Alt+U, Alt+P and Alt+L go to each control |
| Minimalist design | Only the fields and the button of the task, with a single message area |
| Recognise and recover from errors | After an error the focus goes to the field to fix: the username for `UNKNOWN_USER`, the password (emptied) for `WRONG_PASSWORD`; the message disappears when the user types |
| Help | One line under the title explains which credentials to use |
| Gestalt: proximity | Each label is right above its field, with more space between groups than inside them |
| Colour and readability | The error has an icon and text, not only colour; dark text on a light background |

## Files
| File | Action | What |
|---|---|---|
| `pom.xml` | change | `server` `1.0.0-SNAPSHOT` in `<dependencyManagement>` |
| `client/pom.xml` | change | dependency on `server` (compile scope, as `Main` creates the service; `runtime` would not compile) |
| `client/src/main/java/es/uclm/esi/iso2/client/access/presentation/IU_Login.java` | create | `JPanel` with `txtUsername`, `txtPassword`, `enterCredentials`, `showLoginError`, `showSuccess`; receives `IAuthenticationService` |
| `client/src/main/java/es/uclm/esi/iso2/client/access/presentation/IU_Welcome.java` | create | `JPanel` with the name, the roles and *Log out* |
| `client/src/main/java/es/uclm/esi/iso2/client/Main.java` | create | creates the service and the window, and changes between the screens |
| `agents/architecture.md` | change | `client` depends on `common` and `server` |

The screens are `JPanel`, not `JFrame`, so that Adam's tests can create them in the CI, which has no display. Only the
`IU_` classes and `Main` use Swing, so the GUI technology can change without touching `common` or `server`.

## Steps
Tick each step with the hash of its commit (green `mvn -B verify`).
- [x] 1. This plan. `55f2829`
- [x] 2. `pom.xml` and `client/pom.xml`: `client` depends on `server`. `1d2d722`
- [x] 3. `IU_Login`: fields, layout, shortcuts and *Log in* disabled until both fields have text. `d022ea6`
- [x] 4. `IU_Login`: `enterCredentials`, `showLoginError` with the messages, focus after an error, and `showSuccess`. `ff9646e`
- [x] 5. `IU_Welcome`: name, roles and *Log out*. `df043a5`
- [ ] 6. `Main`: native look and feel, window and change of screens.
- [ ] 7. `agents/architecture.md`.
- [ ] 8. Javier checks by hand the five users of #13 and the three errors, and the usability decisions are ticked; the
      result is written here.

## How to run it
`mvn -B package -DskipTests`, and then, on Windows:
`java -cp "common/target/classes;server/target/classes;client/target/classes" es.uclm.esi.iso2.client.Main`

## Verification
- [ ] `mvn -B verify` is green.
- [ ] After the user enters the credentials, the welcome screen with their roles or an error is shown (#12).
- [ ] `IU_Login` and `IU_Welcome` only import `common` and Swing, never `server`.
- [ ] Each row of *Usability decisions* is checked in the running application.
- [ ] Adam's tests are in `client/src/test/java/` and pass. **The sub-issue is not done before this.**
- [ ] Code reviewed by a member, in the pull request.

## Status
- **In progress:** step 6
- **Next step:** a member creates `feature/UC22-client`; then step 1
- **Open questions:**
  - Meeting 6: final GUI technology (Swing for now).
  - Design (Marcos and Denis): add `IU_Welcome`, `Main` and the dependency of `client` on `server` to the diagram.
  - Requirements: #12 still describes the recover password link; mark it as postponed.
- **Last update:** 31/10/2026, Javier
