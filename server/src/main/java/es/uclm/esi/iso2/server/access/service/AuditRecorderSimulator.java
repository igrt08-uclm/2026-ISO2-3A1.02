package es.uclm.esi.iso2.server.access.service;

/** Temporary implementation for UC22 (D-023): AuditService replaces it in UC19. */
public class AuditRecorderSimulator implements IAuditRecorder {
    @Override
    public void register(String author, String action, String targetResource) {
        System.out.println(author + " | " + action + " | " + targetResource);
    }
}
