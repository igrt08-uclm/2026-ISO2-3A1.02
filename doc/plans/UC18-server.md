# Plan – UC18 – Server (#17)

| | |
|---|---|
| **Sub-issue** | #17 *UC18 – Server (module v1.2.0)* · branch `feature/UC18-server`, from `develop` |
| **Use case** | #10 *UC18 – Manage roles and permissions (module v1.2.0)* · FR18 · `server` and `common` `1.2.0` |
| **Specification** | #10, #17 and the wiki page *Requirements and Use Cases* (FR18) |
| **Design** | `doc/design/C1-server-design.png` · contracts: section 6 of #8 *C1. Access and security* · analysis: D10, D13, D14 and D15 of `doc/AN-IT1-DECISIONS.md` |
| **Plan approved by** | Javier, 02/11/2026; steps 10a and 11, 06/11/2026 |

## Questions and answers
Answered by Javier on 02/11/2026.
- **Q:** Who can change roles and permissions? **A:** Only an existing, active user with the permission `MANAGE_ROLES`, and the server
  checks it: the four operations of change receive `requester` and throw `AccessDeniedException` otherwise. This changes the contract
  of section 6 of #8, so it is asked to Marcos and Denis in #17. It is the same `requester` that step 9 of the diagram needs.
- **Q:** Who can consult the roles and permissions of a user? **A:** Any user: `getRoles` and `getPermissions` do not change (section 6 of #8).
- **Q:** Which permissions exist? **A:** Only `MANAGE_ROLES`, in the role System administrator. The other three roles start with
  none, and each use case adds its own (rule 3). `VIEW_AUDIT_LOG` was planned too, but it was removed on 06/11/2026: the audit log
  of UC19 checks the role, so the permission would have had no effect (Javier).
- **Q:** What if a user already has the role, or does not have the one to revoke? **A:** Nothing happens, and nothing is audited. The
  same for permissions.
- **Q:** What if the role, the user or the permission does not exist? **A:** `IllegalArgumentException`.
- **Q:** Can the last administrator lose the role? **A:** No: an operation that leaves no active user with `MANAGE_ROLES` gives
  `IllegalStateException`. So the role of the last active administrator cannot be revoked, and neither can `MANAGE_ROLES`.
- **Q:** Does a change reach the users of a role? **A:** Yes, at once (section 6 of #8): `getPermissions` and the check of `requester`
  read the permissions from `IRoleRepository`, not from the copy of the role inside `UserAccount`.
- **Q:** What is registered? **A:** `ROLE_ASSIGNED`, `ROLE_REVOKED`, `PERMISSION_ASSIGNED` and `PERMISSION_REVOKED`, with targets such as
  `user:mgarcia/role:PHYSICIAN` and `role:PHYSICIAN/permission:VIEW_AUDIT_LOG`, and `ROLE_CHANGE_DENIED` with the target `roles`.
  They use `IAuditRecorder`, which is already on `develop`, so this branch does not need UC19 (D-023).
- **Q:** Persistence? **A:** In memory, like the users. Persistence is discussed in Meeting 6.
- **Q:** How does the client choose the user and the role? **A:** From lists: `getUsers()` and `getAvailableRoles()` are added to
  `IRoleManagementService`. Marcos agreed in #16 *UC18 – Client (module v1.2.0)* and will update the diagrams (Javier, 06/11/2026).
- **Q:** Does UC18 change the audit log of UC19? **A:** No: UC18 does not change any class of UC19, so that each use case keeps
  its own code, and `AuditService` keeps checking the role `SYSTEM_ADMINISTRATOR` (Javier, 06/11/2026).
- **Q:** Who writes the tests? **A:** As Adam's laptop is being repaired, the agent writes them in Adam's style, after Javier approves
  their design.
- **Q:** In which order is UC18 done? **A:** The order of UC19: server, client, tests of the server, tests of the client, with one
  commit per step. The client waits for UC19 to be on `develop`, because both change `Main` and `IU_Welcome`.

## Points of the design to confirm (Marcos and Denis)
- **Contract:** `requester` in the four operations of change. Marcos accepted it in #17, and Design will update the diagrams.
- **Contract:** `getUsers()` and `getAvailableRoles()`, accepted by Marcos in #16.
- `InMemoryRoleRepository` and `InMemoryPermissionRepository` are not in the diagram.
- `IUserAccountRepository` needs `findAll()` to count the active administrators. The diagram only has `findByUsername` and `save`.
- `Role` has to keep its permissions, as the association `Role * — * Permission` of the diagram says.
- `getAvailablePermissions()` is in `RoleManagementService`, but not in `IRoleManagementService`, and the client needs it (step 4b of
  the diagram). It is added to the interface.

## Files
| File | Action | What |
|---|---|---|
| `common/src/main/java/es/uclm/esi/iso2/common/access/domain/Permission.java` | create | `code`, `description` |
| `common/src/main/java/es/uclm/esi/iso2/common/access/domain/Role.java` | change | keeps its permissions; the constructor with three values stays |
| `common/src/main/java/es/uclm/esi/iso2/common/access/contracts/AccessDeniedException.java` | create | the same class as in `feature/UC19-server`, so that both merge without conflicts |
| `common/src/main/java/es/uclm/esi/iso2/common/access/contracts/IRoleManagementService.java` | create | the contract, with `requester` and `getAvailablePermissions()` |
| `server/src/main/java/es/uclm/esi/iso2/server/access/persistence/IPermissionRepository.java` | create | `findByCode`, `findAll` |
| `server/src/main/java/es/uclm/esi/iso2/server/access/persistence/InMemoryPermissionRepository.java` | create | preloaded with `MANAGE_ROLES` |
| `server/src/main/java/es/uclm/esi/iso2/server/access/persistence/IRoleRepository.java` | create | `findById`, `findAll`, `save` |
| `server/src/main/java/es/uclm/esi/iso2/server/access/persistence/InMemoryRoleRepository.java` | create | preloaded with the four roles |
| `server/src/main/java/es/uclm/esi/iso2/server/access/persistence/IUserAccountRepository.java` and `InMemoryUserAccountRepository.java` | change | `findAll()` |
| `server/src/test/java/…/service/FakeUserAccountRepository.java` | change | `findAll()`, so that Adam's tests keep compiling |
| `server/src/main/java/es/uclm/esi/iso2/server/access/service/RoleManagementService.java` | create | implements `IRoleManagementService` |
| `pom.xml`, `common/pom.xml`, `server/pom.xml` | change | `common` and `server` go to `1.2.0-SNAPSHOT` (D-014, D-021) |

## Order of the work
| Stage | What | Branch |
|---|---|---|
| 1 | Implementation of the server (this plan) | `feature/UC18-server`, from `develop` |
| 2 | Implementation of the client, with its own plan | `feature/UC18-client`, once UC19 is on `develop` |
| 3 | Design and implementation of the tests of the server | `feature/UC18-server` |
| 4 | Design and implementation of the tests of the client | `feature/UC18-client` |

Pull requests: UC19 first, as D-021 sets. Before opening the one of this branch, `develop` is merged here, and any conflict in the
versions of the `pom.xml` files is solved then.

## Steps
One step, one commit, one at a time. Tick each step with the hash of its commit (green `mvn -B verify`).

**Stage 1: implementation of the server**
- [x] 1. This plan. `9df81dc`
- [x] 2. `common`: `Permission`, and the permissions of `Role`. `472b5ca`
- [x] 3. `common`: `AccessDeniedException` and `IRoleManagementService`. `6978cfe`
- [x] 4. `server`: `IPermissionRepository` and `InMemoryPermissionRepository`. `c2b0da3`
- [x] 5. `server`: `IRoleRepository` and `InMemoryRoleRepository`. `05125e0`
- [x] 6. `server`: `findAll()` in `IUserAccountRepository`, its implementation and the test double. `d06e57b`
- [x] 7. `server`: `RoleManagementService`: `getRoles`, `getPermissions` and `getAvailablePermissions`. `acda231`
- [x] 8. `server`: `RoleManagementService`: the four operations of change, the checks and the audit. `3e11094`
- [x] 9. `pom.xml`, `common/pom.xml` and `server/pom.xml`: version `1.2.0-SNAPSHOT`. `a1acc76`
- [x] 10. Check by hand with a temporary program, and write the result here. Result on 02/11/2026, all as expected: the queries; the
      four changes, also repeated; four rejected requesters; unknown or null values; a change of permissions reaches the users at
      once; the last active administrator cannot lose the role or `MANAGE_ROLES`, an inactive one does not count; and the audit.

**Stage 1, added on 06/11/2026** (after UC19 was merged into `develop`)
- [x] 10a. Only `MANAGE_ROLES`: `VIEW_AUDIT_LOG` is removed from the permissions and from the role System administrator. `c752e22`
- [x] 11. `common` and `server`: `getUsers()` and `getAvailableRoles()`.

**Stage 3: tests of the server** (after the client)
- [ ] 12. Design of the tests: Javier approves the test cases, written here.
- [ ] 13. Tests, in Adam's style.

## Verification
- [x] `mvn -B verify` is green.
- [x] A change of roles or permissions takes full effect at once (#17).
- [x] Only an active user with `MANAGE_ROLES` can make a change, and each change and each rejected attempt is registered.
- [x] No operation leaves the system without an active administrator.
- [ ] The tests were checked to fail when the code is wrong.
- [ ] The interfaces match the contracts of section 6 of #8, once it is updated.
- [ ] Code reviewed by a member, in the pull request.

## Status
- **In progress:** nothing
- **Next step:** stage 2, in `doc/plans/UC18-client.md`, on `feature/UC18-client`. Then step 12 here.
- **Open questions:**
  - Should the audit log be controlled by a permission instead of the role? It would mean changing UC19: a question for the team.
  - The points of the design above.
  - The server trusts the `requester` it receives: there are no sessions (D09 of the analysis), and the client and the server are one
    program (D-016).
- **Last update:** 06/11/2026, Javier
