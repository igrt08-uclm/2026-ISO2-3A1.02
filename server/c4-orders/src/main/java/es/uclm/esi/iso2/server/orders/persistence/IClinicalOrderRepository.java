package es.uclm.esi.iso2.server.orders.persistence;

import es.uclm.esi.iso2.common.orders.domain.ClinicalOrder;
import java.util.List;

/** Where the clinical orders are kept (UC06). C4 is the only component that writes them (D-023). */
public interface IClinicalOrderRepository {
    /**
     * Finds an order by its id.
     *
     * @param orderId the id, for example {@code ORD-0001}
     * @return the order, or {@code null} if there is none
     */
    ClinicalOrder findById(String orderId);

    /**
     * Finds the orders of a patient.
     *
     * @param patientId the internal id of the patient
     * @return the orders of that patient, in the order in which they were saved; empty if there are none
     */
    List<ClinicalOrder> findByPatient(String patientId);

    /**
     * Saves an order: it is added, or it replaces the one with the same {@code orderId}.
     *
     * @param order the order to save
     */
    void save(ClinicalOrder order);
}
