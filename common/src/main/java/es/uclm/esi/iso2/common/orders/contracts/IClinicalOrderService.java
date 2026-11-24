package es.uclm.esi.iso2.common.orders.contracts;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.orders.domain.ClinicalOrder;
import es.uclm.esi.iso2.common.orders.domain.DiagnosticOrder;
import es.uclm.esi.iso2.common.orders.domain.FollowUpOrder;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeed;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.util.List;

/**
 * Orders diagnostic procedures and keeps their scheduling needs for C5 (UC06). Ordering needs the permission
 * {@code ORDER_PROCEDURES} of the requester; the operations that C5 uses also accept {@code MANAGE_APPOINTMENTS}.
 * Every denied attempt and every created order is registered in the audit log. An order is never edited, cancelled
 * or deleted (D-039).
 */
public interface IClinicalOrderService {
    /**
     * Creates a diagnostic order for an active patient and an active test type, with one of the active configured
     * priorities, and one pending scheduling need for it. The server gives the {@code orderId}, takes the requester
     * as the physician who asked for it and today as the creation date.
     *
     * @param requester the username of the physician who orders
     * @param order the data of the order; its {@code orderId}, {@code requester} and {@code creationDate} are
     *        ignored
     * @return the created order, with its {@code orderId}, {@code requester} and {@code creationDate}
     * @throws AccessDeniedException if the requester does not have {@code ORDER_PROCEDURES}
     * @throws ValidationException with every reason if the order is not valid; nothing is saved
     */
    DiagnosticOrder orderDiagnosticProcedure(String requester, DiagnosticOrder order)
            throws AccessDeniedException, ValidationException;

    /**
     * Creates a periodic follow-up order for an active patient and a test type eligible for follow-up, with its
     * clinical indication, its justification and a positive recurrence interval, and the pending scheduling needs of
     * the next 12 months (D-040). The server gives the {@code orderId}, takes the requester as the physician who asked
     * for it and today as the creation date.
     *
     * @param requester the username of the physician who orders
     * @param order the data of the order; its {@code orderId}, {@code requester} and {@code creationDate} are
     *        ignored
     * @return the created order, with its {@code orderId}, {@code requester} and {@code creationDate}
     * @throws AccessDeniedException if the requester does not have {@code ORDER_PROCEDURES}
     * @throws ValidationException with every reason if the order is not valid; nothing is saved
     */
    FollowUpOrder orderFollowUp(String requester, FollowUpOrder order)
            throws AccessDeniedException, ValidationException;

    /**
     * Returns the orders of a patient, diagnostic and follow-up ones, in the order in which they were created.
     *
     * @param requester the username of the physician who asks
     * @param patientId the internal id of the patient
     * @return the orders of the patient; empty if there are none or the patient is unknown
     * @throws AccessDeniedException if the requester does not have {@code ORDER_PROCEDURES}
     * @throws ValidationException if the patient id is not given
     */
    List<ClinicalOrder> getOrdersOfPatient(String requester, String patientId)
            throws AccessDeniedException, ValidationException;

    /**
     * Returns the scheduling needs that are still pending, for C5 to book them (UC13).
     *
     * @param requester the username of the user who asks
     * @return the pending needs, in the order in which they were created
     * @throws AccessDeniedException if the requester has neither {@code ORDER_PROCEDURES} nor
     *         {@code MANAGE_APPOINTMENTS}
     */
    List<SchedulingNeed> getPendingSchedulingNeeds(String requester) throws AccessDeniedException;

    /**
     * Marks a pending scheduling need as booked, when C5 books its appointment (UC13).
     *
     * @param requester the username of the user who books
     * @param needId the id of the scheduling need
     * @throws AccessDeniedException if the requester has neither {@code ORDER_PROCEDURES} nor
     *         {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException if there is no such need or it is already booked
     */
    void markSchedulingNeedBooked(String requester, String needId)
            throws AccessDeniedException, ValidationException;

    /**
     * Returns one order, diagnostic or follow-up, for example the one of a scheduling need.
     *
     * @param requester the username of the user who asks
     * @param orderId the id of the order
     * @return the order
     * @throws AccessDeniedException if the requester has neither {@code ORDER_PROCEDURES} nor
     *         {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException if there is no order with that id
     */
    ClinicalOrder getOrder(String requester, String orderId) throws AccessDeniedException, ValidationException;

    /**
     * Marks a booked scheduling need as pending again, when C5 cancels its appointment (D-041).
     *
     * @param requester the username of the user who cancels
     * @param needId the id of the scheduling need
     * @throws AccessDeniedException if the requester has neither {@code ORDER_PROCEDURES} nor
     *         {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException if there is no such need or it is already pending
     */
    void markSchedulingNeedPending(String requester, String needId)
            throws AccessDeniedException, ValidationException;
}
