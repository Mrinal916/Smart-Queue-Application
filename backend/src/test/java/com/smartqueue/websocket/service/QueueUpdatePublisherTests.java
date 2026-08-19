package com.smartqueue.websocket.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.smartqueue.websocket.dto.QueueUpdateEvent;
import com.smartqueue.websocket.event.QueueDomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class QueueUpdatePublisherTests {

  @Test
  void publishesServiceOfficeAndCitizenNotifications() {
    SimpMessagingTemplate template = org.mockito.Mockito.mock(SimpMessagingTemplate.class);
    QueueUpdatePublisher publisher = new QueueUpdatePublisher(template);
    UUID officeId = UUID.randomUUID();
    UUID serviceId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    UUID citizenId = UUID.randomUUID();

    publisher.publish(
        new QueueDomainEvent(
            "TOKEN_CALLED", officeId, serviceId, tokenId, citizenId, "CALLED", Instant.now()));

    verify(template)
        .convertAndSend(eq("/topic/services/" + serviceId), any(QueueUpdateEvent.class));
    verify(template).convertAndSend(eq("/topic/offices/" + officeId), any(QueueUpdateEvent.class));
    verify(template)
        .convertAndSendToUser(
            eq(citizenId.toString()), eq("/queue/notifications"), any(QueueUpdateEvent.class));
  }
}
