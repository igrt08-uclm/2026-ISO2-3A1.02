package es.uclm.esi.iso2.server.orders.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import es.uclm.esi.iso2.common.orders.domain.ClinicalOrder;
import es.uclm.esi.iso2.common.orders.domain.DiagnosticOrder;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link InMemoryClinicalOrderRepository} (TC06S-43 of the test design of UC06). */
@DisplayName("InMemoryClinicalOrderRepository")
class InMemoryClinicalOrderRepositoryTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    private InMemoryClinicalOrderRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryClinicalOrderRepository();
    }

    private static DiagnosticOrder order(String orderId, String patientId) {
        return new DiagnosticOrder(orderId, patientId, "TT-01", "mgarcia", "Chest pain", "Rule out pneumonia", TODAY,
                "URGENT");
    }

    @Test
    @DisplayName("TC06S-43a an unknown id gives null")
    void unknownId() {
        assertNull(repository.findById("ORD-9999"));
    }

    @Test
    @DisplayName("TC06S-43b the orders of one patient are returned and the others are not")
    void ordersOfOnePatient() {
        ClinicalOrder first = order("ORD-0001", "P-0001");
        ClinicalOrder other = order("ORD-0002", "P-0002");
        repository.save(first);
        repository.save(other);
        List<ClinicalOrder> found = repository.findByPatient("P-0001");
        assertAll(
                () -> assertEquals(1, found.size()),
                () -> assertSame(first, found.get(0)),
                () -> assertEquals(List.of(other), repository.findByPatient("P-0002")),
                () -> assertEquals(List.of(), repository.findByPatient("P-9999")));
    }

    @Test
    @DisplayName("TC06S-43c saving the same id twice replaces the order")
    void saveReplaces() {
        ClinicalOrder first = order("ORD-0001", "P-0001");
        ClinicalOrder second = order("ORD-0001", "P-0002");
        repository.save(first);
        repository.save(second);
        assertAll(
                () -> assertSame(second, repository.findById("ORD-0001")),
                () -> assertEquals(1, repository.findByPatient("P-0001").size() + repository.findByPatient("P-0002")
                        .size()));
    }
}
