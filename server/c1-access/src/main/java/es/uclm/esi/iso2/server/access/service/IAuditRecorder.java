package es.uclm.esi.iso2.server.access.service;

public interface IAuditRecorder {
    void register(String author, String action, String targetResource);
}
