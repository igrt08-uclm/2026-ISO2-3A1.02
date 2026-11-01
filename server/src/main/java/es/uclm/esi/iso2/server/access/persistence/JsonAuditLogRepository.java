package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.AuditLog;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Keeps the audit log in a JSON file, with no external library (rule 6 of AGENTS.md). It is temporary: how to store the
 * data is decided in Meeting 6.
 */
public class JsonAuditLogRepository implements IAuditLogRepository {
    private static final Comparator<AuditLog> NEWEST_FIRST =
            Comparator.comparing(AuditLog::getTimestamp).thenComparing(AuditLog::getId).reversed();

    private final Path file;

    public JsonAuditLogRepository(Path file) {
        this.file = file;
    }

    @Override
    public void save(AuditLog entry) {
        List<AuditLog> entries = readEntries();
        entries.add(entry);
        writeEntries(entries);
    }

    @Override
    public List<AuditLog> find(String filters) {
        return readEntries().stream().filter(parseFilters(filters)).sorted(NEWEST_FIRST).toList();
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

    private List<AuditLog> readEntries() {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try {
            return new EntriesParser(Files.readString(file, StandardCharsets.UTF_8)).parse();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("The audit log is damaged: " + file, exception);
        }
    }

    /** Writes a new file and then replaces the old one, so a failure while writing cannot lose the entries. */
    private void writeEntries(List<AuditLog> entries) {
        String json = entries.stream().map(JsonAuditLogRepository::toJson)
                .collect(Collectors.joining("," + System.lineSeparator(), "[" + System.lineSeparator(),
                        System.lineSeparator() + "]" + System.lineSeparator()));
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String toJson(AuditLog entry) {
        return "  {\"id\": " + entry.getId()
                + ", \"timestamp\": " + quote(entry.getTimestamp().toInstant().toString())
                + ", \"author\": " + quote(entry.getAuthor())
                + ", \"action\": " + quote(entry.getAction())
                + ", \"targetResource\": " + quote(entry.getTargetResource()) + "}";
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder quoted = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> quoted.append("\\\"");
                case '\\' -> quoted.append("\\\\");
                case '\n' -> quoted.append("\\n");
                case '\r' -> quoted.append("\\r");
                case '\t' -> quoted.append("\\t");
                default -> {
                    if (character < 0x20) {
                        quoted.append(String.format("\\u%04x", (int) character));
                    } else {
                        quoted.append(character);
                    }
                }
            }
        }
        return quoted.append('"').toString();
    }

    /** Reads the format written by {@link #toJson}: a list of objects whose values are strings, numbers or null. */
    private static final class EntriesParser {
        private final String text;
        private int position;

        private EntriesParser(String text) {
            this.text = text;
        }

        private List<AuditLog> parse() {
            List<AuditLog> entries = new ArrayList<>();
            expect('[');
            if (next() == ']') {
                expect(']');
            } else {
                do {
                    entries.add(parseEntry());
                } while (acceptComma());
                expect(']');
            }
            skipSpaces();
            if (position != text.length()) {
                throw new IllegalArgumentException("Unexpected text at position " + position);
            }
            return entries;
        }

        private AuditLog parseEntry() {
            Map<String, String> fields = new HashMap<>();
            expect('{');
            do {
                String name = parseString();
                expect(':');
                fields.put(name, parseValue());
            } while (acceptComma());
            expect('}');
            return new AuditLog(Long.valueOf(fields.get("id")), Date.from(Instant.parse(fields.get("timestamp"))),
                    fields.get("author"), fields.get("action"), fields.get("targetResource"));
        }

        private String parseValue() {
            char first = next();
            if (first == '"') {
                return parseString();
            }
            int start = position;
            while (position < text.length() && ",}]".indexOf(text.charAt(position)) < 0
                    && !Character.isWhitespace(text.charAt(position))) {
                position++;
            }
            String value = text.substring(start, position);
            return "null".equals(value) ? null : value;
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (text.charAt(position) != '"') {
                char character = text.charAt(position++);
                value.append(character == '\\' ? parseEscape() : character);
            }
            position++;
            return value.toString();
        }

        private char parseEscape() {
            char escaped = text.charAt(position++);
            return switch (escaped) {
                case '"', '\\', '/' -> escaped;
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'u' -> {
                    position += 4;
                    yield (char) Integer.parseInt(text.substring(position - 4, position), 16);
                }
                default -> throw new IllegalArgumentException("Wrong escape at position " + position);
            };
        }

        private boolean acceptComma() {
            if (next() == ',') {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (next() != expected) {
                throw new IllegalArgumentException("Expected " + expected + " at position " + position);
            }
            position++;
        }

        private char next() {
            skipSpaces();
            return text.charAt(position);
        }

        private void skipSpaces() {
            while (position < text.length() && Character.isWhitespace(text.charAt(position))) {
                position++;
            }
        }
    }
}
