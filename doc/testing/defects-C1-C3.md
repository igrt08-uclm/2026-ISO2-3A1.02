# Defects and findings of Testing – C1, C2 and C3 (15/11/2026)

Found by Adam Gouram (Testing) while designing and running the tests of UC22, UC17, UC20, the C1 patch 1.2.2 and UC12,
and in the manual tests. Each one is assigned to the member who owns that part (agents/components.md):
**Implementation – Javier**, **Design – Marcos and Denis**, **Requirements – Ignacio and David**.

Status: **Fixed** · **Open (defect)** = contradicts the plan or the requirements · **Open (to decide)** = the plan
does not say it; someone has to decide.

## Implementation (Javier Villanueva)

| Id | Use case | Finding | Status |
|---|---|---|---|
| F-01 | UC22 | `InMemoryUserAccountRepository.save()` with a null username broke `findByUsername` (NullPointerException) | Fixed |
| F-02 | UC22 | `IU_Login.showSuccess()` was public and could send `null` to `IU_Welcome` | Fixed (now private) |
| F-03 | UC17 | A test type with a priority that does not exist (`XYZ`) was accepted | Fixed (ce14819) |
| F-04 | UC17 | Duplicated-id message said "A appointment policy" | Fixed (25d005a) |
| F-05 | UC17 | The time `24:00` was accepted as `00:00` | Fixed (25d005a) |
| F-06 | UC12 client | **D1:** with unsaved changes, *New patient* asks "Discard the unsaved changes?" **twice**; cancelling the second one loses the changes anyway. Q8 says one question. Confirmed by hand | **Open (defect)** |
| F-07 | UC12 client | **D2:** dragging the selection to another row and back to the same one loses the changes without asking | **Open (defect, minor)** |
| F-08 | UC12 client | **D3:** *Back* with unsaved changes leaves the screen without asking. The usability table of the plan says "the screen asks before it throws away unsaved changes". Found by hand | **Open (defect)** |
| F-09 | UC12 client | The error line is cut on the right when it is long (it goes under the *Back* button). Found by hand | **Open (defect, usability)** |
| F-10 | UC12 client | Alt+H is used by both the search label and the *Search* button | **Open (defect, minor)** |
| F-11 | UC12 server | The id counter (`P-0001`…) is in the service, not in the repository: a service built with a repository that already has patients starts again at `P-0001` and overwrites a patient. Does not happen with `Main` | Open (to decide) |
| F-12 | UC12 server | Blank identifier in a modification gives two messages ("is required" and "cannot be changed") | Open (to decide, minor) |
| F-13 | UC12 server | `getPatient` / `modifyPatient` of an unknown id are not audited, while a denied one is | Open (to decide) |
| F-14 | UC20 server | With `system = null`, a user without the permission gets `IllegalArgumentException` instead of `AccessDeniedException`, and the denied access is not audited | Open (to decide) |
| F-15 | UC20 server | The keys and values of the settings are saved with their spaces (`" 30 "`); only the endpoint is trimmed | Open (to decide, minor) |
| F-16 | UC20 server | `null` settings are stored as `null`, not as an empty map | Open (to decide, minor) |
| F-17 | UC20 client | If the server returned a `null` endpoint, the form would always show "Unsaved changes" (today the server always stores `""`) | Open (to decide, minor) |
| F-18 | UC20 client | After an access denied, the table can still be clicked and can ask "Discard the unsaved changes?" | Open (to decide, minor) |
| F-19 | UC20 client | *Revert* does not clear the message area, so an old error stays visible | Open (defect, minor) |
| F-20 | UC17 server | A **new** test type can be created with a **deactivated** priority (Q9 only thought of editing old ones) | Open (to decide) |

## Requirements (Ignacio Romero and David Calzado) – rules that are not defined

| Id | Use case | Finding | Status |
|---|---|---|---|
| R-01 | UC12 | Format of the identifier: the examples are `HC-0000001` (with `-`), but `HC0000099` is accepted. Must the format be fixed? Found by hand | Open (to decide) |
| R-02 | UC12 | The duplicate check ignores case but only the spaces at the ends (`HC 0000001` ≠ `HC0000001`), while Q4 says "ignoring case and spaces" | Open (to decide) |
| R-03 | UC12 | The search does not ignore accents: `lucia` does not find "Lucía" | Open (to decide) |
| R-04 | UC12 | The e-mail rule accepts `ana@example.test.` and `ana@example..test`; the date of birth has no lower limit (`01/01/1800`); the phone keeps inner spaces | Open (to decide) |
| R-05 | UC20 | The endpoint rule accepts `http://` alone (no host) and is case sensitive (`HTTPS://…` is rejected) | Open (to decide) |

## Design (Marcos Loro and Denis Ionescu) – diagrams that do not match the code

| Id | Use case | Finding | Status |
|---|---|---|---|
| D-01 | UC22 | The communication diagram of the login shows the SSO check first; the agreed order (#13) is user → SSO → active | Check if updated |
| D-02 | UC17 | `C2-client-design.png` still shows `SystemParameter`, `ParameterGroup` and `getParameters(group)`; the code uses one class and one operation per group (#40) | Open |
| D-03 | UC20 | `configureIntegration` returns the saved configuration; the diagram has `void` | Open |
