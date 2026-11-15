package es.uclm.esi.iso2.server.patients.persistence;

import es.uclm.esi.iso2.common.patients.domain.Patient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Keeps the patients in memory (D-030), so they are lost when the program closes. */
public class InMemoryPatientRepository implements IPatientRepository {
    private final Map<String, Patient> patients = new LinkedHashMap<>();

    @Override
    public Patient findById(String patientId) {
        return patients.get(patientId);
    }

    @Override
    public Patient findByIdentifier(String identifier) {
        String wanted = normalized(identifier);
        for (Patient patient : patients.values()) {
            if (normalized(patient.getIdentifier()).equals(wanted)) {
                return patient;
            }
        }
        return null;
    }

    @Override
    public List<Patient> search(String filters) {
        String text = normalized(filters);
        return patients.values().stream()
                .filter(patient -> text.isEmpty() || normalized(patient.getIdentifier()).contains(text)
                        || normalized(patient.getFullName()).contains(text))
                .toList();
    }

    @Override
    public void save(Patient patient) {
        patients.put(patient.getPatientId(), patient);
    }

    private static String normalized(String text) {
        return text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
    }
}
