# Plan – UC20 – Client (#42)

| | |
|---|---|
| **Sub-issue** | #42 *UC20 – Client (C2 v1.1.0)* · branch `feature/UC20-client` (it already contains `feature/UC20-server`, with the patch of C1 1.2.2 merged) |
| **Use case** | #41 *UC20 – Configure integrations with other hospital systems (C2 v1.1.0)* · FR20 · component C2 1.1.0 |
| **Specification** | use case issue #41, sub-issue #42 and the wiki page *Requirements and Use Cases* |
| **Design** | `doc/design/C2-client-design.png` (`IU_ManageIntegrations`) · contracts: `IIntegrationManagementService` (see `doc/plans/UC20-server.md`) · section 6 of #33 *C2. Configuration and integrations* |
| **Usability guide** | IPO I, as in `doc/plans/UC17-client.md` |
| **Plan approved by** | Javier, 15/11/2026 |

## Decisions already taken
- The screen decisions are taken by Implementation (Javier; Marcos agreed in #42): one screen with a table of the four integrations and, below it, the form of the selected one with *Save* and *Test connection*; a button *Configure integrations* in the welcome screen, only with the permission `CONFIGURE_INTEGRATIONS`; the word "SIMULATED" in the table and in the form, in a different colour.
- What a valid setting is (David, #42): the endpoint is required only in real mode and must start with `http://` or `https://`; in simulated mode it can be empty; the extra settings are `key=value` pairs, one per line, and every key needs a value. If something is invalid the screen shows the error and nothing is saved.
- The data is validated in the client and in the server (NFR03; Ignacio and Marcos in #46, and Javier on 15/11/2026): the screen uses the same rules as the server.
- The server (#43) is done: `IIntegrationManagementService` in `common` (`getIntegrations`, `getIntegration`, `configureIntegration`, which returns the saved configuration, and `testConnection`), `IntegrationManagementService`, the gateways and the seed data.
- The tests and the manual check of the screen are Adam's (Testing): no step here writes them; the last one hands the branch over.
- The button in `IU_Welcome` and the wiring in `Main` (client of C1) go in the pull request of this sub-issue, as in UC17 (Javier, 15/11/2026). Ignacio is asked to look at them in that pull request.

## Questions and answers
Each one has a proposal and all of them are answered.
- **Q1:** What the screen checks before it calls the server. **Proposal:** the same rules as the server: the endpoint is required in real mode and must start with `http://` or `https://` if it is not empty; each line of the settings is `key=value` with a key and a value that are not empty; all the errors are shown together and nothing is sent. The server still validates. **A:** approved (Javier, 15/11/2026)
- **Q2:** *Test connection* tests the **saved** configuration. **Proposal:** it is disabled while the form has unsaved changes, with the note "Unsaved changes: save or revert to test the connection"; it is enabled again after *Save* or *Revert*. **A:** approved (Javier, 15/11/2026)
- **Q3:** Confirmation. **Proposal:** the screen asks to accept or cancel before it saves a change that **disables** an integration or switches it to **real** mode ("Disable the integration with <system>?", "Switch <system> to real mode?"; one question if both change); any other save is not asked. **A:** approved (Javier, 15/11/2026)
- **Q4:** Client of C1. **Proposal:** the button and `Main` go in this pull request (see the decisions). **A:** approved (Javier, 15/11/2026)
- **Q5:** Module and versions. **Proposal:** `client-c2-configuration` goes to `1.1.0-SNAPSHOT` (UC20 = C2 1.1.0) in its `pom.xml` and in the managed version of `pom.xml`; `client-c1-access` goes from `1.2.1-SNAPSHOT` to `1.2.2-SNAPSHOT` (one more PATCH, because `IU_Welcome` and `Main` change again, as in UC17); `common` and the servers do not change. Changing a `pom.xml` needs the approval of Javier (rule 9). **A:** approved as proposed (Javier, 15/11/2026)
- **Q6:** Class. **Proposal:** one class `IU_ManageIntegrations` (a `JPanel`, with the name of the design) in `es.uclm.esi.iso2.client.configuration.presentation`, which receives the `IIntegrationManagementService`, the username of the requester and the action of *Back*, and asks to confirm through a `Predicate<String>` (as `IU_ConfigureParameters`, so that the tests of Adam do not open a window). No helper class. **A:** approved as proposed (Javier, 15/11/2026)
- **Q7:** The form. **Proposal:** *Endpoint* is a text field; *Settings* is a text area of five rows, one `key=value` per line; *Enabled* is a check box; *Mode* is two radio buttons, *Simulated* and *Real*. The label of the endpoint says "required in real mode". **A:** approved as proposed (Javier, 15/11/2026)
- **Q8:** How the settings text is read. **Proposal:** blank lines are ignored; each other line is split at the first `=`; the key and the value lose their spaces; a line with no `=`, with an empty key or with an empty value is an error ("Line 2: use key=value"); a key that is repeated is also an error ("The setting <key> is repeated"), because a map would hide it. The order of the lines is kept. **A:** approved as proposed (Javier, 15/11/2026)
- **Q9:** The table. **Proposal:** columns *System*, *Mode*, *Enabled* and *Status*; the systems are called "Single sign-on (SSO)", "Electronic health record (EHR)", "Admissions" and "RIS/PACS", always in the order of the server; *Mode* shows `SIMULATED` or `REAL`; *Enabled* shows "Yes" or "No"; *Status* shows "Active" or "Inactive". The first row is selected when the screen opens. **A:** approved as proposed (Javier, 15/11/2026)
- **Q10:** "SIMULATED" must not be mistaken for a real connection. **Proposal:** in the table and in the form the word is bold and in dark amber (readable on white); it is a word, not only a colour; and when the selected integration is simulated a line above the form says "SIMULATED: no real system is contacted". **A:** approved as proposed (Javier, 15/11/2026)
- **Q11:** *Revert*. **Proposal:** a button *Revert* that discards the unsaved changes and loads the saved configuration again, so that the user can test the connection without saving. **A:** approved as proposed (Javier, 15/11/2026)
- **Q12:** Messages. **Proposal:** after a save: "Integration Single sign-on (SSO) saved." in green; after a test: "Connection test of Single sign-on (SSO): Connection OK (3 ms)." in green when it works and "Connection test of Single sign-on (SSO): integration disabled." in red when it does not (the reason is the message of the server); errors of validation in red, all together, with `⚠` and each one ending with a full stop. When the server says that the user does not have the permission: "You do not have permission to configure the integrations." and only *Back* stays enabled, as in UC17. **A:** approved as proposed (Javier, 15/11/2026)
- **Q13:** Welcome screen and `Main`. **Proposal:** `IU_Welcome` gets one more constructor with `Runnable onConfigureIntegrations` and the button *Configure integrations* (Alt+I), shown only with `CONFIGURE_INTEGRATIONS`; the other constructors stay, so the tests of C1 do not change; the buttons are in one more column. `Main` has one `AuthenticatorSystemSimulator`, used by `AuthenticationService` and by `IntegrationManagementService` (the SSO that the login uses is the one that is tested), creates the service, calls `loadSeedData()` and shows the screen with the title "Manage integrations". **A:** approved as proposed (Javier, 15/11/2026)
- **Q14:** Keyboard. **Proposal:** *Save* Alt+S, *Test connection* Alt+T, *Revert* Alt+R, *Back* Alt+B; the labels of the fields Alt+E (endpoint), Alt+G (settings), Alt+M (mode), Alt+N (enabled), chosen not to repeat the ones of the welcome screen (V, M, C, L, I). **A:** approved as proposed (Javier, 15/11/2026)
- **Q15:** What happens if the form has unsaved changes and the user presses another row of the table. **Proposal:** the screen asks "Discard the unsaved changes?" (accept or cancel, with the same dialog as the other confirmations); if the user cancels, the selection goes back to the row it had, with its changes. **A:** approved (Javier, 15/11/2026)
- **Note:** the diagram of Design still has `getIntegration(system)`, `configureIntegration(...)` as `void` and `testConnection(system)` without a requester; the code follows the contract of #43 (the requester first, and `configureIntegration` returns the saved configuration). Design has to reflect it. The methods of the diagram (`displayIntegrations`, `onSaveConfiguration`, `onTestConnection`, `showTestResult`, `showOperationResultSuccess` and `showOperationResultError`) are kept with these names where they make sense.

## Screen (low-fidelity prototype)
```
+------------------------------------------------------------------------------+
| Radiodiagnostics System - Manage integrations                            _ X |
+------------------------------------------------------------------------------+
|  Integrations with other hospital systems                                    |
|  +------------------------------------------------------------------------+  |
|  | System                           Mode        Enabled   Status          |  |
|  | Single sign-on (SSO)             SIMULATED   Yes       Active          |  |
|  | Electronic health record (EHR)   SIMULATED   No        Inactive        |  |
|  | Admissions                       SIMULATED   Yes       Active          |  |
|  | RIS/PACS                         REAL        Yes       Active          |  |
|  +------------------------------------------------------------------------+  |
|  SIMULATED: no real system is contacted                                      |
|  Endpoint (required in real mode)         Settings (key=value, one per line) |
|  [____________________________]          [timeout=30                      ] |
|  Mode  (o) Simulated  ( ) Real            [                                ] |
|  [x] Enabled                                                                 |
|  Unsaved changes: save or revert to test    [ Revert ] [ Test connection ] [ Save ] |
|  (!) The endpoint must start with http:// or https://.                       |
|                                                                   [ Back ]   |
+------------------------------------------------------------------------------+
```
Welcome screen: a new button *Configure integrations* (Alt+I) after *Configure system*, visible only with `CONFIGURE_INTEGRATIONS`.

## Usability decisions
| Principle (IPO I) | Decision |
|---|---|
| Visibility of the system status | A message area says what happened (saved, result of the test, errors), and the line "Unsaved changes" shows when the form differs from what is saved |
| User's language | Plain names of the systems and messages; no codes (`RIS_PACS`) on the screen |
| Control and freedom | *Revert* discards the changes; *Back* returns to the welcome screen; nothing is lost without saying |
| Consistency and standards | Same message area, colours, icons, tabs of buttons and *Back* as *Manage roles* and *Configure system* |
| Error prevention | *Test connection* is disabled while there are unsaved changes (it would test something else); confirmation before disabling or switching to real mode; the system is chosen from the table, never typed |
| Recognition rather than recall | The four integrations are always in the table with their mode, whether they are enabled and their status; the format of the settings is written next to the field |
| Flexibility and shortcuts | Alt+S, Alt+T, Alt+R and Alt+B, and Tab moves between fields |
| Minimalist design | One screen, one table and one form |
| Recognise and recover from errors | All the errors of one save appear together, in red, with an icon, and the form keeps what the user typed |
| Gestalt: proximity and similarity | The form is right below the table it edits; each label right above its field; the buttons of the form together at the right |
| Colour and readability | "SIMULATED" is a word in bold and amber; results have an icon and text, not only colour |

## Files
| File | Action | What |
|---|---|---|
| `pom.xml` | change | managed version of `client-c2-configuration` `1.1.0-SNAPSHOT` (Q5) |
| `client/c2-configuration/pom.xml` | change | version `1.1.0-SNAPSHOT` (Q5) |
| `client/c2-configuration/src/main/java/es/uclm/esi/iso2/client/configuration/presentation/IU_ManageIntegrations.java` | create | the screen of Q6 to Q12 |
| `client/c1-access/pom.xml` | change | version `1.2.2-SNAPSHOT` (Q5) |
| `client/c1-access/src/main/java/es/uclm/esi/iso2/client/access/presentation/IU_Welcome.java` | change | one more constructor and the button *Configure integrations* (Q13) |
| `client/c1-access/src/main/java/es/uclm/esi/iso2/client/Main.java` | change | one `AuthenticatorSystemSimulator`, the service, `loadSeedData()` and the navigation (Q13) |
| `agents/architecture.md` | change | the version of the client of C2 and of C1 |
| `doc/plans/UC20-client.md` | create/update | this plan |

The screen is a `JPanel`, so that Adam's tests can create it without a display. It only imports `common` and Swing, never `server`. Every class and public method has its Javadoc in English (rule 18).

## Steps
Tick each step with the hash of its commit (green `mvn -B verify -Djacoco.skip=true` until Adam's tests exist; no test is written here).
- [x] 1. This plan (`docs(UC20): plan for the client part (#42)`). - 07b362d.
- [x] 2. Poms: versions of `client-c2-configuration` and `client-c1-access` (Q5). - a1bd3d5.
- [ ] 3. `IU_ManageIntegrations`, the display: table of the four integrations, the form of the selected one, "SIMULATED" in bold amber and the line above the form, *Back*, and the denied access (Q7, Q9, Q10, Q12).
- [ ] 4. `IU_ManageIntegrations`, *Save*: reading of the settings, validation in the client, confirmation, save and messages (Q1, Q3, Q8, Q12).
- [ ] 5. `IU_ManageIntegrations`, *Test connection* and *Revert*: the "Unsaved changes" line, the button disabled while there are changes, and the result of the test (Q2, Q11, Q12).
- [ ] 6. `IU_Welcome` (button) and `Main` (service, seed data, navigation) (Q13).
- [ ] 7. `agents/architecture.md`.
- [ ] 8. Hand the branch over to Adam: the manual check of the screen with `admin` (and `mgarcia`, who does not see the button) and the tests of UC20-client, who opens the pull request (`Closes #42`); close only with his tests green and the coverage check passing.

## How to run it
`mvn -B -ntp install -DskipTests` and then `mvn -B -pl client/c1-access exec:java -Dexec.mainClass=es.uclm.esi.iso2.client.Main`; log in with `admin` and press *Configure integrations*.

## Verification
- [ ] Every acceptance criterion of #42 is checked: the screen lists the four integrations with their status and mode; the administrator can edit the settings, enable or disable an integration, switch its mode and run a connection test, and the result (OK, or the reason) is shown; the screen is only available with the permission.
- [ ] `IU_ManageIntegrations` only imports `common` and Swing.
- [ ] No class of C1 changes except `IU_Welcome`, `Main` and the `pom.xml`.
- [ ] Each row of *Usability decisions* is checked in the running application (Adam).
- [ ] `mvn -B verify` is green with Adam's tests (the coverage check included).
- [ ] The CI of the pull request is green.

## Status
- **In progress:** nothing
- **Next step:** step 3 (`IU_ManageIntegrations`, the display, in `client/c2-configuration`)
- **Open questions:** none
- **Last update:** 15/11/2026, Javier
