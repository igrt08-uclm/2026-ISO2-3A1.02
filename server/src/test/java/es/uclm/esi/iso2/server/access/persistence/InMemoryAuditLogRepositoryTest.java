package es.uclm.esi.iso2.server.access.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.access.domain.AuditLog;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests of {@link InMemoryAuditLogRepository}: the order of the results, the filters and their errors. */
@DisplayName("InMemoryAuditLogRepository")
class InMemoryAuditLogRepositoryTest {
    private InMemoryAuditLogRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAuditLogRepository();
    }

    /** Two logins and one access to a patient, on three different days. */
    private void saveThreeEntries() {
        repository.save(entry(1, "2026-11-01T10:00:00", "admin", "LOGIN_OK", "authentication"));
        repository.save(entry(2, "2026-11-02T10:00:00", "nobody", "LOGIN_FAILED", "authentication:UNKNOWN_USER"));
        repository.save(entry(3, "2026-11-03T10:00:00", "mgarcia", "VIEW_REPORT", "PAT-1"));
    }

    private static AuditLog entry(long id, String dateTime, String author, String action, String target) {
        Date date = Date.from(LocalDateTime.parse(dateTime).atZone(ZoneId.systemDefault()).toInstant());
        return new AuditLog(id, date, author, action, target);
    }

    private static List<Long> ids(List<AuditLog> entries) {
        return entries.stream().map(AuditLog::getId).toList();
    }

    @Test
    @DisplayName("An empty repository gives an empty list")
    void emptyRepositoryGivesAnEmptyList() {
        // Act and Assert
        assertTrue(repository.find(null).isEmpty());
    }

    @ParameterizedTest(name = "filters = \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", ";"})
    @DisplayName("No filters give every entry")
    void noFiltersGiveEveryEntry(String filters) {
        // Arrange
        saveThreeEntries();

        // Act and Assert
        assertEquals(3, repository.find(filters).size());
    }

    @Test
    @DisplayName("The newest entries come first")
    void newestEntriesComeFirst() {
        // Arrange
        saveThreeEntries();

        // Act and Assert
        assertEquals(List.of(3L, 2L, 1L), ids(repository.find(null)));
    }

    // Two entries can be saved in the same millisecond.
    @Test
    @DisplayName("With the same date, the highest id comes first")
    void withTheSameDateTheHighestIdComesFirst() {
        // Arrange
        repository.save(entry(1, "2026-11-01T10:00:00", "admin", "LOGIN_OK", "authentication"));
        repository.save(entry(2, "2026-11-01T10:00:00", "admin", "AUDIT_CONSULTED", "audit"));

        // Act and Assert
        assertEquals(List.of(2L, 1L), ids(repository.find(null)));
    }

    @ParameterizedTest(name = "{0} finds {1}")
    @CsvSource({
        "author=admin,        1",
        "action=LOGIN_FAILED, 2",
        "target=PAT-1,        3",
        "target=UNKNOWN,      2"
    })
    @DisplayName("Each filter finds its entry")
    void eachFilterFindsItsEntry(String filters, long expectedId) {
        // Arrange
        saveThreeEntries();

        // Act and Assert
        assertEquals(List.of(expectedId), ids(repository.find(filters)));
    }

    // "authentication" must also find "authentication:UNKNOWN_USER", as it is one kind of resource.
    @Test
    @DisplayName("The target filter finds every entry that contains the text")
    void targetFilterFindsEveryEntryThatContainsTheText() {
        // Arrange
        saveThreeEntries();

        // Act and Assert
        assertEquals(List.of(2L, 1L), ids(repository.find("target=authentication")));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"author=adm", "author=ADMIN", "action=LOGIN"})
    @DisplayName("The author and action filters are exact")
    void authorAndActionFiltersAreExact(String filters) {
        // Arrange
        saveThreeEntries();

        // Act and Assert
        assertTrue(repository.find(filters).isEmpty());
    }

    @ParameterizedTest(name = "{0} finds {1}")
    @CsvSource({
        "from=2026-11-02,               3;2",
        "to=2026-11-02,                 2;1",
        "from=2026-11-02;to=2026-11-02, 2"
    })
    @DisplayName("The dates are included in the range")
    void datesAreIncludedInTheRange(String filters, String expectedIds) {
        // Arrange
        saveThreeEntries();

        // Act
        String found = String.join(";", ids(repository.find(filters)).stream().map(String::valueOf).toList());

        // Assert
        assertEquals(expectedIds, found);
    }

    @Test
    @DisplayName("The to filter includes the whole day")
    void toFilterIncludesTheWholeDay() {
        // Arrange
        repository.save(entry(1, "2026-11-01T23:59:59", "admin", "LOGIN_OK", "authentication"));
        repository.save(entry(2, "2026-11-02T00:00:00", "admin", "LOGIN_OK", "authentication"));

        // Act and Assert
        assertEquals(List.of(1L), ids(repository.find("to=2026-11-01")));
    }

    @Test
    @DisplayName("Several filters only find the entries that meet all of them")
    void severalFiltersOnlyFindEntriesThatMeetAllOfThem() {
        // Arrange
        saveThreeEntries();
        repository.save(entry(4, "2026-11-04T10:00:00", "admin", "LOGIN_FAILED", "authentication:WRONG_PASSWORD"));

        // Act and Assert
        assertEquals(List.of(4L), ids(repository.find("author=admin;action=LOGIN_FAILED;from=2026-11-01")));
    }

    @Test
    @DisplayName("Filters with no result give an empty list")
    void filtersWithNoResultGiveAnEmptyList() {
        // Arrange
        saveThreeEntries();

        // Act and Assert
        assertTrue(repository.find("author=admin;action=VIEW_REPORT").isEmpty());
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {" author = admin ", "author=admin;", ";author=admin", "author=admin; ;"})
    @DisplayName("Spaces and empty pairs are ignored")
    void spacesAndEmptyPairsAreIgnored(String filters) {
        // Arrange
        saveThreeEntries();

        // Act and Assert
        assertEquals(List.of(1L), ids(repository.find(filters)));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"colour=red", "author", "from=yesterday", "to=2026-13-01", "from=01/11/2026"})
    @DisplayName("An unknown key, a pair without = or a wrong date give an error")
    void wrongFiltersGiveAnError(String filters) {
        // Arrange
        saveThreeEntries();

        // Act and Assert
        assertThrows(IllegalArgumentException.class, () -> repository.find(filters));
    }

    // D11: the entries are only added, so nobody can remove one through the returned list.
    @Test
    @DisplayName("The returned list cannot change the repository")
    void returnedListCannotChangeTheRepository() {
        // Arrange
        saveThreeEntries();
        List<AuditLog> found = repository.find(null);

        // Act
        assertThrows(UnsupportedOperationException.class, found::clear);

        // Assert
        assertEquals(3, repository.find(null).size());
    }
}
