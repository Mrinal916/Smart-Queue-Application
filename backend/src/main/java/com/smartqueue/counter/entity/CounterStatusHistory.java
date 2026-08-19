package com.smartqueue.counter.entity;

import com.smartqueue.counter.enums.CounterStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "counter_status_history")
public class CounterStatusHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "counter_id")
  private Counter counter;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CounterStatus status;

  @Column(nullable = false)
  private Instant occurredAt;

  protected CounterStatusHistory() {}

  public CounterStatusHistory(Counter c, CounterStatus s) {
    counter = c;
    status = s;
    occurredAt = Instant.now();
  }
}
