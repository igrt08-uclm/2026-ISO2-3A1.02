package es.uclm.esi.iso2.server.appointments.service;

import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.AppointmentStatus;
import es.uclm.esi.iso2.common.appointments.domain.TimeSlot;
import es.uclm.esi.iso2.common.appointments.domain.WaitlistEntry;
import es.uclm.esi.iso2.common.appointments.domain.WaitlistStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** The appointments that the server loads when it starts with no data (UC13). They are invented examples. */
final class AppointmentSeedData {
    private AppointmentSeedData() {
    }

    /** Three appointments with every status: BOOKED, CANCELLED and RESCHEDULED. */
    static List<Appointment> appointments(LocalDate day) {
        return List.of(
                new Appointment("APT-0001", "NEED-0001", "P-0001", "CT-HEAD", "Room CT-1",
                        "mgarcia",
                        LocalDateTime.of(day, LocalTime.of(9, 0)),
                        LocalDateTime.of(day, LocalTime.of(9, 20)),
                        AppointmentStatus.BOOKED),
                new Appointment("APT-0002", "NEED-0002", "P-0002", "CT-HEAD", "Room CT-1",
                        "mgarcia",
                        LocalDateTime.of(day, LocalTime.of(9, 20)),
                        LocalDateTime.of(day, LocalTime.of(9, 40)),
                        AppointmentStatus.CANCELLED),
                new Appointment("APT-0003", "NEED-0003", "P-0003", "CT-HEAD", "Room CT-1",
                        "mgarcia",
                        LocalDateTime.of(day, LocalTime.of(10, 0)),
                        LocalDateTime.of(day, LocalTime.of(10, 20)),
                        AppointmentStatus.RESCHEDULED));
    }

    /** Two patients waiting for different test types, added the same morning, with pending needs of C4 (UC15). */
    static List<WaitlistEntry> waitlistEntries(LocalDate day) {
        return List.of(
                new WaitlistEntry("WL-0001", "NEED-0002", "P-0002", "CT-HEAD", "URGENT",
                        LocalDateTime.of(day, LocalTime.of(8, 0)), WaitlistStatus.WAITING),
                new WaitlistEntry("WL-0002", "NEED-0004", "P-0003", "XRAY-CHEST", "ROUTINE",
                        LocalDateTime.of(day, LocalTime.of(8, 5)), WaitlistStatus.WAITING));
    }
}
