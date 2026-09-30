package com.cdb.cms.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Offline storage: a per-entity-type store backed by a LinkedHashMap, which
 * keeps insertion order for stable listings. Ids are generated as
 * {@code <prefix>-<n>} (e.g. usr-1, stu-1) so console output stays readable.
 *
 * <p>Thread-safety: the console UI is single-threaded, so plain HashMap
 * semantics suffice; a Firestore adapter would inherit Firestore's
 * server-side concurrency instead of adding locking here.
 */
public class InMemoryRepository<T extends Identifiable> implements Repository<T> {

    private final Map<String, T> store = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private final String idPrefix;

    public InMemoryRepository(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    @Override
    public synchronized T save(T entity) {
        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(idPrefix + "-" + sequence.incrementAndGet());
        }
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<T> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<T> findBy(Predicate<T> filter) {
        List<T> result = new ArrayList<>();
        for (T entity : store.values()) {
            if (filter.test(entity)) {
                result.add(entity);
            }
        }
        return result;
    }

    @Override
    public boolean deleteById(String id) {
        return store.remove(id) != null;
    }

    @Override
    public long count() {
        return store.size();
    }
}
