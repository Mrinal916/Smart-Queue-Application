package com.smartqueue.queue.entity;

import com.smartqueue.common.entity.AuditableEntity;
import com.smartqueue.counter.entity.Counter;
import com.smartqueue.token.entity.Token;
import com.smartqueue.token.enums.TokenStatus;
import com.smartqueue.user.entity.UserAccount;
import jakarta.persistence.*;

@Entity
@Table(name = "queue_history")
public class QueueHistory extends AuditableEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "token_id")
  private Token token;

  @Enumerated(EnumType.STRING)
  @Column(name = "previous_status")
  private TokenStatus previousStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", nullable = false)
  private TokenStatus newStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "performed_by")
  private UserAccount performedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "counter_id")
  private Counter counter;

  protected QueueHistory() {}

  public QueueHistory(Token t, TokenStatus p, TokenStatus n, UserAccount u, Counter c) {
    token = t;
    previousStatus = p;
    newStatus = n;
    performedBy = u;
    counter = c;
  }
}
