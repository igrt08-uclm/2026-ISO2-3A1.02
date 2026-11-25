package es.uclm.esi.iso2.server.orders.persistence;

import es.uclm.esi.iso2.common.orders.domain.ClinicalOrder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Keeps the clinical orders in memory (D-030), so they are lost when the program closes. */
public class InMemoryClinicalOrderRepository implements IClinicalOrderRepository {
    private final Map<String, ClinicalOrder> orders = new LinkedHashMap<>();

    @Override
    public ClinicalOrder findById(String orderId) {
        return orders.get(orderId);
    }

    @Override
    public List<ClinicalOrder> findByPatient(String patientId) {
        return orders.values().stream()
                .filter(order -> Objects.equals(order.getPatientId(), patientId))
                .toList();
    }

    @Override
    public void save(ClinicalOrder order) {
        orders.put(order.getOrderId(), order);
    }
}
