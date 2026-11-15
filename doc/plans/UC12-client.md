# Plan – UC12 – Client (#45)

| | |
|---|---|
| **Sub-issue** | #45 *UC12 – Client (C3 v1.0.0)* · branch `feature/UC12-client` (it already contains `feature/UC12-server`) |
| **Use case** | #44 *UC12 – Manage patient demographic data (register and modify) (C3 v1.0.0)* · FR12 · component C3 1.0.0 |
| **Specification** | use case issue #44, sub-issue #45 and the wiki page *Requirements and Use Cases* |
| **Design** | `doc/design/C3-client-design.png` (`IU_ManagePatients`) · contracts: `IPatientService` (see `doc/plans/UC12-server.md`) · section 6 of #47 *C3. Patients* |
| **Usability guide** | IPO I, as in `doc/plans/UC17-client.md` |
| **Plan approved by** | Javier, 15/11/2026 |

## Decisions already taken
- **Screen (Implementation, Javier; Marcos agreed in #45):** one screen with a search box and a table of results on the left and the form of the selected patient on the right; the button *New patient* clears the form; a button *Manage patients* in the welcome screen, only with `MANAGE_PATIENTS`; when modifying, the identifier cannot be changed; the date is written as `dd/MM/yyyy`; if the patient already exists the screen says so and loads the existing record in the form; the validation errors are shown together, in red, and nothing is saved.
- **Validation in the client and in the server (NFR03; Ignacio and Marcos in #46, David in #46):** the screen uses the same rules as the server.
- **Rules of the data (David, #46, D-037):** required are the identifier (the health card number, unique), the full name, the date of birth and the sex; the national ID is optional; the date of birth cannot be in the future; the e-mail, if given, has the form `name@domain.ext`; the phone, if given, has 9 to 15 digits (spaces and a leading `+` are ignored).
- **The server is done (#46):** `IPatientService` in `common` (`searchPatients`, `getPatient`, `registerPatient` and `modifyPatient`), `DuplicatePatientException`, `PatientService` and the seed data with three invented patients. Only the Admissions staff (`lmartin`) have `MANAGE_PATIENTS`; the administrator does not.
- **Not in this version:** the discharge of a patient is in iteration 8.
- The tests and the manual check of the screen are Adam's (Testing): no step here writes them; the last one hands the branch over.
- The button in `IU_Welcome` and the wiring in `Main` (client of C1) go in the pull request of this sub-issue, as in UC17 and UC20 (Javier, 15/11/2026).

## Questions and answers
Few questions, as Ignacio asked in Meeting 8 (E-19): all of them are answered, and the screen is decided by Implementation.
- **Q1:** Module, packages and versions. **Proposal:** a new module `client/c3-patients` (`client-c3-patients`, version `1.0.0-SNAPSHOT`, depends only on `common` and JUnit), package `es.uclm.esi.iso2.client.patients.presentation`; `client-c1-access` gets the dependencies on `server-c3-patients` and `client-c3-patients` (because its `Main` starts the program) and goes to `1.2.3-SNAPSHOT` (UC20-client, in review, takes `1.2.2`). `IU_Welcome`, `Main`, the poms and `agents/architecture.md` are also changed by UC20-client: when `develop` is brought to this branch, the second one to be merged solves the small conflicts. Creating a module and changing poms needs the approval of Javier (rule 9). **A:** approved as proposed with a change: the dependencies and the version of `client-c1-access` are done in step 6, after the pull requests of UC20 are merged into `develop` (Javier chose the mixed option on 15/11/2026, to avoid the conflicts) (Javier, 15/11/2026)
- **Q2:** Class. **Proposal:** one class `IU_ManagePatients` (a `JPanel`, the name of the design), which receives the `IPatientService`, the username of the requester, a `Predicate<String>` to confirm (as the other screens, so that Adam's tests do not open a window) and the action of *Back*. The methods of the diagram are kept with their names: `onSearch`, `displayPatients`, `onNewPatient`, `onSavePatient`, `showValidationErrors`, `showOperationResultSuccess`, `showOperationResultError` and `showExistingPatient`. **A:** approved as proposed (Javier, 15/11/2026)
- **Q3:** The form and the table. **Proposal:** the table has *Identifier*, *Full name* and *Date of birth*; the form has the identifier, the national ID (optional), the full name, the date of birth (`dd/MM/yyyy`), the sex (a list: Female, Male, Unspecified), the address, the phone and the e-mail, each label right above its field; the buttons are *Search*, *New patient* and *Save*. When the screen opens it shows all the patients (a search with an empty text). **A:** approved as proposed (Javier, 15/11/2026)
- **Q4:** Search. **Proposal:** *Search* (or the Enter key in the box) asks the server with the text, and a line says how many patients were found ("3 patients found."); an empty text lists all of them. No patient is selected after a search. **A:** approved as proposed (Javier, 15/11/2026)
- **Q5:** Validation in the client. **Proposal:** the same rules and the same messages as the server (listed in `doc/plans/UC12-server.md`, Q5), plus "The date of birth must be written as dd/MM/yyyy" for a text that is not a valid date (strict: 31/02/2000 is not valid); all the errors together and nothing is sent; the field of each error found by the client gets a red border (the errors that come from the server are only in the list). **A:** approved as proposed (Javier, 15/11/2026)
- **Q6:** Duplicate. **Proposal:** when the server answers `DuplicatePatientException`, the screen shows "A patient with this identifier already exists: the record is loaded so that you can modify it." (`showExistingPatient`), selects that patient in the table and loads it in the form, with the identifier locked. **A:** approved as proposed (Javier, 15/11/2026)
- **Q7:** Messages. **Proposal:** after a registration "Patient P-0004 registered." and after a modification "Patient P-0004 saved." in green; errors in red with `⚠`, each one ending with a full stop; when the server says that the user does not have the permission: "You do not have permission to manage patients." and only *Back* stays enabled. **A:** approved as proposed (Javier, 15/11/2026)
- **Q8:** Unsaved changes. **Proposal:** if the form has unsaved changes and the user presses another row, *New patient* or *Search*, the screen asks "Discard the unsaved changes?" (accept or cancel); if the user cancels, nothing changes. A form of a new patient that is empty is not a change. **A:** approved as proposed (Javier, 15/11/2026)
- **Q9:** Welcome screen and `Main`. **Proposal:** `IU_Welcome` gets one more constructor with `Runnable onManagePatients` and the button *Manage patients* (Alt+P), shown only with `MANAGE_PATIENTS`; the other constructors stay, so the tests of C1 do not change. `Main` creates the `PatientService` with the role service and the audit service of C1, calls `loadSeedData()` and shows the screen with the title "Manage patients". **A:** approved as proposed (Javier, 15/11/2026)
- **Q10:** Keyboard. **Proposal:** *Search* Alt+H, *New patient* Alt+N, *Save* Alt+S and *Back* Alt+B; the fields Alt+I (identifier), Alt+A (national ID), Alt+F (full name), Alt+D (date of birth), Alt+X (sex), Alt+R (address), Alt+O (phone) and Alt+E (e-mail). **A:** approved as proposed (Javier, 15/11/2026)
- **Note:** the diagram of Design still shows `UserAccount` as `requester`, `updatePatient(requester, patient)`, a `Patient` without `nationalId` and `Date`; the code follows #47 and D-037 (`modifyPatient(requester, patientId, patient)`, `LocalDate`). Design has to reflect it (#114).

## Screen (low-fidelity prototype)
```
+------------------------------------------------------------------------------+
| Radiodiagnostics System - Manage patients                                _ X |
+------------------------------------------------------------------------------+
|  Patients                                                                    |
|  Search [____________________] [ Search ]         Identifier  [HC-0000001  ] |
|  +--------------------------------------------+   National ID [             ]|
|  | Identifier   Full name            Birth    |   Full name   [Lucía Ejem...]|
|  | HC-0000001 > Lucía Ejemplo Prueba 14/03/80 |   Date of birth [14/03/1980 ]|
|  | HC-0000002   Mario Demo Simulado  02/11/75 |   Sex         [Female      v]|
|  | HC-0000003   Elena Muestra...     25/07/01 |   Address     [Calle Inve...]|
|  +--------------------------------------------+   Phone       [600000001   ] |
|  3 patients found.                                E-mail      [lucia.ejem..] |
|  [ New patient ]                                              [ Save ]       |
|  (!) The date of birth cannot be in the future. The phone must have 9 to 15 digits. |
|                                                                   [ Back ]   |
+------------------------------------------------------------------------------+
```
Welcome screen: a new button *Manage patients* (Alt+P), visible only with `MANAGE_PATIENTS`.

## Usability decisions
| Principle (IPO I) | Decision |
|---|---|
| Visibility of the system status | A message area says what happened (registered, saved, errors) and a line says how many patients were found |
| User's language | Plain names; the sex is written "Female", "Male" or "Unspecified"; dates as `dd/MM/yyyy`, the way they are written in Spain |
| Control and freedom | *New patient* clears the form; *Back* returns to the welcome screen; nothing is deleted |
| Consistency and standards | Same message area, colours, icons, confirmations and *Back* as the other screens |
| Error prevention | The identifier is locked when modifying; the sex is chosen from a list; the dates and the phone are checked before the server is called; the screen asks before it throws away unsaved changes |
| Recognition rather than recall | The patients are always in the table; each label is right above its field, and the format of the date is written next to it |
| Flexibility and shortcuts | Enter searches; Alt+H, Alt+N, Alt+S and Alt+B, and Tab moves between fields |
| Minimalist design | One screen: search and table at the left, form at the right |
| Recognise and recover from errors | All the errors of one save appear together, in red, with an icon, the field with a problem is marked, and the form keeps what the user typed |
| Gestalt: proximity and similarity | Search, table and the button *New patient* together at the left; the form and *Save* together at the right |
| Colour and readability | The errors have an icon and text, not only colour, and the field with a problem is marked with a border |

## Files
| File | Action | What |
|---|---|---|
| `client/pom.xml`, `pom.xml` | change | module `c3-patients`; managed versions of `server-c3-patients` (already) and `client-c3-patients` (Q1) |
| `client/c3-patients/pom.xml` | create | `client-c3-patients` 1.0.0-SNAPSHOT, depends only on `common` and JUnit |
| `client/c3-patients/src/main/java/es/uclm/esi/iso2/client/patients/presentation/IU_ManagePatients.java` | create | the screen of Q2 to Q8; **new Java package** |
| `client/c1-access/pom.xml` | change | version `1.2.3-SNAPSHOT` and the dependencies on `server-c3-patients` and `client-c3-patients` (Q1) | Done in step 6.
| `client/c1-access/src/main/java/es/uclm/esi/iso2/client/access/presentation/IU_Welcome.java` | change | one more constructor and the button *Manage patients* (Q9) |
| `client/c1-access/src/main/java/es/uclm/esi/iso2/client/Main.java` | change | the service, `loadSeedData()` and the navigation (Q9) |
| `agents/architecture.md` | change | the code tree with `client/c3-patients` and the versions |
| `doc/plans/UC12-client.md` | create/update | this plan |

The screen is a `JPanel`, so that Adam's tests can create it without a display. It only imports `common` and Swing, never `server`. Every class and public method has its Javadoc in English (rule 18).

## Steps
Tick each step with the hash of its commit (green `mvn -B verify -Djacoco.skip=true` until Adam's tests exist; no test is written here).
- [x] 1. This plan (`docs(UC12): plan for the client part (#45)`). - 3b23aa8.
- [x] 2. Poms: the module `client/c3-patients` and its managed version (Q1). The poms of `client-c1-access` wait for step 6. - 0f9846d.
- [x] 3. `IU_ManagePatients`, the display: search box, table, form of the selected patient, the line with the number found, *Back* and the denied access (Q3, Q4, Q7). - 7fdfb4e.
- [x] 4. `IU_ManagePatients`, *Save* and *New patient*: reading of the form, validation in the client, register or modify, duplicate and messages (Q5, Q6, Q7). - ed7c678.
- [x] 5. `IU_ManagePatients`, unsaved changes: the question before another row, *New patient* or *Search* (Q8). - 7048310. Checked roughly with a throwaway program outside the repo that presses the buttons with the real service (open, select, new, errors, register, duplicate, modify, the questions and the denied access); no test was written.
- [x] 6. When the pull requests of UC20 (#118, #119) are merged into `develop`: bring `develop` to this branch (merge); `IU_Welcome` (button) and `Main` (service, seed data, navigation); the version `1.2.3-SNAPSHOT` and the dependencies of `client-c1-access` (Q1, Q9). - 1061a87. develop with UC20 was brought in 0601380 (one conflict, in architecture.md, solved keeping both lines). Checked roughly with a throwaway program: only the Admissions staff sees *Manage patients*; the 7-argument constructor does not show it; no test was written.
- [ ] 7. `agents/architecture.md` (after step 6, so that it is written over the changes of UC20).
- [ ] 8. Hand the branch over to Adam: the manual check of the screen with `lmartin` (`admin` and `mgarcia` do not see the button) and the tests of UC12-client, who opens the pull request (`Closes #45`); close only with his tests green and the coverage check passing.

## How to run it
`mvn -B -ntp install -DskipTests` and then `mvn -B -pl client/c1-access exec:java -Dexec.mainClass=es.uclm.esi.iso2.client.Main`; log in with `lmartin` / `Admissions.2026` and press *Manage patients*.

## Verification
- [ ] Every acceptance criterion of #45 is checked: the Admissions staff register, search and modify from the screen; invalid or duplicate data is rejected with an error message; the screen is only available with the permission; the interfaces match #47 as changed in the plan of the server.
- [ ] `IU_ManagePatients` only imports `common` and Swing.
- [ ] No class of C1 changes except `IU_Welcome`, `Main` and the `pom.xml`.
- [ ] Each row of *Usability decisions* is checked in the running application (Adam).
- [ ] `mvn -B verify` is green with Adam's tests (the coverage check included).
- [ ] The CI of the pull request is green.

## Status
- **In progress:** nothing
- **Next step:** step 7 (`agents/architecture.md`)
- **Open questions:** none
- **Last update:** 15/11/2026, Javier
