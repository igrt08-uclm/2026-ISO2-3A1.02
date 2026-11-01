# Plan – UC19 – Server (#15)

| | |
|---|---|
| **Sub-issue** | #15 *UC19 – Server (module v1.1.0)* · branch `feature/UC19-server`, to be created by a member from #15, with `develop` as its source |
| **Use case** | #11 *UC19 – Audit system activity (module v1.1.0)* · FR19 · `server` and `common` `1.1.0` |
| **Specification** | #11, #15 and the wiki page *Requirements and Use Cases* (FR19) |
| **Design** | `doc/design/C1-server-design.png` · contracts: section 6 of #8 *C1. Access and security* · analysis: D03, D10, D11 and D12 of `doc/AN-IT1-DECISIONS.md` |
| **Plan approved by** | Javier, 01/11/2026 |

## Questions and answers
Answered by Javier on 01/11/2026.
- **Q:** How are the filters passed? **A:** As the `String` of the contract. Format: `key=value` pairs separated by `;`, for
  example `author=admin;action=LOGIN_OK;target=PAT-1;from=2026-11-01;to=2026-11-30`. Every key is optional, and an empty
  or null `String` returns everything.
- **Q:** How is each filter compared? **A:** `author` and `action` are exact; `target` contains the text, so
  `authentication` also finds `authentication:WRONG_PASSWORD`; `from` and `to` are `yyyy-MM-dd` dates, both included.
  An unknown key, a pair without `=` or a wrong date gives `IllegalArgumentException`.
- **Q:** Can a value contain `;`? **A:** No, because it separates the filters. The server has no way to tell them apart, so
  the screen of #14 *UC19 – Client (module v1.1.0)* must not let the user type it in a filter (Javier).
- **Q:** How is the patient filtered? **A:** With `target`: `AuditLog` keeps the patient or resource as an attribute until
  `Patient` exists (D03 of the analysis).
- **Q:** Who can consult the log? **A:** Only an existing, active user with the role `SYSTEM_ADMINISTRATOR`, and the **server**
  checks it: `getActivityLog(requester, filters)` throws `AccessDeniedException` otherwise (Javier). The actor of FR19 is the
  system administrator, but the design had no check. This changes the contract in section 6 of #8.
- **Q:** Is consulting the log audited (D12 of the analysis)? **A:** Yes, option (a), as the server now knows who asks: an allowed
  consultation is registered as `AUDIT_CONSULTED` and a rejected one as `AUDIT_ACCESS_DENIED`, both with the target `audit` (Javier).
- **Q:** Is the log kept when the application closes? **A:** No: it stays in memory, like the users, so it is lost on closing.
  A JSON file was implemented first (9fbb64a), but Marcos said in #15 that, as Ismael mentioned in class, persistence is not
  needed, so it was replaced (Javier). Persistence is discussed in Meeting 6.
- **Q:** Order and date type? **A:** The newest entries first, and `java.util.Date`, as in the diagram.
- **Q:** `AuditRecorderSimulator`? **A:** It is deleted when `AuditService` replaces it (D-023). As `client`'s `Main` uses it,
  this happens in #14 *UC19 – Client (module v1.1.0)*, not here.
- **Q:** Who writes the tests? **A:** As Adam's laptop is being repaired, the agent writes them in Adam's style, after
  Javier approves their design.
- **Q:** In which order is UC19 done? **A:** (1) implementation of the server, (2) implementation of the client,
  (3) design and implementation of the tests of the server, (4) design and implementation of the tests of the client.
  Each step has its own commit, one at a time (Javier).

## Points of the design to confirm (Marcos and Denis)
- `InMemoryAuditLogRepository` is not in the diagram. It implements `IAuditLogRepository`, like
  `InMemoryUserAccountRepository` does for the users.
- **Contract:** `IAuditService.getActivityLog(requester : String, filters : String)`, the new `AccessDeniedException` in
  `common`, and `IU_Audit` passing the logged user. Section 6 of #8 and the diagrams have to be updated.
- `AuditService` uses `IUserAccountRepository` to check who asks. The communication diagram uses `UserAccount` for another
  reason: `obtainUserData(id)` (step 4), to complete the records. `AuditLog` already has the author, so that step is not
  implemented unless Design says what data it needs.
- A failed login is still registered if the user does not exist: `register` does not check the author.

## Files
| File | Action | What |
|---|---|---|
| `common/src/main/java/es/uclm/esi/iso2/common/access/domain/AuditLog.java` | create | `id`, `timestamp`, `author`, `action`, `targetResource`; it cannot be modified (D11 of the analysis) |
| `common/src/main/java/es/uclm/esi/iso2/common/access/contracts/AccessDeniedException.java` | create | checked exception, like `AuthenticationException` |
| `common/src/main/java/es/uclm/esi/iso2/common/access/contracts/IAuditService.java` | create | `getActivityLog(requester, filters) : List<AuditLog>`, which throws `AccessDeniedException` |
| `server/src/main/java/es/uclm/esi/iso2/server/access/persistence/IAuditLogRepository.java` | create | `save(entry)` and `find(filters) : List<AuditLog>` |
| `server/src/main/java/es/uclm/esi/iso2/server/access/persistence/InMemoryAuditLogRepository.java` | create | keeps the entries in memory, only adds (D11), reads the filters and orders the results |
| `server/src/main/java/es/uclm/esi/iso2/server/access/service/AuditService.java` | create | implements `IAuditService` and `IAuditRecorder`; `register` gives each entry the next `id`, after the highest one saved, and its date; `getActivityLog` checks the requester with `IUserAccountRepository` and registers the consultation |
| `pom.xml`, `common/pom.xml`, `server/pom.xml` | change | `common` and `server` go to `1.1.0-SNAPSHOT` (D-014, D-021) |
| `common/src/test/java/…/domain/AuditLogTest.java` and `…/contracts/AccessDeniedExceptionTest.java` | create | test cases below |
| `server/src/test/java/…/persistence/InMemoryAuditLogRepositoryTest.java` | create | test cases below |
| `server/src/test/java/…/service/AuditServiceTest.java` and `FakeAuditLogRepository.java` | create | test cases below, and their test double |
| `server/src/test/java/…/service/AuditServiceIntegrationTest.java` | create | test cases below |

## Test cases (design of the tests)
Approved by Javier on 01/11/2026 (step 7). Same style as Adam's tests: `@DisplayName`, *Arrange, Act, Assert*, `@ParameterizedTest` for the similar cases,
hand-written test doubles, and a comment with the decision when a test comes from one.

| Class | Case | Expected result |
|---|---|---|
| `AuditLogTest` | Create an entry | Keeps its five attributes |
| `AuditLogTest` | Change the `Date` used to create it, or the one returned | The entry does not change (D11) |
| `AccessDeniedExceptionTest` | Create it with a requester | Keeps the requester, also if it is null |
| `InMemoryAuditLogRepositoryTest` | Empty repository | An empty list |
| `InMemoryAuditLogRepositoryTest` | `find` with a null or empty `String` | Every entry |
| `InMemoryAuditLogRepositoryTest` | Entries with different dates | The newest first |
| `InMemoryAuditLogRepositoryTest` | Entries with the same date | The highest `id` first |
| `InMemoryAuditLogRepositoryTest` | Filter by `author`, by `action`, each exact | Only those entries; a part of the text finds nothing |
| `InMemoryAuditLogRepositoryTest` | Filter by `target` with part of the text | The entries that contain it |
| `InMemoryAuditLogRepositoryTest` | `from`, `to`, and both | Both dates included, the whole day of `to` |
| `InMemoryAuditLogRepositoryTest` | Several filters together | Only the entries that meet all of them |
| `InMemoryAuditLogRepositoryTest` | Filters with no result | An empty list |
| `InMemoryAuditLogRepositoryTest` | Spaces around the values, and an empty pair (`author=admin;`) | They are ignored |
| `InMemoryAuditLogRepositoryTest` | Unknown key, pair without `=`, wrong date | `IllegalArgumentException` |
| `InMemoryAuditLogRepositoryTest` | Change the returned list | The repository does not change (D11) |
| `AuditServiceTest` | `register` | Saves one entry with its author, action, target and date |
| `AuditServiceTest` | Several `register` calls | Different `id` in each entry |
| `AuditServiceTest` | `register` for a user that does not exist | It is saved: failed logins have an author |
| `AuditServiceTest` | `getActivityLog` by an active administrator | Sends the filters to the repository and returns its result |
| `AuditServiceTest` | Requester unknown, null, inactive, or without the administrator role | `AccessDeniedException`, and no records are returned |
| `AuditServiceTest` | An allowed consultation | Registers `AUDIT_CONSULTED`, which does not appear in its own results (D12) |
| `AuditServiceTest` | A rejected consultation | Registers `AUDIT_ACCESS_DENIED` with the requester as author |
| `AuditServiceTest` | Wrong filters from an administrator | `IllegalArgumentException`, and no consultation is registered |
| `AuditServiceIntegrationTest` | Successful and failed logins with `AuthenticationService` | `admin` can consult them with `getActivityLog` |
| `AuditServiceIntegrationTest` | `mgarcia` and `pnavarro` try to consult | Access denied, and `admin` then sees both attempts |

## Order of the work
| Stage | What | Branch |
|---|---|---|
| 1 | Implementation of the server | `feature/UC19-server`, from `develop` |
| 2 | Implementation of the client, with its own plan `doc/plans/UC19-client.md` | `feature/UC19-client`, from `feature/UC19-server` |
| 3 | Design and implementation of the tests of the server | `feature/UC19-server`, then merged into `feature/UC19-client` |
| 4 | Design and implementation of the tests of the client | `feature/UC19-client` |

The pull requests follow the same order as in UC22: first the server, and the client after it is merged.

## Steps
One step, one commit, one at a time. Tick each step with the hash of its commit (green `mvn -B verify`).

**Stage 1: implementation of the server**
- [x] 1. This plan. `93c1b6c`
- [x] 2. `common`: `AuditLog`. `dbe98ad`
- [x] 3. `common`: `AccessDeniedException` and `IAuditService`. `e4dd5ff`
- [x] 4. `server`: `IAuditLogRepository` and a repository with the filters, first as a JSON file. `9fbb64a`
- [x] 5. `server`: `AuditService`. `a000b85`
- [x] 6. `pom.xml`, `common/pom.xml` and `server/pom.xml`: version `1.1.0-SNAPSHOT`. `fd50454`
- [x] 6a. `InMemoryAuditLogRepository` replaces the JSON file, and `data/` leaves `.gitignore`, after Marcos' comment in #15. `34aa3b0`

Checked by hand on 01/11/2026 with a temporary program, with the repository in memory: logins consulted by `admin`, access
denied to the other four requesters, every filter and the wrong filters.

**Stage 3: tests of the server** (after stage 2, in `doc/plans/UC19-client.md`)
- [x] 7. Design of the tests: Javier approves the test cases, written here. `48bb774`
- [x] 8. Tests of `common`. `f047044`
- [x] 9. Tests of `InMemoryAuditLogRepository`. `90d448e`
- [ ] 10. Tests of `AuditService`, unit and integration.

## Verification
- [x] `mvn -B verify` is green, with every test case above.
- [x] The tests were checked to fail when the code is wrong: 10 errors were introduced on purpose in a temporary copy (the active
      check, the role check, each registered consultation, the ids, the order, three filters and the copy of the date), and all
      of them were detected.
- [ ] The interfaces match the contracts: `register(action)` and `getActivityLog(requester, filters)` of section 6 of #8, once
      it is updated.
- [x] Storing an activity has effect and it can be consulted by an administrator, and only by one (#15).
- [ ] Code reviewed by a member, in the pull request.

## Status
- **In progress:** nothing
- **Next step:** stage 4, on `feature/UC19-client`: merge this branch and write the tests of the client.
- **Open questions:**
  - The points of the design above.
  - Meeting 6: persistence. The audit log and the users are in memory, and everything is lost on closing.
  - The server trusts the `requester` it receives: there are no sessions (D09 of the analysis), and the client and the server
    are one program (D-016).
  - The role check is temporary: with UC18 it should use the permissions.
- **Last update:** 01/11/2026, Javier
