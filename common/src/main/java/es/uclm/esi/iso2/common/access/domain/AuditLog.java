package es.uclm.esi.iso2.common.access.domain;

import java.util.Date;

/** One entry of the audit log. It cannot be modified: entries are only added (D11 of the analysis). */
public class AuditLog {
    private final Long id;
    private final Date timestamp;
    private final String author;
    private final String action;
    private final String targetResource;

    public AuditLog(Long id, Date timestamp, String author, String action, String targetResource) {
        this.id = id;
        this.timestamp = new Date(timestamp.getTime());
        this.author = author;
        this.action = action;
        this.targetResource = targetResource;
    }

    public Long getId() {
        return id;
    }

    public Date getTimestamp() {
        return new Date(timestamp.getTime());
    }

    public String getAuthor() {
        return author;
    }

    public String getAction() {
        return action;
    }

    public String getTargetResource() {
        return targetResource;
    }
}
