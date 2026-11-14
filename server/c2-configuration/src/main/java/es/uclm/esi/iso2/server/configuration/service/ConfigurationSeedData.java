package es.uclm.esi.iso2.server.configuration.service;

import es.uclm.esi.iso2.common.configuration.domain.AppointmentPolicy;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.Protocol;
import es.uclm.esi.iso2.common.configuration.domain.Schedule;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/** The data loaded when the system starts with no data (D-003). They are invented examples, not real data. */
final class ConfigurationSeedData {
    private ConfigurationSeedData() {
    }

    /** The three priorities of the orders: urgent, preferential and routine. */
    static List<Priority> priorities() {
        return List.of(
                new Priority("URGENT", "Urgent", "To be done as soon as possible, within hours", true),
                new Priority("PREFERENTIAL", "Preferential", "To be done within a few days", true),
                new Priority("ROUTINE", "Routine", "To be done within a few weeks", true));
    }

    /** The six examinations of the catalogue, each one with every field of FR17. */
    static List<TestType> testTypes() {
        return List.of(
                new TestType("XRAY-CHEST", "Chest X-ray", "X-ray", "Room RX-1", 10,
                        "Remove the metallic objects of the chest", false, false,
                        "Not recommended during pregnancy unless it is essential", "ROUTINE", true, true),
                new TestType("CT-HEAD", "Head CT", "CT", "Room CT-1", 20,
                        "Fasting for 4 hours if contrast is used", true, true,
                        "Check the kidney function and the allergies before the contrast", "PREFERENTIAL", false,
                        true),
                new TestType("MRI-KNEE", "Knee MRI", "MRI", "Room MRI-1", 40,
                        "Remove every metallic object", false, true,
                        "Not allowed with pacemakers or implants that are not compatible", "ROUTINE", true, true),
                new TestType("US-ABDOMEN", "Abdominal ultrasound", "Ultrasound", "Room US-1", 25,
                        "Fasting for 6 hours and a full bladder", false, false, "No radiation", "ROUTINE", true,
                        true),
                new TestType("MAMMO-BILATERAL", "Bilateral mammography", "Mammography", "Room MG-1", 20,
                        "Do not use creams or deodorant", false, false,
                        "Not recommended during pregnancy unless it is essential", "PREFERENTIAL", true, true),
                new TestType("DEXA-SPINE", "Bone densitometry", "Densitometry", "Room DX-1", 15,
                        "No calcium supplements during the previous 24 hours", false, false,
                        "Low radiation dose", "ROUTINE", true, true));
    }

    /** One example of protocol. */
    static List<Protocol> protocols() {
        return List.of(new Protocol("PROT-CT-HEAD", "Head CT with contrast",
                "Check the creatinine, place the venous line, inject the contrast and scan from the base of the "
                        + "skull to the vertex",
                true));
    }

    /** One example of schedule: the CT room is open from Monday to Friday. */
    static List<Schedule> schedules() {
        return List.of(new Schedule("Room CT-1",
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY),
                LocalTime.of(8, 0), LocalTime.of(15, 0), true));
    }

    /** One example of appointment policy. */
    static List<AppointmentPolicy> policies() {
        return List.of(new AppointmentPolicy("POL-CANCEL", "Cancellation notice",
                "An appointment can be cancelled or rescheduled up to 24 hours before it", true));
    }
}
