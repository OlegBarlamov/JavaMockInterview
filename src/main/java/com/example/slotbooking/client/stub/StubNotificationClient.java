package com.example.slotbooking.client.stub;

import com.example.slotbooking.client.NotificationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class StubNotificationClient implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(StubNotificationClient.class);

    @Override
    public void sendEmail(String to, String subject, String body) {
        StubLatency.simulate(500);
        log.debug("[stub] email sent: {}", subject);
    }

    @Override
    public void sendPush(String customerId, String message) {
        StubLatency.simulate(300);
        log.debug("[stub] push sent");
    }
}
