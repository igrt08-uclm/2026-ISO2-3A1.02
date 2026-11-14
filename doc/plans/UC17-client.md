# Plan – UC17 – Client (#39)

| | |
|---|---|
| **Sub-issue** | #39 *UC17 – Client (C2 v1.0.0)* · branch `feature/UC17-client` (it already contains `feature/UC17-server`) |
| **Use case** | #38 *UC17 – Configure system parameters and exam catalogue (C2 v1.0.0)* · FR17 · component C2 1.0.0 |
| **Specification** | use case issue #38, sub-issue #39 and the wiki page *Requirements and Use Cases* |
| **Design** | `doc/design/C2-client-design.png` (`IU_ConfigureParameters`) · contracts: section 6 of #33 *C2. Configuration and integrations*, as changed in `doc/plans/UC17-server.md` |
| **Usability guide** | IPO I, as in `doc/plans/UC22-client.md` |
| **Plan approved by** | Javier, 14/11/2026 |

## Decisions already taken (comments in #39 and #40, 13–14/11/2026)
- The screen decisions are taken by Implementation (Javier; Marcos agreed): one tab per group, a button *Configure system* in the welcome screen only with the permission `CONFIGURE_SYSTEM`, the priority chosen from a list, `id` and group not editable after creation, deactivated entries greyed with the word "inactive".
- `requester` is the username (`String`). Every operation of `ISystemParameterService` needs `CONFIGURE_SYSTEM`, also the reads.
- The server (#40) is done: contract in `common`, `SystemParameterService`, seed data and the four `deactivate*` operations.
- The tests are Adam's (Testing): no step here writes them; the last step hands the branch over.

## Questions and answers
Open points that #39, the design and the contracts do not settle; each one has a proposal.
- **Q1:** Where does `Main` live, if it must create `SystemParameterService` (it needs `server-c2-configuration`) and show a screen of `client-c2-configuration`? Today `Main` is in `client/c1-access`. **Proposal:** it stays there and `client-c1-access` gets two new dependencies (`client-c2-configuration` and `server-c2-configuration`). It is the smallest change, and `Main` is still the only class that imports `server`. The drawback: the module of C1 depends on the module of C2, and `agents/architecture.md` says that a component does not depend on another one. **Alternative:** a new module for `Main` (more files and poms). This is an architecture decision (rule 1): do you want to ask Ignacio, or do you decide? **A:** approved as proposed: `Main` stays in `client/c1-access` (Javier, 14/11/2026). Nothing is published to Ignacio unless Javier asks for it.
- **Q2:** Version of `client-c1-access`: it changes `IU_Welcome` (one button) and `Main`. **Proposal:** `1.2.1-SNAPSHOT` (PATCH, as the server of C1 in #109). The confirmation at log out of Meeting 8 would be another change in the same version. **A:** approved as proposed (Javier, 14/11/2026)
- **Q3:** New client module. **Proposal:** `client/c2-configuration` with `artifactId` `client-c2-configuration`, version `1.0.0-SNAPSHOT` (D-032), depending on `common` and JUnit only; package `es.uclm.esi.iso2.client.configuration.presentation`. Changes of poms need your approval (rule 9): `pom.xml` (managed versions of the two new artifacts), `client/pom.xml` (the module), the new `pom.xml` and the one of `client/c1-access`. **A:** approved as proposed, including the changes of poms (Javier, 14/11/2026)
- **Q4:** Classes of the screen. **Proposal:** `IU_ConfigureParameters` (the `JPanel` of the design, with the five tabs) plus three package-private helper panels so that it does not become a huge class: `ParameterGroupPanel` (generic for protocols, appointment policies and priorities, which have the same fields: id, name, description), `TestTypePanel` and `SchedulePanel`. The helpers are not in the design: they are only Swing internals; if you prefer one single class, say so. **A:** approved as proposed (Javier, 14/11/2026)
- **Q5:** Deactivated entries. **Proposal:** they are listed greyed with "inactive" and can be selected to read them, but they cannot be edited nor deactivated again (the buttons are disabled). There is no *Reactivate*: it is not in #38. If you want it, `save*` and `updateTestType` already accept `active = true`, so it would only be one more button. **A:** approved as proposed (Javier, 14/11/2026)
- **Q6:** Validation in the client. **Proposal:** the screen only checks what it needs to build the object: the duration must be a whole number and the times must be `HH:mm`; if not, it shows the error and does not call the server. All the other rules (required fields, duration > 0, end after start, duplicated id) are the server's, and the screen shows **every** error of the `ValidationException` together, in red. This avoids repeating the rules in two places. **A:** approved as proposed (Javier, 14/11/2026)
- **Q7:** The *Schedule* has no id, it is identified by its resource (the room). **Proposal:** a text field *Room or resource* (not editable when editing an existing one), seven checkboxes for the days (Monday to Sunday) and two text fields for the times (`HH:mm`). **A:** approved as proposed (Javier, 14/11/2026)
- **Q8:** The *Room* of a test type is free text in the contract (e.g. `Room CT-1`). **Proposal:** a text field as well, without checking that a schedule exists for that room (the server does not check it either). **A:** approved as proposed (Javier, 14/11/2026)
- **Q9:** The tabs and the lists after a change. **Proposal:** after every create, edit or deactivate the table of that group is read again from the server and the same row stays selected; the priorities list of the test-type form is read again when the screen opens and after a change in the *Priorities* tab. **A:** approved as proposed (Javier, 14/11/2026)
- **Q10:** `AccessDeniedException` while on the screen (for example the permission was revoked meanwhile). **Proposal:** the message "You do not have permission to configure the system." and every button is disabled; the user goes back with *Back*. **A:** approved as proposed, after Javier asked what it meant: only *Back* stays enabled (Javier, 14/11/2026)
- **Note:** the diagram of Design (`C2-client-design.png`) still shows `SystemParameter`, `ParameterGroup` and `getParameters(group)`. The code follows the contracts of #33 as changed in #40 (typed classes and one operation per group). The methods `displayGroup`, `onCreateParameter`, `onEditParameter`, `onDeactivateParameter`, `confirmDeactivation`, `showOperationResultSuccess` and `showOperationResultError` of the diagram are kept with these names where they make sense.

## Screen (low-fidelity prototype)
```
+------------------------------------------------------------------------------+
| Radiodiagnostics System - Configure system                               _ X |
+------------------------------------------------------------------------------+
|  Configure system parameters                                                 |
|  [Test types] [Protocols] [Schedules] [Appointment policies] [Priorities]    |
| +--------------------------------------+-----------------------------------+ |
| | Id            Name            State  | Id        [CT-HEAD (no edit)]     | |
| | XRAY-CHEST    Chest X-ray     active | Name      [Head CT              ] | |
| | CT-HEAD     > Head CT         active | Modality  [CT                   ] | |
| | MRI-KNEE      Knee MRI        active | Room      [Room CT-1            ] | |
| | OLD-TEST      Old test (inactive)    | Duration (minutes) [20]           | |
| |                                      | Preparation [..................] | |
| |                                      | [ ] Requires contrast             | |
| |                                      | [ ] Requires consent              | |
| |                                      | Safety information [............] | |
| |                                      | Priority  [Urgent            v]   | |
| |                                      | [ ] Eligible for follow-up        | |
| | [ New ]                              | [ Save ]        [ Deactivate ]    | |
| +--------------------------------------+-----------------------------------+ |
|  (!) The duration must be greater than zero. The name is required.           |
|                                                                   [ Back ]   |
+------------------------------------------------------------------------------+
```
Welcome screen: a new button *Configure system* (Alt+C) between *Manage roles* and *Log out*, visible only with `CONFIGURE_SYSTEM`.

## Usability decisions
| Principle (IPO I) | Decision |
|---|---|
| Visibility of the system status | A message area under the tabs says what happened ("✔ Test type CT-HEAD saved.") or what failed ("⚠ …"), as in *Manage roles* |
| User's language | Plain messages; the list of priorities shows the name ("Urgent"), not the id |
| Control and freedom | *New* clears the form; *Back* returns to the welcome screen; nothing is deleted, only deactivated |
| Consistency and standards | Same tabs, message area, colours, icons and *Back* button as *Manage roles*; main buttons at the bottom right of the form |
| Error prevention | The id is not editable after creation; *Save* and *Deactivate* are disabled when they do not apply (nothing selected, inactive entry); confirmation before deactivating |
| Recognition rather than recall | The table shows the existing entries; the labels are always visible; the priority is chosen from a list |
| Flexibility and shortcuts | Alt+N (New), Alt+S (Save), Alt+D (Deactivate), Alt+B (Back), Alt+letter for the tabs; Tab moves between fields |
| Minimalist design | Each tab only has the fields of its group |
| Recognise and recover from errors | All the errors of one save appear together, in red, with an icon, and the form keeps what the user typed |
| Gestalt: proximity and similarity | Table on the left and form on the right; each label right above its field; the five tabs have the same layout |
| Colour and readability | Inactive entries are grey **and** have the word "inactive"; errors have icon and text, not only colour |

## Files
| File | Action | What |
|---|---|---|
| `pom.xml` | change | managed versions of `server-c2-configuration` `1.0.0-SNAPSHOT` and `client-c2-configuration` `1.0.0-SNAPSHOT` (Q1, Q3) |
| `client/pom.xml` | change | module `c2-configuration` |
| `client/c2-configuration/pom.xml` | create | `client-c2-configuration` 1.0.0-SNAPSHOT, depends on `common` and JUnit only |
| `client/c2-configuration/src/main/java/es/uclm/esi/iso2/client/configuration/presentation/IU_ConfigureParameters.java` | create | the `JPanel` with the five tabs, the message area and *Back*; receives `ISystemParameterService`, the requester, a `Predicate<String>` to confirm and the action of *Back* (as `IU_ManageRoles`) |
| `…/presentation/ParameterGroupPanel.java` | create | generic tab for protocols, appointment policies and priorities (Q4) |
| `…/presentation/TestTypePanel.java` | create | tab of the test types, with the list of priorities (Q9) |
| `…/presentation/SchedulePanel.java` | create | tab of the schedules (Q7) |
| `client/c1-access/src/main/java/es/uclm/esi/iso2/client/access/presentation/IU_Welcome.java` | change | one more constructor with `Runnable onConfigureSystem` and the button *Configure system*; the existing constructors stay, so the tests of C1 do not change |
| `client/c1-access/src/main/java/es/uclm/esi/iso2/client/Main.java` | change | creates `SystemParameterService` (with the role service and the audit service of C1), calls `loadSeedData()`, and shows the new screen (Q1) |
| `client/c1-access/pom.xml` | change | version `1.2.1-SNAPSHOT` (Q2) and the two new dependencies (Q1) |
| `agents/architecture.md` | change | the code tree with `client/c2-configuration` and the dependency of `client-c1-access` |
| `doc/plans/UC17-client.md` | create/update | this plan |

The screens are `JPanel`, so that Adam's tests can create them without a display. They only import `common` and Swing, never `server`. Every class and public method has its Javadoc in English (rule 18).

## Steps
Tick each step with the hash of its commit (green `mvn -B verify -Djacoco.skip=true` until Adam's tests exist, as in #40).
- [x] 1. This plan (`docs(UC17): plan for the client part (#39)`). – 738f761.
- [x] 2. Poms: new module `client/c2-configuration`, managed versions, dependencies and version of `client-c1-access` (Q1, Q2, Q3). The build stays green with an empty module. – a25e67e.
- [x] 3. `ParameterGroupPanel`: table, form (id, name, description), *New*, *Save*, *Deactivate*, inactive entries in grey, errors together. – fab0a74.
- [x] 4. `IU_ConfigureParameters`: the window with the tabs for protocols, appointment policies and priorities, the message area, the confirmation and *Back*. – 1bf325c.
- [ ] 5. `TestTypePanel`: all the fields of FR17, the list of active priorities and the client check of the duration (Q6, Q9).
- [ ] 6. `SchedulePanel`: room, days of the week, times and their client check (Q6, Q7).
- [ ] 7. `IU_Welcome` (button with the permission) and `Main` (service, seed data, navigation).
- [ ] 8. `agents/architecture.md`.
- [ ] 9. Javier checks by hand with `admin` and with `mgarcia` (no button) and the usability decisions of the table above; the result is written here.
- [ ] 10. Hand the branch over to Adam (tests of UC17-client, with a fake `ISystemParameterService`); the sub-issue is closed only with his tests green and the coverage check passing.

## How to run it
`mvn -B install -DskipTests` and then `mvn -B -pl client/c1-access exec:java -Dexec.mainClass=es.uclm.esi.iso2.client.Main`.

## Verification
- [ ] Every acceptance criterion of #39 is checked: the administrator lists, creates, edits and deactivates the entries of the five groups from the screen; invalid data is rejected with an error message; the screen is only available with `CONFIGURE_SYSTEM`; interfaces match the contracts.
- [ ] `IU_ConfigureParameters` and its panels only import `common` and Swing.
- [ ] No class of C1 changes except `IU_Welcome`, `Main` and the poms.
- [ ] Each row of *Usability decisions* is checked in the running application.
- [ ] `mvn -B verify` is green with Adam's tests (the coverage check included).
- [ ] The CI of the pull request is green; code reviewed by a member.

## Status
- **In progress:** nothing
- **Next step:** step 5 (`TestTypePanel` in `client/c2-configuration`, added as the first tab of `IU_ConfigureParameters`)
- **Open questions:** none
- **Last update:** 14/11/2026, Javier
