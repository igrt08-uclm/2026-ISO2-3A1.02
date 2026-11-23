package es.uclm.esi.iso2.server.appointments.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the read methods of {@link InMemoryAppointmentRepository} that {@code AppointmentService} does not use in
 * UC13 (C5 1.0.0, #131). Each test names its case of `doc/testing/test-cases/UC13-server-test-design.md` (TC13S-xx);
 * the expected results come from the Javadoc of {@link IAppointmentRepository}. The repository holds five booked
 * appointments around the range from 24/11/2026 to 01/12/2026.
 */
@DisplayName("InMemoryAppointmentRepository")
class InMemoryAppointmentRepositoryTest {
    private static final LocalDate FROM = LocalDate.of(2026, 11, 24);
    private static final LocalDate TO = LocalDate.of(2026, 12, 1);

    private InMemoryAppointmentRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAppointmentRepository();
        repository.save(appointment("APT-0001", "Room 1", "mgarcia", FROM.minusDays(1)));
        repository.save(appointment("APT-0002", "Room 1", "mgarcia", FROM));
        repository.save(appointment("APT-0003", "Room 2", "jfernandez", FROM));
        repository.save(appointment("APT-0004", "Room 1", "mgarcia", TO));
        repository.save(appointment("APT-0005", "Room 1", "mgarcia", TO.plusDays(1)));
    }

    private static Appointment appointment(String id, String room, String physician, LocalDate day) {
        LocalDateTime start = LocalDateTime.of(day, LocalTime.of(9, 0));
        return new Appointment(id, "NEED-" + id.substring(4), "P-0001", "TT-01", room, physician, start,
                start.plusMinutes(15), AppointmentStatus.BOOKED);
    }

    private static List<String> idsOf(List<Appointment> found) {
        return found.stream().map(Appointment::getAppointmentId).toList();
    }

    @Test
    @DisplayName("TC13S-64 the range of a room includes both limits and nothing outside them or of another room")
    void findByRoomAndDateRange() {
        assertAll(
                () -> assertEquals(List.of("APT-0002", "APT-0004"),
                        idsOf(repository.findByRoomAndDateRange("Room 1", FROM, TO))),
                () -> assertEquals(List.of("APT-0003"), idsOf(repository.findByRoomAndDateRange("Room 2", FROM, TO))),
                () -> assertTrue(repository.findByRoomAndDateRange("Room 3", FROM, TO).isEmpty()));
    }

    @Test
    @DisplayName("TC13S-65 the appointments of a day are those of every room on that day only")
    void findByDate() {
        assertAll(
                () -> assertEquals(List.of("APT-0002", "APT-0003"), idsOf(repository.findByDate(FROM))),
                () -> assertTrue(repository.findByDate(FROM.plusDays(1)).isEmpty()));
    }

    @Test
    @DisplayName("TC13S-66 the appointments of a day of a physician leave out the other physicians and days")
    void findByDateAndPhysician() {
        assertAll(
                () -> assertEquals(List.of("APT-0002"), idsOf(repository.findByDateAndPhysician(FROM, "mgarcia"))),
                () -> assertEquals(List.of("APT-0003"),
                        idsOf(repository.findByDateAndPhysician(FROM, "jfernandez"))),
                () -> assertTrue(repository.findByDateAndPhysician(FROM.plusDays(1), "mgarcia").isEmpty()));
    }
}
