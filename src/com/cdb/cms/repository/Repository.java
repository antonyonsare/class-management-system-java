package com.cdb.cms.repository;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Generic persistence contract for one entity type.
 *
 * <p>This interface is the project's integration seam: the offline build ships
 * an in-memory implementation, and a future adapter (Firestore, or a local
 * server + database) only needs to implement this same contract — no service
 * or UI code changes. Query methods that currently filter in memory
 * (findBy) map directly to Firestore where-clauses in an adapter.
 *
 * @param <T> entity type
 */
public interface Repository<T extends Identifiable> {

    /** Inserts or updates the entity, assigning an id when absent. */
    T save(T entity);

    Optional<T> findById(String id);

    /** All entities in insertion order. */
    List<T> findAll();

    /** Entities matching the filter. */
    List<T> findBy(Predicate<T> filter);

    boolean deleteById(String id);

    long count();

    default long count(Predicate<T> filter) {
        return findBy(filter).size();
    }

    default Optional<T> findFirst(Predicate<T> filter) {
        return findBy(filter).stream().findFirst();
    }

    default boolean exists(Predicate<T> filter) {
        return findFirst(filter).isPresent();
    }
}
