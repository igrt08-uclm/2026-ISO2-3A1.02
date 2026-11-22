package es.uclm.esi.iso2.common.orders.contracts;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.orders.domain.DiagnosticOrder;
import es.uclm.esi.iso2.common.shared.ValidationException;

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
}
