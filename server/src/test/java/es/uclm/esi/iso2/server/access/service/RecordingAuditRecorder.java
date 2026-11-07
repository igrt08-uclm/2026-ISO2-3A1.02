package es.uclm.esi.iso2.server.access.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Test double of {@link IAuditRecorder} that keeps every entry in memory so tests can check them. */
class RecordingAuditRecorder implements IAuditRecorder {
    /** One call to {@code register}. A record gives {@code equals}, so entries can be compared directly. */
    record AuditEntry(String author, String action, String targetResource) {
    }

    private final List<AuditEntry> entries = new ArrayList<>();

    @Override
    public void register(String author, String action, String targetResource) {
        entries.add(new AuditEntry(author, action, targetResource));
    }

    List<AuditEntry> entries() {
        return Collections.unmodifiableList(entries);
    }
}
