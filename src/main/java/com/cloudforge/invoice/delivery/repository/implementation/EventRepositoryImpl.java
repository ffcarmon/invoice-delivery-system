package com.cloudforge.invoice.delivery.repository.implementation;


import com.cloudforge.invoice.delivery.domain.UserEvent;
import com.cloudforge.invoice.delivery.enumeration.EventType;
import com.cloudforge.invoice.delivery.exception.ApiException;
import com.cloudforge.invoice.delivery.repository.EventRepository;
import com.cloudforge.invoice.delivery.rowmapper.UserEventRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;

import static com.cloudforge.invoice.delivery.query.EventQuery.*;
import static java.util.Map.of;

@Repository
@RequiredArgsConstructor
@Slf4j
public class EventRepositoryImpl implements EventRepository {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Collection<UserEvent> getEventsByUserId(Long userId) {
        log.info("Fetching events for user ID: {}", userId);
        try {
            Collection<UserEvent> events = jdbc.query(SELECT_EVENTS_BY_USER_ID_QUERY, of("id", userId), new UserEventRowMapper());
            log.info("Found {} events for user ID: {}", events.size(), userId);
            return events;
        } catch (Exception exception) {
            log.error("Error fetching events for user ID: {}. Exception: {}", userId, exception.getMessage());
            throw new ApiException("An error occurred while retrieving user events.");
        }
    }

    @Override
    public void addUserEvent(String email, EventType eventType, String device, String ipAddress) {
        log.info("Adding event for user email: {} | EventType: {} | Device: {} | IP: {}", email, eventType, device, ipAddress);
        try {
            int rowsAffected = jdbc.update(INSERT_EVENT_BY_USER_EMAIL_QUERY,
                    of("email", email,
                            "type", eventType.toString(),
                            "device", device,
                            "ipAddress", ipAddress));

            if (rowsAffected > 0) {
                log.info("Event successfully added for user: {}", email);
            } else {
                log.warn("No event was inserted for user: {}", email);
            }
        } catch (Exception exception) {
            log.error("Error adding event for user email: {}. Exception: {}", email, exception.getMessage());
            throw new ApiException("An error occurred while adding user event.");
        }
    }


    @Override
    public void addUserEvent(Long userId, EventType eventType, String device, String ipAddress) {

    }
}
