# Plan – UC18 – Client (#16)

| | |
|---|---|
| **Sub-issue** | #16 *UC18 – Client (module v1.2.0)* · branch `feature/UC18-client`, created by Javier from #16 with `feature/UC18-server` as its source |
| **Use case** | #10 *UC18 – Manage roles and permissions (module v1.2.0)* · FR18 · `client` `1.2.0` |
| **Specification** | #10, #16 and the wiki page *Requirements and Use Cases* (FR18) |
| **Design** | `doc/design/C1-client-design.png` (`IU_ManageRoles`) · contracts: section 6 of #8 *C1. Access and security* and `IRoleManagementService` in `common` · analysis: D15 |
| **Usability guide** | IPO I: *Heuristicas_Usabilidad* (Nielsen, Shneiderman), *Tema2_FactorHumano_1* (Gestalt), *Tema3_2_Principios_Estandares_Guias* |
| **Plan approved by** | Javier, 06/11/2026 |

## Questions and answers
Answered by Javier on 02/11/2026 and 06/11/2026. The decisions of the server are in `doc/plans/UC18-server.md`.
- **Q:** Branch? **A:** `feature/UC18-client` from `feature/UC18-server`, because the server of UC18 is not on `develop` yet. The pull
  requests go in the same order as in UC19: first the server.
- **Q:** How is the screen organised? **A:** Two tabs, one per operation, as the branches *a* and *b* of the communication diagram
  (D15): *User roles* and *Role permissions*. It follows the minimalist design, the grouping of Gestalt and the lower memory load of
  the IPO notes, which do not mention tabs as such.
- **Q:** How are the user and the role chosen? **A:** From lists, with `getUsers()`, `getAvailableRoles()` and
  `getAvailablePermissions()` (#16).
- **Q:** Who sees *Manage roles* in the welcome screen? **A:** Whoever has the permission `MANAGE_ROLES`. The server checks it anyway.
- **Q:** Does UC18 change UC19? **A:** No (Javier, 06/11/2026). *View audit log* and the audit screen stay as they are. `IU_Welcome`
  and `Main` are shared by every use case, so UC18 only adds its own button and its screen to them, without changing what is there.
- **Q:** Confirmation? **A:** Always: assigning and revoking a role or a permission asks *OK* or *Cancel* first.
- **Q:** How is the result shown? **A:** In a message in the screen itself, as in the login and the audit log: green if it works, red
  with an icon if it fails. The lists are updated at once.
- **Q:** GUI technology and language? **A:** Swing and English, as in UC22 and UC19.
- **Q:** Who writes the tests? **A:** The agent, in Adam's style, after Javier approves their design (stage 4).
- **Q:** How does the welcome screen know whether the user has `MANAGE_ROLES`? **A:** The roles of the logged user do not
  carry their permissions, because they are kept in `IRoleRepository`. `Main` reads them with `getPermissions(username)` and
  passes them to a new constructor of `IU_Welcome`; the other constructors do not change (step 6).
- **Q:** Where are the error messages? **A:** In step 3 instead of step 5, because `assignRole` throws
  `AccessDeniedException` and the code does not compile without handling it. Step 5 added the rest.

## Screens (low-fidelity prototype)
Tab *User roles*:
```
+-----------------------------------------------------------------+
|  Manage roles and permissions                                   |
|  [ User roles ]  Role permissions                               |
|                                                                 |
|  User                         Roles of the user                 |
|  [ mgarcia - Miguel G... v]   +-----------------------------+   |
|                               | Physician                   |   |
|  Role to assign               | Patient                     |   |
|  [ Admissions staff     v]    +-----------------------------+   |
|  [ Assign role ]                         [ Revoke selected ]    |
|                                                                 |
|  ✔ Role Admissions staff assigned to mgarcia.                   |
|                                                       [ Back ]  |
+-----------------------------------------------------------------+
```
Tab *Role permissions* is the same, with *Role*, *Permissions of the role* and *Permission to assign*.

Confirmation:
```
+----------------------------------------------------+
| Confirm                                            |
|  Revoke the role Patient from mgarcia?             |
|                              [ OK ]  [ Cancel ]    |
+----------------------------------------------------+
```

Messages:
| Situation | Message |
|---|---|
| Assigned | ✔ Role *name* assigned to *user*. / ✔ Permission *code* assigned to *role*. |
| Revoked | ✔ Role *name* revoked from *user*. / ✔ Permission *code* revoked from *role*. |
| Already had it, or did not have it | Nothing changed: *user* already has that role. (and the other three cases) |
| `AccessDeniedException` | ⚠ You do not have permission to manage roles. |
| `IllegalStateException` | ⚠ This change would leave the system without an active administrator. |
| `IllegalArgumentException` | ⚠ That user, role or permission no longer exists. Reload the screen. |

## Usability decisions
| Principle (IPO I) | Decision |
|---|---|
| Visibility of the system status, feedback | A message after each change, and the lists show the new state at once |
| User's language | Names of roles and full names of users, not ids; plain messages |
| Control and freedom | *Cancel* in every confirmation, *Back* always available, and every change can be undone with the opposite button |
| Consistency and standards | The two tabs work in the same way; same look, messages and colours as the login and the audit log |
| Error prevention | Lists instead of typing, a confirmation for every change, *Revoke selected* disabled until something is selected, and options hidden to users without `MANAGE_ROLES` |
| Recognition rather than recall | The current roles or permissions are always visible next to what is assigned |
| Flexibility and shortcuts | Alt+U and Alt+P change tab, and Alt+A, Alt+R and Alt+B run the buttons |
| Minimalist design | One operation per tab, with only its lists and buttons |
| Recognise and recover from errors | The message says what happened and what to do |
| Gestalt: proximity | The selection on the left and the current state on the right, each label right above its control |
| Colour and readability | Errors and successes have an icon and text, not only colour |

Every row was checked by Javier in the running application on 06/11/2026.

## Files
| File | Action | What |
|---|---|---|
| `client/src/main/java/es/uclm/esi/iso2/client/access/presentation/IU_ManageRoles.java` | create | `JPanel` with the two tabs; `selectUser`, `selectRole`, `selectPermissions`, `displayRoles` and `showOperationResultSuccess`, as in the diagram; receives `IRoleManagementService`, the logged user, the action for *Back* and how to confirm |
| `client/src/main/java/es/uclm/esi/iso2/client/access/presentation/IU_Welcome.java` | change | adds *Manage roles*, for whoever has `MANAGE_ROLES`; *View audit log* does not change |
| `client/src/main/java/es/uclm/esi/iso2/client/Main.java` | change | `RoleManagementService` with the shared repositories, the confirmation window, and the change to the new screen |
| `client/pom.xml` | change | version `1.2.0-SNAPSHOT` |
| `client/src/test/java/…/presentation/IU_ManageRolesTest.java`, `FakeRoleManagementService.java` and `IU_WelcomeTest.java` | create, change | stage 4; the existing tests do not change |

`IU_ManageRoles` only imports `common` and Swing, never `server`. The confirmation is received in the constructor, so that the tests do
not open a window: `Main` passes a dialog with *OK* and *Cancel*, and the tests pass an answer.

## Test cases (design of the tests)
Approved by Javier on 06/11/2026 (step 11). Same style as Adam's tests: `@DisplayName`, *Arrange, Act,
Assert*, `@ParameterizedTest` for the similar cases, a hand-written test double (`FakeRoleManagementService`), and the
controls found through `getComponents()`. The panels are only built, never shown, so the tests run in the CI, which has
no display. The confirmation is a function that the test passes, so no window is opened.

| Class | Case | Expected result |
|---|---|---|
| `IU_ManageRolesTest` | Open the screen | The users and the roles come from the service in its order, the first user and the first role are selected, and their roles and permissions are shown |
| `IU_ManageRolesTest` | The texts of the lists | `username - full name` for the users, the name for the roles, and `CODE - description` for the permissions, not the ids |
| `IU_ManageRolesTest` | Select another user; another role | Its roles; its permissions (`selectUser`, `displayRoles`, `selectRole`, `selectPermissions`) |
| `IU_ManageRolesTest` | Nothing selected in a list; something selected | *Revoke selected* is disabled; it is enabled |
| `IU_ManageRolesTest` | Assign and revoke a role, and a permission, with *OK* | The service receives the logged user and the selected values, the list shows the new state, the same user or role stays selected, and the message is green with ✔ (`showOperationResultSuccess`) |
| `IU_ManageRolesTest` | The question of each of the four operations | `Assign the role X to user?`, `Revoke the role X from user?`, and the same with the permissions |
| `IU_ManageRolesTest` | *Cancel*, in the four operations | The service is not called, and there is no message |
| `IU_ManageRolesTest` | Assign something already held; revoke something no longer held | No question, the service is not called, and `Nothing changed: …` |
| `IU_ManageRolesTest` | `AccessDeniedException`, `IllegalStateException`, `IllegalArgumentException` | The red message of the table *Messages*, with ⚠, and the lists show the state of the server |
| `IU_ManageRolesTest` | After an error, a change that works | The green message replaces the red one |
| `IU_ManageRolesTest` | Press *Back* | Runs the action for going back |
| `IU_ManageRolesTest` | The shortcuts | Alt+U and Alt+P on the tabs, and Alt+A, Alt+R and Alt+B on the buttons |
| `IU_WelcomeTest` | A user with `MANAGE_ROLES` | Sees *Manage roles*, which runs its action; an administrator sees *View audit log*, *Manage roles* and *Log out*, in that order |
| `IU_WelcomeTest` | A user without `MANAGE_ROLES` | Does not see it |
| `IU_WelcomeTest` | Without the action | Not even a user with `MANAGE_ROLES` sees it |
| `IU_WelcomeTest` | The constructors of UC22 and UC19 | Do not show it |

New files, in `client/src/test/java/…/access/presentation`: `IU_ManageRolesTest` and `FakeRoleManagementService`.
`IU_WelcomeTest` gets the new tests; the existing ones do not change.

## Order of the work
This plan covers stages 2 and 4 of `doc/plans/UC18-server.md`. Step 11 of the server goes first, as this screen needs it.

## Steps
One step, one commit, one at a time. Tick each step with the hash of its commit (green `mvn -B verify`).

**Stage 2: implementation of the client**
- [x] 1. This plan. `7c47a58`
- [x] 2. `IU_ManageRoles`: the two tabs, their lists and buttons, and the layout. `adb5f88`
- [x] 3. `IU_ManageRoles`: tab *User roles*: load the lists, assign, revoke and refresh. `8c7caa3`
- [x] 4. `IU_ManageRoles`: tab *Role permissions*: the same with the permissions. `3a4efba`
- [x] 5. `IU_ManageRoles`: confirmation, messages and shortcuts. `2c7c24f`
- [x] 6. `IU_Welcome`: *Manage roles*, only for whoever has `MANAGE_ROLES`. `bd5fea9`
- [x] 7. `Main`: `RoleManagementService`, the shared repositories and the new screen. `2eaa585`
- [x] 8. `client/pom.xml`: version `1.2.0-SNAPSHOT`. `bc6e7ab`
- [x] 9. Javier checks the application by hand, and the usability decisions are ticked. 06/11/2026, Javier

**Stage 4: tests of the client** (after stage 3)
- [x] 10. `git merge` of `feature/UC18-server`. `a9baa9f`
- [x] 11. Design of the tests: Javier approves the test cases, written here. `3e9f332`
- [x] 12. Tests of `IU_ManageRoles`, with `FakeRoleManagementService`. `b91e6b2`
- [x] 13. Tests of `IU_Welcome`, and the check that the tests fail when the code is wrong. Commit *test(UC18): tests of Manage roles in the welcome screen (#16)*

## How to run it
`mvn -B package -DskipTests`, and then, on Windows:
`java -cp "common/target/classes;server/target/classes;client/target/classes" es.uclm.esi.iso2.client.Main`

## Verification
- [x] `mvn -B verify` is green.
- [x] The system administrator can assign and revoke roles and permissions, and the changes take effect at once (#16).
- [x] A user without `MANAGE_ROLES` does not see the option, and the server rejects the request.
- [x] `IU_ManageRoles` and `IU_Welcome` only import `common` and Swing, never `server`.
- [x] Each row of *Usability decisions* is checked in the running application.
- [x] The tests were checked to fail when the code is wrong: 13 errors were introduced on purpose in a temporary copy,
      one at a time (no confirmation, the colour of the success message, no refresh, the selected user not kept,
      *Revoke selected* enabled at the start, a role already held sent again, another requester, the message of the last
      administrator, the user shown by its id, a missing shortcut, the list of permissions not emptied, and *Manage roles*
      without the permission or without the action), and all of them were detected.
- [ ] Code reviewed by a member, in the pull request.

## Status
- **In progress:** nothing
- **Next step:** the pull request of `feature/UC18-server` (#17) first; this one (#16) is a draft until it is merged, and then `develop` is merged here
- **Open questions:** Design (Marcos and Denis) updates the diagrams with `requester`, the lists and the confirmation.
- **Last update:** 06/11/2026, Javier: stage 4 done
