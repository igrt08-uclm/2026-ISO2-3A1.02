package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.AuditLog;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** Keeps the audit log in memory, like the users: the application does not need persistence for now. */
public class InMemoryAuditLogRepository implements IAuditLogRepository {
    private static final Comparator<AuditLog> NEWEST_FIRST =
            Comparator.comparing(AuditLog::getTimestamp).thenComparing(AuditLog::getId).reversed();

    private final List<AuditLog> entries = new ArrayList<>();

    @Override
    public void save(AuditLog entry) {
        entries.add(entry);
    }

    @Override
    public List<AuditLog> find(String filters) {
        return entries.stream().filter(parseFilters(filters)).sorted(NEWEST_FIRST).toList();
    }

    private static Predicate<AuditLog> parseFilters(String filters) {
        Predicate<AuditLog> matches = entry -> true;
        if (filters == null || filters.isBlank()) {
            return matches;
        }
        for (String pair : filters.split(";")) {
            if (!pair.isBlank()) {
                int separator = pair.indexOf('=');
                if (separator < 0) {
                    throw new IllegalArgumentException("Filter without '=': " + pair);
                }
                String key = pair.substring(0, separator).trim();
                String value = pair.substring(separator + 1).trim();
                matches = matches.and(filterFor(key, value));
            }
        }
        return matches;
    }

    private static Predicate<AuditLog> filterFor(String key, String value) {
        return switch (key) {
            case "author" -> entry -> value.equals(entry.getAuthor());
            case "action" -> entry -> value.equals(entry.getAction());
            case "target" -> entry -> entry.getTargetResource() != null && entry.getTargetResource().contains(value);
            case "from" -> {
                LocalDate from = parseDate(value);
                yield entry -> !dateOf(entry).isBefore(from);
            }
            case "to" -> {
                LocalDate to = parseDate(value);
                yield entry -> !dateOf(entry).isAfter(to);
            }
            default -> throw new IllegalArgumentException("Unknown filter: " + key);
        };
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Wrong date: " + value, exception);
        }
    }

    private static LocalDate dateOf(AuditLog entry) {
        return entry.getTimestamp().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
