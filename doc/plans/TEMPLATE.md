# Plan – UCxx – Server | Client (#n)

> Copy this file as `doc/plans/UCxx-server.md` or `doc/plans/UCxx-client.md` on the branch of the sub-issue.
> The agent writes it in step 3 of the cycle in [AGENTS.md](../../AGENTS.md); the member approves it before any code
> is written, and it is kept up to date until the pull request is merged.

| | |
|---|---|
| **Sub-issue** | #n · branch `feature/UCxx-server` |
| **Use case** | #n · FRxx · module version x.y.0 |
| **Specification** | use case issue #n and the wiki page *Requirements and Use Cases* |
| **Design** | `doc/design/…` · contracts: section 6 of the component issue #n |
| **Plan approved by** | member, dd/mm/yyyy |

## Questions and answers
Every doubt asked in step 2, with the member's answer.
- **Q:** … **A:** …

## Files
| File | Action | What |
|---|---|---|
| `server/src/main/java/es/uclm/esi/iso2/server/<component>/…` | create | … |
| `server/src/test/java/es/uclm/esi/iso2/server/<component>/…` | create | tests of … |

## Steps
Tick each step with the hash of its commit (green `mvn -B verify`).
- [ ] 1. …
- [ ] 2. …

## Verification
- [ ] Every acceptance criterion of the sub-issue is checked, with the test that proves it.
- [ ] `mvn -B verify` is green.

## Status
- **Next step:** … (file, class or method)
- **Open questions:** …
- **Last update:** dd/mm/yyyy, member
