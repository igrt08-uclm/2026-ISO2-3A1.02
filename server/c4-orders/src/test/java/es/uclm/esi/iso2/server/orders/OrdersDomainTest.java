package es.uclm.esi.iso2.server.orders;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import es.uclm.esi.iso2.common.orders.domain.DiagnosticOrder;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeed;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeedStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of the data classes of C4 in {@code common} (TC06S-41 and TC06S-42 of the test design of UC06). */
@DisplayName("Clinical orders data")
class OrdersDomainTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    @Test
    @DisplayName("TC06S-41 an order keeps the data given to its constructor")
    void orderKeepsItsData() {
        DiagnosticOrder order = new DiagnosticOrder("ORD-0001", "P-0001", "TT-01", "mgarcia", "Chest pain",
                "Rule out pneumonia", TODAY, "URGENT");
        assertAll(
                () -> assertEquals("ORD-0001", order.getOrderId()),
                () -> assertEquals("P-0001", order.getPatientId()),
                () -> assertEquals("TT-01", order.getTestTypeId()),
                () -> assertEquals("mgarcia", order.getRequester()),
                () -> assertEquals("Chest pain", order.getIndication()),
                () -> assertEquals("Rule out pneumonia", order.getJustification()),
                () -> assertEquals(TODAY, order.getCreationDate()),
                () -> assertEquals("URGENT", order.getPriorityId()));
    }

    @Test
    @DisplayName("TC06S-41 an order created without id, requester or date keeps them null")
    void orderWithoutServerData() {
        DiagnosticOrder order = new DiagnosticOrder(null, "P-0001", "TT-01", null, "Chest pain",
                "Rule out pneumonia", null, "URGENT");
        assertAll(
                () -> assertNull(order.getOrderId()),
                () -> assertNull(order.getRequester()),
                () -> assertNull(order.getCreationDate()));
    }

    @Test
    @DisplayName("TC06S-41 a scheduling need keeps the data given to its constructor")
    void needKeepsItsData() {
        SchedulingNeed need = new SchedulingNeed("NEED-0001", "ORD-0001", TODAY, SchedulingNeedStatus.PENDING);
        assertAll(
                () -> assertEquals("NEED-0001", need.getNeedId()),
                () -> assertEquals("ORD-0001", need.getOrderId()),
                () -> assertEquals(TODAY, need.getDueDate()),
                () -> assertEquals(SchedulingNeedStatus.PENDING, need.getStatus()));
    }

    @Test
    @DisplayName("TC06S-42 the status has the two values pending and booked")
    void statusValues() {
        assertEquals(List.of(SchedulingNeedStatus.PENDING, SchedulingNeedStatus.BOOKED),
                List.of(SchedulingNeedStatus.values()));
    }
}
