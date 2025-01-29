package com.cloudforge.invoice.delivery.listener;

import com.cloudforge.invoice.delivery.event.NewUserEvent;
import com.cloudforge.invoice.delivery.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.cloudforge.invoice.delivery.utils.RequestUtils.getDevice;
import static com.cloudforge.invoice.delivery.utils.RequestUtils.getIpAddress;

@Component
@RequiredArgsConstructor
public class NewUserEventListener {
    private final EventService eventService;
    private final HttpServletRequest request;

    @EventListener
    public void onNewUserEvent(NewUserEvent event) {
        eventService.addUserEvent(event.getEmail(), event.getType(), getDevice(request), getIpAddress(request));
    }
}
