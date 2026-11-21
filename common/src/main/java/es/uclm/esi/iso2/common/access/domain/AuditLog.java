package es.uclm.esi.iso2.common.access.domain;

import java.util.Date;

/** One entry of the audit log. It cannot be modified: entries are only added (D11 of the analysis). */
public class AuditLog {
    private final Long id;
    private final Date timestamp;
    private final String author;
    private final String action;
    private final String targetResource;

    /**
     * Creates an audit log entry.
     *
     * @param id the identifier of the entry
     * @param timestamp the date and time of the action
     * @param author the username of the author who performed the action
     * @param action the action performed
     * @param targetResource the resource affected by the action
     */
    public AuditLog(Long id, Date timestamp, String author, String action, String targetResource) {
        this.id = id;
        this.timestamp = new Date(timestamp.getTime());
        this.author = author;
        this.action = action;
        this.targetResource = targetResource;
    }

    /**
     * Returns the identifier of the entry.
     *
     * @return the identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the date and time of the action.
     *
     * @return a defensive copy of the timestamp
     */
    public Date getTimestamp() {
        return new Date(timestamp.getTime());
    }

    /**
     * Returns the username of the author.
     *
     * @return the author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Returns the action performed.
     *
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * Returns the target resource affected by the action.
     *
     * @return the target resource
     */
    public String getTargetResource() {
        return targetResource;
    }
}
