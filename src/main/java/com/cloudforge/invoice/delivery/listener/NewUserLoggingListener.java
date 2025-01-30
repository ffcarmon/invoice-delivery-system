package com.cloudforge.invoice.delivery.listener;

import com.cloudforge.invoice.delivery.event.NewUserEvent;
//import com.cloudforge.invoice.delivery.service.LoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewUserLoggingListener {
    //private final LoggingService loggingService;

    @Async
    @EventListener
    public void logNewUserEvent(NewUserEvent event) {
        log.info("Logging event: {} for user {}", event.getType(), event.getEmail());
        //loggingService.logEvent(event.getEmail(), event.getType());
    }
}
