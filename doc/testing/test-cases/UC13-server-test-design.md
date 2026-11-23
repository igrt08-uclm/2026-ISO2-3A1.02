# Test design – UC13 – Server (#131)

| | |
|---|---|
| **Use case** | #129 *UC13 – Manage the appointment schedule (C5 v1.0.0)* · FR13 |
| **Sub-issue** | #131 *UC13 – Server (C5 v1.0.0)* · branch `feature/UC13-server-tests` (from `feature/UC13-server`) |
| **Plan** | `doc/plans/UC13-server.md` |
| **Specification** | #131; `doc/plans/C5-design.md` (contract and model of C5); D37, D38, D40, D44 |
| **Designed by** | Ignacio Romero Torres, 23/11/2026 — for the review of Adam (Testing) |

## Test data
- **Clock:** fixed at 24/11/2026 08:00 (a Tuesday), so Room 1 (Tuesdays from 09:00 to 11:00) is in the future.
- **Users (C1):** `lmartin` active with `MANAGE_APPOINTMENTS`; `admin` and `mgarcia` active without it; `pnavarro`
  inactive; `nobody` does not exist.
- **Patients (C3):** `P-0001` and `P-0002` active; `P-0004` inactive.
- **Test types and schedules (C2):** `TT-01` active, Room 1, 15 minutes, schedule Tuesdays 09:00-11:00 (eight slots);
  `TT-02` active, Room 2, 30 minutes; `TT-03` inactive.
- **C4:** a pending need `NEED-0001` of the diagnostic order `ORD-0001` (patient `P-0001`, `TT-01`, priority `URGENT`,
  ordered by `mgarcia` on 23/11/2026, due 23/11/2026).

## Equivalence classes and boundary values
| Parameter | Classes / values | Cases |
|---|---|---|
| requester | with `MANAGE_APPOINTMENTS` · without it · inactive · unknown · `null` | 01-03 |
| test type | active with schedule · active without schedule that day · inactive · unknown · blank | 03-05, 08, 15 |
| date range | one day · a day without schedule · range ending before it starts · `null` | 03, 04, 06, 07 |
| need | pending · not pending (booked or unknown) · blank | 09-12, 13 |
| slot | free · of another room · outside the schedule · in the past · taken · `null` | 09, 15-18, 14 |
| patient | active · inactive | 09, 19 |
| appointment | booked · rescheduled · cancelled · unknown · blank | 21, 25, 22, 23b, 23 |

## Cases
| Case | What it checks | Expected result |
|---|---|---|
| TC13S-01 | A user without `MANAGE_APPOINTMENTS` tries the reads, a booking and a cancellation | `AccessDeniedException` and `APPOINTMENT_ACCESS_DENIED` in each one |
| TC13S-02 | A denied attempt | C2, C3 and C4 are not called and nothing is saved |
| TC13S-03 | The free slots of a Tuesday for `TT-01` | Eight slots from 09:00 to 11:00, one every 15 minutes, in Room 1 |
| TC13S-04 | A day without schedule (Wednesday) | No slots |
| TC13S-05 | A booked slot and, after cancelling, the same slot | The booked one is not offered (seven left); after the cancellation there are eight again |
| TC13S-06 | A range that ends before it starts | `The last date cannot be before the first one` |
| TC13S-07 | A `null` range | `The date range is required` |
| TC13S-08 | An inactive test type | `The test type TT-03 is not active` |
| TC13S-09 | A valid booking | `APT-0001` with the need, the patient, the test type, the room, the physician of the order and the status `BOOKED` |
| TC13S-10 | The booking and C4 | The need is `BOOKED` in C4 and the audit has `APPOINTMENT_BOOKED` on `APPOINTMENT:APT-0001` |
| TC13S-11 | A second booking | `APT-0002` |
| TC13S-12 | A need that is not pending | `The scheduling need NEED-9999 is not pending` |
| TC13S-13 | A blank need | `The scheduling need is required` |
| TC13S-14 | A `null` slot | `The slot is required` |
| TC13S-15 | A slot of another room | `The room Room 2 is not the room of the test type TT-01` |
| TC13S-16 | A slot before the schedule | `The slot 24/11/2026 08:00 is not inside the schedule of the room Room 1` |
| TC13S-17 | A slot of yesterday | The errors include `The slot 23/11/2026 09:00 is in the past` |
| TC13S-18 | A slot already booked | `The slot 24/11/2026 09:00 is not free` |
| TC13S-19 | An inactive patient | `The patient P-0004 is not active` |
| TC13S-20 | A rejected booking | Nothing saved, the need still pending and no audit entry |
| TC13S-21 | Moving a booked appointment to 10:00 | Same id and need, status `RESCHEDULED`, both audit entries, seven free slots (the old one is free again) |
| TC13S-22 | Moving a cancelled appointment | `The appointment APT-0001 is cancelled` |
| TC13S-23 | A blank appointment id | `The appointment is required` |
| TC13S-23b | An unknown appointment id | `There is no appointment with the id APT-9999` |
| TC13S-24 | Moving to a taken slot | `The slot 24/11/2026 10:00 is not free` and the appointment keeps its slot |
| TC13S-25 | Cancelling | Status `CANCELLED`, the need `PENDING` again in C4 and `APPOINTMENT_CANCELLED` |
| TC13S-26 | Cancelling twice | `The appointment APT-0001 is already cancelled` |
| TC13S-27 | Cancelling an appointment whose need is pending already | The cancellation succeeds (C4 answers that it was pending) |
| TC13S-28 | The two reads | `APPOINTMENT_ACCESSED` with `appointments` and with `PATIENT:P-0001`, and the appointments returned |
| TC13S-29 | A blank patient id in the read | `The patient is required` |
| TC13S-30 | The pending needs view | The need with its order, the patient name, the test type name, the priority and the due date |
| TC13S-31 | After a booking | The need is not among the pending ones |
| TC13S-32 | The appointments of a patient without appointments | Empty list |

## Map to the coverage of D-033
- Every line of `AppointmentService` is covered except the branch of an `AccessDeniedException` of C2 inside
  `schedulesOf`, which cannot happen with `MANAGE_APPOINTMENTS` (#174, #175).
- `common` keeps its coverage with `AppointmentModelTest`, which travels with the implementation branch of #131.

## Not covered here
- The client of UC13 (#130): its tests belong to its own design.
- UC15 (#134) and UC14 (#137): their services do not exist yet.
