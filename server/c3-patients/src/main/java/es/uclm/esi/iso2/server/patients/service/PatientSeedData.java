package es.uclm.esi.iso2.server.patients.service;

import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.patients.domain.Sex;
import java.time.LocalDate;
import java.util.List;

/**
 * The example patients that the server loads when it starts with no data, so that the components that come next can be
 * tried without registering patients first (UC12). Everything is invented: they are not real people, and the health card
 * numbers do not exist.
 */
final class PatientSeedData {
    private PatientSeedData() {
    }

    /** Three invented patients, without an id: the service gives them theirs. */
    static List<Patient> patients() {
        return List.of(
                new Patient(null, "HC-0000001", "", "Lucía Ejemplo Prueba", LocalDate.of(1980, 3, 14), Sex.FEMALE,
                        "Calle Inventada 1, 13001 Ciudad Real", "600000001", "lucia.ejemplo@example.test", true),
                new Patient(null, "HC-0000002", "", "Mario Demo Simulado", LocalDate.of(1975, 11, 2), Sex.MALE,
                        "Avenida de Muestra 22, 45001 Toledo", "600000002", "mario.demo@example.test", true),
                new Patient(null, "HC-0000003", "", "Elena Muestra Ficticia", LocalDate.of(2001, 7, 25), Sex.FEMALE,
                        "Plaza Ficticia 3, 02001 Albacete", "600000003", "elena.muestra@example.test", true));
    }
}
