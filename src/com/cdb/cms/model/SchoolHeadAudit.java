package com.cdb.cms.model;

import com.cdb.cms.repository.Identifiable;

import java.time.LocalDateTime;

/**
 * An audit trail entry for school head actions. Maps to the Firestore
 * 'schoolHeadAudit' collection.
 */
public class SchoolHeadAudit implements Identifiable {
    private String id;
    private String headId;
    private String actionDescription;
    private LocalDateTime at;

    public SchoolHeadAudit() {
    }

    public SchoolHeadAudit(String id, String headId, String actionDescription, LocalDateTime at) {
        this.id = id;
        this.headId = headId;
        this.actionDescription = actionDescription;
        this.at = at;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHeadId() {
        return headId;
    }

    public void setHeadId(String headId) {
        this.headId = headId;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
    }

    public LocalDateTime getAt() {
        return at;
    }

    public void setAt(LocalDateTime at) {
        this.at = at;
    }
}
