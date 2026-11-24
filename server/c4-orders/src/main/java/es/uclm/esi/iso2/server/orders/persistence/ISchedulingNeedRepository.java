package es.uclm.esi.iso2.server.orders.persistence;

import es.uclm.esi.iso2.common.orders.domain.SchedulingNeed;
import java.util.List;

/** Where the scheduling needs of the orders are kept (UC06). C4 is the only component that writes them (D-023). */
public interface ISchedulingNeedRepository {
    /**
     * Finds a scheduling need by its id.
     *
     * @param needId the id, for example {@code NEED-0001}
     * @return the scheduling need, or {@code null} if there is none
     */
    SchedulingNeed findById(String needId);

    /**
     * Finds the scheduling needs that are still pending.
     *
     * @return the pending needs, in the order in which they were first saved; empty if there are none
     */
    List<SchedulingNeed> findPending();

    /**
     * Saves a scheduling need: it is added, or it replaces the one with the same {@code needId}.
     *
     * @param need the scheduling need to save
     */
    void save(SchedulingNeed need);
}
