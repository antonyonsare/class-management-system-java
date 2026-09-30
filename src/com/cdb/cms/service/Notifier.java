package com.cdb.cms.service;

import com.cdb.cms.model.Notification;
import com.cdb.cms.repository.Repositories;

import java.time.LocalDateTime;

/**
 * Fire-and-forget in-app notifications, replacing the Firestore 'notifications'
 * collection listeners of the web app. Console UIs poll
 * {@link #forUser(String)} after each action, which gives the same
 * "you have new notifications" feel without push infrastructure.
 */
public class Notifier {

    private final Repositories repos;

    public Notifier(Repositories repos) {
        this.repos = repos;
    }

    public void notify(String recipientId, String type, String message) {
        Notification n = new Notification();
        n.setRecipientId(recipientId);
        n.setType(type);
        n.setMessage(message);
        n.setCreatedAt(LocalDateTime.now());
        repos.notifications.save(n);
    }

    public java.util.List<Notification> forUser(String recipientId) {
        return repos.notifications.findBy(n -> recipientId.equals(n.getRecipientId()));
    }
}
