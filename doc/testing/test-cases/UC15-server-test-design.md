# Test design · UC15 – Server (C5 v1.1.0) · #134 – Manage waitlists, priority tiers and calendar openings

Cases approved by Javier Villanueva Crespo (26/11/2026) following lesson T.04 (Testing Management): parameters,
equivalence classes, boundary values and error guessing. Expected results come from #134, `doc/plans/UC15-server.md`
(cases and Q7 to Q9), the Javadoc of `IWaitlistService`, design note N5 of `doc/design/C5-server-design.puml`, D-022
and, for the proposal, the answer of Design of 25/11/2026 (Q7).

**Test data (precondition of every case).** Empty `InMemoryWaitlistRepository`, the `AppointmentService` of UC13, the
test doubles of C1 to C4 (`TestDoubles`) and the clock `Clock.fixed(2026-11-24T08:00:00Z, UTC)` (a Tuesday).

- **Users:** `lmartin` has `MANAGE_APPOINTMENTS`; `mgarcia` has not.
- **Tiers, in this order:** `URGENT`, `PREFERENTIAL`, `ROUTINE`, and `OLD` (inactive).
- **Patients:** `P-0001` and `P-0002` active, `P-0004` inactive.
- **Needs of `TT-01` (Room 1, Tuesdays 09:00-11:00, 15 minutes):** `NEED-0001` (`P-0001`), `NEED-0002` (`P-0002`) and
  `NEED-0003` (`P-0004`) pending; `NEED-0004` (`P-0002`) booked.

Messages are checked by their number or by the id they name, not by the whole sentence.

## 1. Parameters and testing values

| Parameter | Equivalence classes | Testing values |
|---|---|---|
| `requester` | With / without the permission | `lmartin`, `mgarcia` |
| `testTypeId` | Given / not given | `TT-01`, `null`, blank |
| `needId` | Pending / booked / already waiting / not given | `NEED-0001`, `NEED-0004`, `NEED-0001` saved, `null` |
| `priorityId` | Active / inactive / unknown / not given | `URGENT`, `OLD`, `NONE`, blank |
| `entryId` | Waiting / booked or removed / unknown / blank | `WL-0001`, `WL-0009`, blank |
| `opening` | Free / taken / not given | Tuesday 09:00, booked 09:00, `null` |
| Range of openings | Valid / reversed (**Limit Val.**) | 24/11 to 24/11, 24/11 to 23/11 |
| Waitlist (proposal) | Empty / first valid / first not valid / none valid | TC15S-23 to TC15S-27 |

## 2. Combination of the values

Each use: one invalid value at a time with the rest valid. Two invalid values together only where the contract asks for
every reason (TC15S-07, 12, 14, 28 and 30).

## 3. Test cases

The tests are in `server/c5-appointments/src/test/java/.../service/WaitlistServiceTest.java`; TC15S-34 is in
`.../persistence/InMemoryWaitlistRepositoryTest.java` and in `common/.../domain/WaitlistModelTest.java`.

| ID | Method | Input (and precondition) | Expected result | Test |
|---|---|---|---|---|
| TC15S-01 | the seven | `mgarcia`, with a waiting entry | Access denied, `WAITLIST_ACCESS_DENIED`, nothing changes | `deniedWithoutPermission` |
| TC15S-02 | `getWaitlist` | Mixed tiers | Ordered by tier | `orderedByTier` |
| TC15S-03 | `getWaitlist` | Same tier | Oldest first | `orderedByDateInsideATier` |
| TC15S-04 | `getWaitlist` | Removed, booked and `TT-02` entries | None returned | `onlyWaitingEntriesOfTheTestType` |
| TC15S-05 | `getWaitlist` | `null`; blank | Validation error | `consultWithoutTestType` |
| TC15S-06 | `addToWaitlist` | `NEED-0001`, `URGENT` | `WL-0001` waiting, `WAITLIST_CHANGED` | `add` |
| TC15S-07 | `addToWaitlist` | `null`, blank | Two errors, nothing saved | `addWithoutData` |
| TC15S-08 | `addToWaitlist` | `NONE` | Validation error | `addWithUnknownTier` |
| TC15S-09 | `addToWaitlist` | `OLD` | Validation error | `addWithInactiveTier` |
| TC15S-10 | `addToWaitlist` | `NEED-0004` | Validation error | `addNeedNotPending` |
| TC15S-11 | `addToWaitlist` | Need already waiting | Validation error | `addNeedAlreadyWaiting` |
| TC15S-12 | `addToWaitlist` | `NEED-0004`, `NONE` | Both errors | `addWithEveryReason` |
| TC15S-13 | `changeWaitlistPriority` | Second entry to `URGENT` | First in the order, date kept, audited | `changeTier` |
| TC15S-14 | `changeWaitlistPriority` | Blank, `null` | Two errors | `changeTierWithoutData` |
| TC15S-15 | `changeWaitlistPriority` | Unknown; booked entry | Validation error | `changeTierOfWrongEntry` |
| TC15S-16 | `changeWaitlistPriority` | `NONE` | Validation error, no change | `changeToInvalidTier` |
| TC15S-17 | `removeFromWaitlist` | Waiting entry | `REMOVED`, kept, `WAITLIST_ENTRY_REMOVED` | `remove` |
| TC15S-18 | `removeFromWaitlist` | Blank | Validation error | `removeWithoutId` |
| TC15S-19 | `removeFromWaitlist` | Unknown; removed entry | Validation error | `removeWrongEntry` |
| TC15S-20 | `getCalendarOpenings` | 24/11 | Free slots of UC13, audited | `openings` |
| TC15S-21 | `getCalendarOpenings` | Blank test type | Validation error | `openingsWithoutTestType` |
| TC15S-22 | `getCalendarOpenings` | Reversed range | Validation error of UC13 | `openingsWithReversedRange` |
| TC15S-23 | `proposePatientForOpening` | First entry valid | That entry | `proposeFirst` |
| TC15S-24 | `proposePatientForOpening` | First patient inactive | The next one | `proposeSkipsInactivePatient` |
| TC15S-25 | `proposePatientForOpening` | First need booked | The next one | `proposeSkipsBookedNeed` |
| TC15S-26 | `proposePatientForOpening` | Nobody waiting | `null` | `proposeWithEmptyWaitlist` |
| TC15S-27 | `proposePatientForOpening` | Nobody valid | `null` | `proposeWithoutValidEntry` |
| TC15S-28 | `proposePatientForOpening` | Blank, `null` | Two errors | `proposeWithoutData` |
| TC15S-29 | `fillOpening` | Waiting entry, free slot | Appointment, need and entry booked, `OPENING_FILLED` | `fill` |
| TC15S-30 | `fillOpening` | Blank, `null` | Two errors | `fillWithoutData` |
| TC15S-31 | `fillOpening` | Unknown; removed entry | Validation error, no appointment | `fillWithWrongEntry` |
| TC15S-32 | `fillOpening` | Slot already booked | Validation error, entry still waiting | `fillTakenOpening` |
| TC15S-33 | `loadSeedData` | Empty, then with data | Loaded once, for `NEED-0002` and `NEED-0004` | `seed` |
| TC15S-34 | model and repository | Save, replace, find, getters | What was saved; `null` if missing | `saveReplaceAndFind`, `waitlistEntry` |

## 4. Coverage and open results

Measured on 26/11/2026, before the fixes of the plan: `WaitlistService` 130/134 lines and 60/62 branches,
`InMemoryWaitlistRepository` 15/15 and 4/4; `server-c5-appointments` 98.2 % of lines and 99.0 % of branches; `common`
87.5 % and 100 %.

Red before step 6 of the plan, green since its fix: TC15S-12, 24, 25, 26, 27 and 33.
