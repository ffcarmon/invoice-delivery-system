package com.cloudforge.invoice.delivery.listener;

import com.cloudforge.invoice.delivery.event.NewUserEvent;
import com.cloudforge.invoice.delivery.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewUserNotificationListener {
    //private final EmailService emailService;

    @Async
    @EventListener
    public void handleNewUserEvent(NewUserEvent event) {
        log.info("Sending email notification for event: {} to {}", event.getType(), event.getEmail());
       // emailService.sendNotification(event.getEmail(), event.getType());
    }
}
