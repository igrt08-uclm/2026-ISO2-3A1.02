# Plan – UC19 – Client (#14)

| | |
|---|---|
| **Sub-issue** | #14 *UC19 – Client (module v1.1.0)* · branch `feature/UC19-client`, from `feature/UC19-server` |
| **Use case** | #11 *UC19 – Audit system activity (module v1.1.0)* · FR19 · `client` `1.1.0` |
| **Specification** | #11, #14 and the wiki page *Requirements and Use Cases* (FR19) |
| **Design** | `doc/design/C1-client-design.png` · contracts: section 6 of #8 *C1. Access and security* and `IAuditService` in `common` |
| **Usability guide** | IPO I: *Heuristicas_Usabilidad* (Nielsen, Shneiderman), *Tema2_FactorHumano_1* (Gestalt), *Tema3_2_Principios_Estandares_Guias* |
| **Plan approved by** | Javier, 01/11/2026 |

## Questions and answers
Answered by Javier on 01/11/2026. The decisions of the server are in `doc/plans/UC19-server.md`.
- **Q:** How does the administrator reach the audit log? **A:** With a *View audit log* button in the welcome screen, shown only to
  administrators. The audit screen has *Back*. The server checks the user anyway.
- **Q:** How are the filters entered? **A:** *User* and *Patient or resource* are text, *Action* is a list with the known actions, and
  *From* and *To* are text as `yyyy-MM-dd`. The text fields do not accept `;`, which separates the filters.
- **Q:** What is shown on opening? **A:** Every entry, in a table with *Date*, *User*, *Action* and *Resource*, the newest first.
  *Search* applies the filters and *Clear* shows everything again. Each load is registered as a consultation.
- **Q:** GUI technology and language? **A:** Swing and English, as in #12 *UC22 – Client (module v1.0.0)*.
- **Q:** `AuditRecorderSimulator`? **A:** It is deleted here, when `Main` uses `AuditService`.
- **Q:** Who writes the tests? **A:** The agent, in Adam's style, after Javier approves their design (stage 4).

## Screens (low-fidelity prototype)
Welcome, for an administrator:
```
+---------------------------------------------------+
|  Welcome, Laura Sánchez Ruiz                      |
|                                                   |
|  You are logged in as: System administrator       |
|                                                   |
|  The functions of your role will appear here.     |
|                                                   |
|                [ View audit log ]    [ Log out ]  |
+---------------------------------------------------+
```

Audit log:
```
+-----------------------------------------------------------------------+
|  Audit log                                                            |
|                                                                       |
|  User                Action              Patient or resource          |
|  [_____________]     [ All actions  v ]  [____________________]       |
|                                                                       |
|  From (yyyy-MM-dd)   To (yyyy-MM-dd)                                  |
|  [_____________]     [_____________]           [ Clear ]  [ Search ]  |
|                                                                       |
|  Date                 User      Action         Resource               |
|  2026-11-01 22:10:58  admin     LOGIN_OK       authentication         |
|  2026-11-01 22:09:12  nobody    LOGIN_FAILED   authentication:UNKN... |
|                                                                       |
|  2 entries                                                            |
|                                                             [ Back ]  |
+-----------------------------------------------------------------------+
```

Messages:
| Situation | Message |
|---|---|
| Entries found | `n entries` |
| No entries | No activity matches these filters. |
| Wrong date | Check the dates: use the format yyyy-MM-dd. |
| `AccessDeniedException` | You do not have permission to consult the audit log. |

## Usability decisions
| Principle (IPO I) | Decision |
|---|---|
| Visibility of the system status, feedback | The number of entries after each search, and a message if there are none |
| User's language | *User* and *Patient or resource* instead of `author` and `target`; plain messages |
| Control and freedom | *Back* and *Clear* are always available |
| Consistency and standards | Same look, positions and main button as the login; the table shows dates as in the filters |
| Error prevention | The list of actions avoids typing them, the fields do not accept `;`, and the option is hidden from non-administrators |
| Recognition rather than recall | The labels show the date format, and the filters stay after a search |
| Flexibility and shortcuts | Enter searches, and Alt+S, Alt+C and Alt+B run the buttons |
| Minimalist design | Only the filters, the table and three buttons |
| Recognise and recover from errors | The message says what to fix, and the table does not change after a wrong date |
| Gestalt: proximity | Each label is right above its field, the filters form one group, and the results another |
| Colour and readability | Errors have an icon and text, as in the login |

## Files
| File | Action | What |
|---|---|---|
| `client/src/main/java/es/uclm/esi/iso2/client/access/presentation/IU_Audit.java` | create | `JPanel` with the filters and the table; `applyFilter(filters)` and `displayActivity(logs)` as in the diagram; receives `IAuditService`, the logged user and the action for *Back* |
| `client/src/main/java/es/uclm/esi/iso2/client/access/presentation/IU_Welcome.java` | change | *View audit log*, only for `SYSTEM_ADMINISTRATOR`; the constructor used by the tests stays |
| `client/src/main/java/es/uclm/esi/iso2/client/Main.java` | change | one `AuditService`, shared with `AuthenticationService`, and the change to the audit screen |
| `server/src/main/java/es/uclm/esi/iso2/server/access/service/AuditRecorderSimulator.java` | delete | `AuditService` replaces it (D-023) |
| `client/pom.xml` | change | version `1.1.0-SNAPSHOT` |
| `client/src/test/java/…/presentation/IU_AuditTest.java`, `FakeAuditService.java` and `IU_WelcomeTest.java` | create, change | stage 4 |

`IU_Audit` only imports `common` and Swing, never `server`, like the other screens.

## Test cases (design of the tests)
Approved by Javier on 01/11/2026 (step 9). Same style as Adam's tests: `@DisplayName`, *Arrange, Act, Assert*, `@ParameterizedTest` for the
similar cases, hand-written test doubles (`FakeAuditService`), and the controls found with `getComponents()`. The panels are only
built, never shown, so the tests run in the CI, which has no display.

| Class | Case | Expected result |
|---|---|---|
| `IU_AuditTest` | Open the screen | Asks for every entry with the logged user, and shows them in the table |
| `IU_AuditTest` | An entry in the table | Date as `yyyy-MM-dd HH:mm:ss`, user, action and resource, in that order |
| `IU_AuditTest` | 0, 1 and several entries | `No activity matches these filters.`, `1 entry` and `n entries` |
| `IU_AuditTest` | Fill in each filter and press *Search* | Sends its `key=value` pair |
| `IU_AuditTest` | Fill in every filter | Sends all the pairs, separated by `;` |
| `IU_AuditTest` | Spaces around the values, and fields with only spaces | The values are trimmed, and the empty ones are not sent |
| `IU_AuditTest` | *All actions* | The action is not sent |
| `IU_AuditTest` | Press Enter in a text field | Searches like *Search* |
| `IU_AuditTest` | Type or paste `;` in a text field | It does not appear |
| `IU_AuditTest` | Press *Clear* | Empties the filters, selects *All actions* and shows everything |
| `IU_AuditTest` | Wrong date | Shows the message, and the table does not change |
| `IU_AuditTest` | After an error, a correct search | The message disappears |
| `IU_AuditTest` | `AccessDeniedException` | Shows the message and empties the table |
| `IU_AuditTest` | Edit a cell | It cannot be edited |
| `IU_AuditTest` | Press *Back* | Runs the action for going back |
| `IU_WelcomeTest` | An administrator | Sees *View audit log*, which runs its action |
| `IU_WelcomeTest` | A user who is not an administrator | Does not see it |
| `IU_WelcomeTest` | The constructor without the action | Does not see it |

## Order of the work
This plan covers stages 2 and 4 of `doc/plans/UC19-server.md`. Stage 3, the tests of the server, is done on `feature/UC19-server`,
and it is merged here before stage 4.

## Steps
One step, one commit, one at a time. Tick each step with the hash of its commit (green `mvn -B verify`).

**Stage 2: implementation of the client**
- [x] 1. This plan. `49e0492`
- [x] 2. `IU_Audit`: filters, table and layout. `ff486b7`
- [x] 3. `IU_Audit`: search, messages and shortcuts. `c2410c1`
- [x] 4. `IU_Welcome`: *View audit log*, only for administrators. `8606152`
- [x] 5. `Main`: `AuditService`, the audit screen, and `AuditRecorderSimulator` deleted. `e568917`
- [x] 6. `client/pom.xml`: version `1.1.0-SNAPSHOT`. `586118b`
- [x] 7. Javier checks the application by hand, and the usability decisions are ticked. Result on 01/11/2026: everything worked
      as expected: the button only for administrators, the search, *Clear*, *Back*, and a wrong date keeps the table and shows
      its message.

**Stage 4: tests of the client** (after stage 3)
- [x] 8. `git merge` of `feature/UC19-server`. `796f093`
- [x] 9. Design of the tests: Javier approves the test cases, written here. `606da98`
- [x] 10. Tests of `IU_Audit` and `IU_Welcome`. `ff2d229`

## How to run it
`mvn -B package -DskipTests`, and then, on Windows:
`java -cp "common/target/classes;server/target/classes;client/target/classes" es.uclm.esi.iso2.client.Main`

## Verification
- [x] `mvn -B verify` is green.
- [x] The tests were checked to fail when the code is wrong: 12 errors were introduced on purpose in a temporary copy (filters,
      buttons, messages and the check of the role), and all of them were detected.
- [x] The system administrator can check the registry and filter it by who, what, when and which patient (#14).
- [x] A user who is not an administrator does not see the option, and the server rejects the request (#15).
- [x] `IU_Audit` and `IU_Welcome` only import `common` and Swing, never `server`.
- [x] Each row of *Usability decisions* is checked in the running application.
- [ ] Code reviewed by a member, in the pull request.

## Status
- **In progress:** nothing
- **Next step:** after #15 *UC19 – Server (module v1.1.0)* is merged, open the pull request to `develop` with `Closes #14` when the
  member asks (rule 8).
- **Open questions:**
  - Design (Marcos and Denis): the answer about the access check, in #15 *UC19 – Server (module v1.1.0)*.
  - The audit log is in memory, so it is lost on closing. Persistence is discussed in Meeting 6.
- **Last update:** 01/11/2026, Javier
