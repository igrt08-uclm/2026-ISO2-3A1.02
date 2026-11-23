package es.uclm.esi.iso2.common.appointments.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of the data classes of C5 in {@code common} (UC13, #131): they only build and read their data. */
@DisplayName("C5 data classes")
class AppointmentModelTest {
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 24, 9, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 11, 24, 9, 15);

    @Test
    @DisplayName("an appointment keeps its data and its status")
    void appointment() {
        Appointment appointment = new Appointment("APT-0001", "NEED-0001", "P-0001", "TT-01", "Room 1", "mgarcia",
                START, END, AppointmentStatus.BOOKED);
        assertAll(
                () -> assertEquals("APT-0001", appointment.getAppointmentId()),
                () -> assertEquals("NEED-0001", appointment.getNeedId()),
                () -> assertEquals("P-0001", appointment.getPatientId()),
                () -> assertEquals("TT-01", appointment.getTestTypeId()),
                () -> assertEquals("Room 1", appointment.getRoom()),
                () -> assertEquals("mgarcia", appointment.getPhysician()),
                () -> assertEquals(START, appointment.getStart()),
                () -> assertEquals(END, appointment.getEnd()),
                () -> assertEquals(AppointmentStatus.BOOKED, appointment.getStatus()));
    }

    @Test
    @DisplayName("a time slot keeps its room and its moments")
    void timeSlot() {
        TimeSlot slot = new TimeSlot("Room 1", START, END);
        assertAll(
                () -> assertEquals("Room 1", slot.getRoom()),
                () -> assertEquals(START, slot.getStart()),
                () -> assertEquals(END, slot.getEnd()));
    }

    @Test
    @DisplayName("a pending need view keeps the data of the need and of its order")
    void pendingNeedView() {
        LocalDate orderDate = LocalDate.of(2026, 11, 23);
        PendingNeedView view = new PendingNeedView("NEED-0001", "ORD-0001", "P-0001", "Lucía Ejemplo Prueba", "TT-01",
                "Chest X-ray", "URGENT", orderDate, orderDate);
        assertAll(
                () -> assertEquals("NEED-0001", view.getNeedId()),
                () -> assertEquals("ORD-0001", view.getOrderId()),
                () -> assertEquals("P-0001", view.getPatientId()),
                () -> assertEquals("Lucía Ejemplo Prueba", view.getPatientName()),
                () -> assertEquals("TT-01", view.getTestTypeId()),
                () -> assertEquals("Chest X-ray", view.getTestTypeName()),
                () -> assertEquals("URGENT", view.getPriorityId()),
                () -> assertEquals(orderDate, view.getOrderDate()),
                () -> assertEquals(orderDate, view.getDueDate()));
    }

    @Test
    @DisplayName("an appointment has three states")
    void appointmentStatus() {
        assertEquals(3, AppointmentStatus.values().length);
    }
}
