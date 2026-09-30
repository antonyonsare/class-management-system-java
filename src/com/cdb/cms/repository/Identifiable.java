package com.cdb.cms.repository;

/**
 * Marker contract for entities that can be stored in a Repository.
 * School uses its schoolCode as its id via a bridge (see Repositories).
 */
public interface Identifiable {
    String getId();

    void setId(String id);
}
