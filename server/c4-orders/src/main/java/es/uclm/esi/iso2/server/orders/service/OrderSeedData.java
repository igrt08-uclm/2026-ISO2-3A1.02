package es.uclm.esi.iso2.server.orders.service;

import es.uclm.esi.iso2.common.orders.domain.DiagnosticOrder;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeed;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeedStatus;
import java.time.LocalDate;
import java.util.List;

/** The orders and scheduling needs that the server loads when it starts with no data (UC06, UC13). Invented examples. */
final class OrderSeedData {
    private OrderSeedData() {
    }

    /** Four orders with their scheduling needs: two BOOKED, two PENDING. */
    static List<DiagnosticOrder> orders(LocalDate day) {
        return List.of(
                new DiagnosticOrder("ORD-0001", "P-0001", "CT-HEAD",
                        "mgarcia", "Headache after a fall", "Rule out bleeding",
                        day, "PREFERENTIAL"),
                new DiagnosticOrder("ORD-0002", "P-0002", "CT-HEAD",
                        "mgarcia", "Persistent dizziness", "Rule out a lesion",
                        day, "ROUTINE"),
                new DiagnosticOrder("ORD-0003", "P-0003", "CT-HEAD",
                        "mgarcia", "Sudden severe headache", "Rule out a haemorrhage",
                        day, "URGENT"),
                new DiagnosticOrder("ORD-0004", "P-0003", "XRAY-CHEST",
                        "mgarcia", "Persistent cough", "Rule out pneumonia",
                        day, "ROUTINE"));
    }

    /** One pending need per order, due on the creation date, with the status of the appointment. */
    static List<SchedulingNeed> needs() {
        return List.of(
                new SchedulingNeed("NEED-0001", "ORD-0001", LocalDate.now(), SchedulingNeedStatus.BOOKED),
                new SchedulingNeed("NEED-0002", "ORD-0002", LocalDate.now(), SchedulingNeedStatus.PENDING),
                new SchedulingNeed("NEED-0003", "ORD-0003", LocalDate.now(), SchedulingNeedStatus.BOOKED),
                new SchedulingNeed("NEED-0004", "ORD-0004", LocalDate.now(), SchedulingNeedStatus.PENDING));
    }
}
