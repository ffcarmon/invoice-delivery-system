package com.cloudforge.invoice.delivery.repository;

import com.cloudforge.invoice.delivery.domain.UserEvent;
import com.cloudforge.invoice.delivery.enumeration.EventType;

import java.util.Collection;

public interface EventRepository {
    Collection<UserEvent> getEventsByUserId(Long userId);
    void addUserEvent(String email, EventType eventType, String device, String ipAddress);
    void addUserEvent(Long userId, EventType eventType, String device, String ipAddress);
}
