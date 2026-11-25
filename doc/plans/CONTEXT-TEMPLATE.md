# Context – Cn Name (#n)

> Copy this file as `doc/plans/Cn-context.md`. The agent reads it first in step 1 of the cycle in
> [AGENTS.md](../../AGENTS.md), instead of opening DECISIONS.md, the issues and the design every time (D-047).
> Every line cites its source and nothing is written without one (rule 12). If a line disagrees with its source, the
> source wins (rule 11): tell the member. Every pull request of the component updates the lines of its own sub-issue,
> and the member who merges it checks that it did.

| | |
|---|---|
| **Component** | Cn Name · module `cn-name` · versions in [agents/components.md](../../agents/components.md) |
| **Iteration** | Itn · Milestone n · component issue #n |
| **Last update** | dd/mm/yyyy, member, pull request #n |

## Decisions that apply
Number and one line only. Read the full entry (`grep -n "^## D-0" DECISIONS.md`) only if the task needs it.
- D-0xx – …
- AN-Cn Dxx – … (`doc/AN-Cn-DECISIONS.md`)

## Contracts (section 6 of #n)
| Operation | In one line |
|---|---|
| `operation(requester, …)` | … |

## Approved design (`doc/design/`)
| Side | Classes | File |
|---|---|---|
| common | … | `common/Cn-common.puml` |
| server | … | `Cn-server-design.puml` |
| client | … | `Cn-client-design.puml` |

## Sub-issues
Status: *open* · *PR #n open* · *merged in #n* · *deferred (Itn)*.
| Issue | Branch | Status |
|---|---|---|
| #n UCxx – Server (Cn x.y.0) | `feature/UCxx-server` | … |

## Links
- Use case issues: #n · analysis: `doc/analysis/…` · use cases: `doc/use-cases/Cn-use-cases.puml`

## Discrepancies between sources
- None found. (Write here what does not match, with both sources, until the team fixes it.)
