package com.smartqueue.websocket.service;

import com.smartqueue.websocket.dto.CounterStatusEvent;
import com.smartqueue.websocket.dto.QueueUpdateEvent;
import com.smartqueue.websocket.event.CounterDomainEvent;
import com.smartqueue.websocket.event.QueueDomainEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class QueueUpdatePublisher {
  private final SimpMessagingTemplate messagingTemplate;

  public QueueUpdatePublisher(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publish(QueueDomainEvent event) {
    QueueUpdateEvent payload =
        new QueueUpdateEvent(
            event.type(),
            event.officeId(),
            event.serviceId(),
            event.tokenId(),
            event.citizenId(),
            event.tokenStatus(),
            event.occurredAt());
    messagingTemplate.convertAndSend("/topic/services/" + event.serviceId(), payload);
    messagingTemplate.convertAndSend("/topic/offices/" + event.officeId(), payload);
    messagingTemplate.convertAndSend("/topic/admin/queue", payload);
    messagingTemplate.convertAndSend("/topic/citizens/" + event.citizenId(), payload);
    messagingTemplate.convertAndSendToUser(
        event.citizenId().toString(), "/queue/notifications", payload);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publish(CounterDomainEvent event) {
    CounterStatusEvent payload =
        new CounterStatusEvent(
            event.officeId(), event.counterId(), event.status(), event.occurredAt());
    messagingTemplate.convertAndSend("/topic/offices/" + event.officeId(), payload);
    messagingTemplate.convertAndSend("/topic/admin/queue", payload);
  }
}
