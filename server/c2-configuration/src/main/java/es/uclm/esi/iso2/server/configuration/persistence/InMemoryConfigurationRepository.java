package es.uclm.esi.iso2.server.configuration.persistence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Keeps the entries of one kind in memory (D-030), so they are lost when the program closes. The same class is used
 * for the five kinds of entry; each one says how to read its own id.
 *
 * @param <T> the kind of entry
 */
public class InMemoryConfigurationRepository<T> implements IConfigurationRepository<T> {
    private final Map<String, T> entries = new LinkedHashMap<>();
    private final Function<T, String> idOf;

    /**
     * Creates an empty repository.
     *
     * @param idOf how to get the id of an entry, for example {@code TestType::getId}
     */
    public InMemoryConfigurationRepository(Function<T, String> idOf) {
        this.idOf = idOf;
    }

    @Override
    public T findById(String id) {
        return entries.get(id);
    }

    @Override
    public List<T> findAll() {
        return List.copyOf(entries.values());
    }

    @Override
    public void save(T entry) {
        entries.put(idOf.apply(entry), entry);
    }
}
