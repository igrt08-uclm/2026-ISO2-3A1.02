package es.uclm.esi.iso2.common.appointments.domain;

import java.time.LocalDateTime;

/**
 * A patient waiting for an appointment of a test type, ordered by priority tier and by the moment it was added
 * (UC15, D42). It is created from a pending scheduling need of C4. It is never deleted: removing it changes its status
 * to {@link WaitlistStatus#REMOVED} and filling an opening changes it to {@link WaitlistStatus#BOOKED} (D-022).
 */
public class WaitlistEntry {
    private final String entryId;
    private final String needId;
    private final String patientId;
    private final String testTypeId;
    private final String priorityId;
    private final LocalDateTime addedAt;
    private final WaitlistStatus status;

    /**
     * Creates the entry of the waitlist.
     *
     * @param entryId the id given by the server ({@code WL-0001})
     * @param needId the id of the scheduling need of C4 that waits
     * @param patientId the internal id of the patient (C3)
     * @param testTypeId the id of the test type of the catalogue (C2)
     * @param priorityId the id of the priority tier of the entry (C2)
     * @param addedAt the moment when the patient was added to the waitlist
     * @param status the state of the entry
     */
    public WaitlistEntry(String entryId, String needId, String patientId, String testTypeId, String priorityId,
            LocalDateTime addedAt, WaitlistStatus status) {
        this.entryId = entryId;
        this.needId = needId;
        this.patientId = patientId;
        this.testTypeId = testTypeId;
        this.priorityId = priorityId;
        this.addedAt = addedAt;
        this.status = status;
    }

    /** Returns the id given by the server. */
    public String getEntryId() {
        return entryId;
    }

    /** Returns the id of the scheduling need of C4 that waits. */
    public String getNeedId() {
        return needId;
    }

    /** Returns the internal id of the patient. */
    public String getPatientId() {
        return patientId;
    }

    /** Returns the id of the test type of the catalogue. */
    public String getTestTypeId() {
        return testTypeId;
    }

    /** Returns the id of the priority tier of the entry. */
    public String getPriorityId() {
        return priorityId;
    }

    /** Returns the moment when the patient was added to the waitlist. */
    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    /** Returns the state of the entry. */
    public WaitlistStatus getStatus() {
        return status;
    }
}
