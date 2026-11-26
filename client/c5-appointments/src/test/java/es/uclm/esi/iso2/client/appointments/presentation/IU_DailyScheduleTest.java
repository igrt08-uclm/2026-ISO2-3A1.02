package es.uclm.esi.iso2.client.appointments.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.AppointmentStatus;
import es.uclm.esi.iso2.common.appointments.domain.ScheduleEntry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of {@link IU_DailySchedule} (UC14 client, C5 1.2.0, #136). Each test names the case of the test design
 * ({@code doc/testing/test-cases/UC14-client-test-design.md}, TC14C-xx) that it checks. The screen uses a double of
 * the service and a fixed clock on 24/11/2026; it is built but never shown.
 */
@DisplayName("IU_DailySchedule")
class IU_DailyScheduleTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-24T08:00:00Z"), ZoneOffset.UTC);
    private static final String SCHEDULE = "Show schedule";
    private static final String ROOM = "Room worklist";
    private static final String PHYSICIAN = "Physician worklist";
    private static final List<String> READINGS = List.of(SCHEDULE, ROOM, PHYSICIAN);
    private static final ScheduleEntry FIRST = line("APT-0001", "Room 1", 9, "Lucía Ejemplo Prueba");
    private static final ScheduleEntry SECOND = line("APT-0002", "Room 1", 10, "Mario Demo Simulado");
    private static final ScheduleEntry THIRD = line("APT-0003", "Room 2", 8, "Lucía Ejemplo Prueba");

    private FakeDailyScheduleService service;
    private int backs;

    @BeforeEach
    void setUp() {
        service = new FakeDailyScheduleService();
        backs = 0;
    }

    @Test
    @DisplayName("TC14C-01 it opens on today with an empty table and reads nothing")
    void opens() {
        // Act
        IU_DailySchedule screen = open();

        // Assert
        assertAll(
                () -> assertEquals("24/11/2026", Screen.field(screen, "Day").getText()),
                () -> assertEquals(0, screen.getScheduleTable().getRowCount()),
                () -> assertTrue(service.calls().isEmpty()));
    }

    @Test
    @DisplayName("TC14C-02 the schedule shows the rows grouped by room and in time order")
    void scheduleOfTwoRooms() {
        // Arrange
        service.putRoom("Room 1", FIRST, SECOND);
        service.putRoom("Room 2", THIRD);
        IU_DailySchedule screen = open();

        // Act
        click(screen, SCHEDULE);

        // Assert
        JTable table = screen.getScheduleTable();
        assertAll(
                () -> assertEquals(List.of("Room", "Start", "End", "Patient", "Test type", "Preparation"),
                        Screen.columns(table)),
                () -> assertEquals(List.of(row("Room 1", "09", "Lucía Ejemplo Prueba"),
                        row("Room 1", "10", "Mario Demo Simulado"), row("Room 2", "08", "Lucía Ejemplo Prueba")),
                        Screen.rows(table)));
    }

    @Test
    @DisplayName("TC14C-03 a schedule with one appointment says it in singular")
    void scheduleWithOneAppointment() {
        // Arrange
        service.putRoom("Room 1", FIRST);
        IU_DailySchedule screen = open();

        // Act
        click(screen, SCHEDULE);

        // Assert
        assertEquals("1 appointment.", screen.getStatusMessage());
    }

    @Test
    @DisplayName("TC14C-04 a day without appointments gives a notice")
    void emptyDay() {
        // Arrange
        IU_DailySchedule screen = open();

        // Act
        click(screen, SCHEDULE);

        // Assert
        assertEmptyNotice(screen, "that day");
    }

    @Test
    @DisplayName("TC14C-05 the worklist of a room keeps the order of the service")
    void roomWorklist() {
        assertWorklistOfTwo(ROOM);
    }

    @Test
    @DisplayName("TC14C-06 the worklist of a room with one appointment says it in singular")
    void roomWorklistWithOne() {
        assertWorklistOfOne(ROOM);
    }

    @Test
    @DisplayName("TC14C-07 an empty worklist of a room gives a notice")
    void emptyRoomWorklist() {
        // Arrange
        IU_DailySchedule screen = open();

        // Act
        click(screen, ROOM);

        // Assert
        assertEmptyNotice(screen, "room");
    }

    @Test
    @DisplayName("TC14C-08 the worklist of a physician keeps the order of the service")
    void physicianWorklist() {
        assertWorklistOfTwo(PHYSICIAN);
    }

    @Test
    @DisplayName("TC14C-09 the worklist of a physician with one appointment says it in singular")
    void physicianWorklistWithOne() {
        assertWorklistOfOne(PHYSICIAN);
    }

    @Test
    @DisplayName("TC14C-10 an empty worklist of a physician gives a notice")
    void emptyPhysicianWorklist() {
        // Arrange
        IU_DailySchedule screen = open();

        // Act
        click(screen, PHYSICIAN);

        // Assert
        assertEmptyNotice(screen, "physician");
    }

    @Test
    @DisplayName("TC14C-11 the day, the room and the physician are sent without spaces around")
    void trimmed() {
        // Arrange
        IU_DailySchedule screen = open();
        Screen.field(screen, "Day").setText("  25/11/2026 ");
        Screen.field(screen, "Room").setText("  Room 1 ");
        Screen.field(screen, "Physician").setText(" mgarcia  ");

        // Act
        READINGS.forEach(button -> click(screen, button));

        // Assert
        assertEquals(List.of("schedule 2026-11-25", "room 2026-11-25 [Room 1]", "physician 2026-11-25 [mgarcia]"),
                service.calls());
    }

    @Test
    @DisplayName("TC14C-12 a badly written day empties the table and sends nothing")
    void badDay() {
        for (String button : READINGS) {
            // Arrange
            service = new FakeDailyScheduleService();
            IU_DailySchedule screen = withList();
            Screen.field(screen, "Day").setText("2026-11-24");

            // Act
            click(screen, button);

            // Assert
            assertAll(button,
                    () -> assertError(screen, "dd/MM/yyyy"),
                    () -> assertEquals(0, screen.getScheduleTable().getRowCount()),
                    () -> assertEquals(1, service.calls().size()));
        }
    }

    @Test
    @DisplayName("TC14C-13 a rejected reading shows its errors and empties the table")
    void rejected() {
        assertEmptiedAfter(fake -> fake.rejectWith("First reason", "Second reason"), "First reason", "Second reason");
    }

    @Test
    @DisplayName("TC14C-14 a failed reading shows the error and empties the table")
    void failed() {
        assertEmptiedAfter(fake -> fake.failWith("Storage unavailable"), "Storage unavailable");
    }

    @Test
    @DisplayName("TC14C-15 a denied reading empties the table and leaves only Back enabled")
    void denied() {
        for (String button : READINGS) {
            // Arrange
            service = new FakeDailyScheduleService();
            IU_DailySchedule screen = withList();
            service.denyAll();

            // Act
            click(screen, button);

            // Assert
            assertAll(button,
                    () -> assertError(screen, "permission"),
                    () -> assertEquals(0, screen.getScheduleTable().getRowCount()),
                    () -> assertEquals(List.of("Back"), Screen.enabledButtons(screen)));
        }
    }

    @Test
    @DisplayName("TC14C-16 Back runs its action")
    void back() {
        // Arrange
        IU_DailySchedule screen = open();

        // Act
        click(screen, "Back");

        // Assert
        assertEquals(1, backs);
    }

    private IU_DailySchedule open() {
        return new IU_DailySchedule(service, "lmartin", () -> backs++, CLOCK);
    }

    /** A screen that already shows one appointment. */
    private IU_DailySchedule withList() {
        service.putRoom("Room 1", FIRST);
        IU_DailySchedule screen = open();
        click(screen, SCHEDULE);
        assertEquals(1, screen.getScheduleTable().getRowCount());
        return screen;
    }

    private void assertWorklistOfTwo(String button) {
        // Arrange
        service.useWorklist(SECOND, FIRST);
        IU_DailySchedule screen = open();

        // Act
        click(screen, button);

        // Assert
        assertAll(
                () -> assertEquals(List.of(row("Room 1", "10", "Mario Demo Simulado"),
                        row("Room 1", "09", "Lucía Ejemplo Prueba")), Screen.rows(screen.getScheduleTable())),
                () -> assertTrue(screen.getStatusMessage().endsWith("2 appointments."), screen.getStatusMessage()));
    }

    private void assertWorklistOfOne(String button) {
        // Arrange
        service.useWorklist(FIRST);
        IU_DailySchedule screen = open();

        // Act
        click(screen, button);

        // Assert
        assertTrue(screen.getStatusMessage().endsWith("1 appointment."), screen.getStatusMessage());
    }

    private void assertEmptiedAfter(Consumer<FakeDailyScheduleService> problem, String... texts) {
        for (String button : READINGS) {
            // Arrange
            service = new FakeDailyScheduleService();
            IU_DailySchedule screen = withList();
            problem.accept(service);

            // Act
            click(screen, button);

            // Assert
            assertAll(button,
                    () -> {
                        for (String text : texts) {
                            assertError(screen, text);
                        }
                    },
                    () -> assertEquals(0, screen.getScheduleTable().getRowCount()));
        }
    }

    private static void assertEmptyNotice(IU_DailySchedule screen, String about) {
        String message = screen.getStatusMessage();
        assertAll(
                () -> assertEquals(0, screen.getScheduleTable().getRowCount()),
                () -> assertTrue(message.startsWith("There are no appointments"), message),
                () -> assertTrue(message.contains(about), message));
    }

    private static void assertError(IU_DailySchedule screen, String text) {
        String message = screen.getStatusMessage();
        assertTrue(message.startsWith("⚠") && message.contains(text), message);
    }

    private static void click(IU_DailySchedule screen, String button) {
        Screen.button(screen, button).doClick();
    }

    private static List<String> row(String room, String hour, String patient) {
        return List.of(room, hour + ":00", hour + ":15", patient, "Chest X-ray", "Remove metal objects");
    }

    private static ScheduleEntry line(String id, String room, int hour, String patient) {
        LocalDateTime start = LocalDateTime.of(2026, 11, 24, hour, 0);
        Appointment appointment = new Appointment(id, "NEED-" + id, "P-0001", "XRAY-CHEST", room, "mgarcia", start,
                start.plusMinutes(15), AppointmentStatus.BOOKED);
        return new ScheduleEntry(appointment, patient, "Chest X-ray", "Remove metal objects");
    }
}
