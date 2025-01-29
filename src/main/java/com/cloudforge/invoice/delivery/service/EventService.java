package com.cloudforge.invoice.delivery.service;

import com.cloudforge.invoice.delivery.domain.UserEvent;
import com.cloudforge.invoice.delivery.enumeration.EventType;

import java.util.Collection;

public interface EventService {
    Collection<UserEvent> getEventsByUserId(Long userId);
    void addUserEvent(String email, EventType eventType, String device, String ipAddress);
    void addUserEvent(Long userId, EventType eventType, String device, String ipAddress);
}
