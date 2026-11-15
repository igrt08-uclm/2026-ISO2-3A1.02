package es.uclm.esi.iso2.common.patients.domain;

import java.time.LocalDate;

/**
 * The demographic data of a patient (UC12). The server gives the {@code patientId}; the {@code identifier} is the
 * health card number, which is unique and detects the duplicates. A patient is never deleted: in a later iteration the
 * discharge will deactivate the record.
 */
public class Patient {
    private final String patientId;
    private final String identifier;
    private final String nationalId;
    private final String fullName;
    private final LocalDate dateOfBirth;
    private final Sex sex;
    private final String address;
    private final String phone;
    private final String email;
    private final boolean active;

    /**
     * Creates the patient.
     *
     * @param patientId the internal id, given by the server ({@code P-0001}); it is {@code null} or empty before
     *        the registration
     * @param identifier the health card number, unique
     * @param nationalId the national ID, optional because not every patient has one
     * @param fullName the full name
     * @param dateOfBirth the date of birth, never in the future
     * @param sex the sex
     * @param address the address, optional
     * @param phone the phone, optional; if given it has 9 to 15 digits
     * @param email the e-mail, optional; if given it has the form {@code name@domain.ext}
     * @param active whether the record is active
     */
    public Patient(String patientId, String identifier, String nationalId, String fullName, LocalDate dateOfBirth,
            Sex sex, String address, String phone, String email, boolean active) {
        this.patientId = patientId;
        this.identifier = identifier;
        this.nationalId = nationalId;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.sex = sex;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.active = active;
    }

    /** Returns the internal id, given by the server. */
    public String getPatientId() {
        return patientId;
    }

    /** Returns the health card number. */
    public String getIdentifier() {
        return identifier;
    }

    /** Returns the national ID, which can be empty. */
    public String getNationalId() {
        return nationalId;
    }

    /** Returns the full name. */
    public String getFullName() {
        return fullName;
    }

    /** Returns the date of birth. */
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    /** Returns the sex. */
    public Sex getSex() {
        return sex;
    }

    /** Returns the address, which can be empty. */
    public String getAddress() {
        return address;
    }

    /** Returns the phone, which can be empty. */
    public String getPhone() {
        return phone;
    }

    /** Returns the e-mail, which can be empty. */
    public String getEmail() {
        return email;
    }

    /** Tells whether the record is active. */
    public boolean isActive() {
        return active;
    }
}
