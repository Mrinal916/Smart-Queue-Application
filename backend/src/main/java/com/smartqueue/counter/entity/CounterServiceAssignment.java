package com.smartqueue.counter.entity;

import com.smartqueue.common.entity.AuditableEntity;
import com.smartqueue.servicecatalog.entity.QueueService;
import jakarta.persistence.*;

@Entity
@Table(name = "counter_service_assignments")
public class CounterServiceAssignment extends AuditableEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "counter_id")
  private Counter counter;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "service_id")
  private QueueService service;

  @Column(nullable = false)
  private boolean active = true;

  protected CounterServiceAssignment() {}

  public CounterServiceAssignment(Counter counter, QueueService service) {
    this.counter = counter;
    this.service = service;
  }

  public Counter getCounter() {
    return counter;
  }

  public QueueService getService() {
    return service;
  }

  public boolean isActive() {
    return active;
  }

  public void release() {
    active = false;
  }

  public void reactivate() {
    active = true;
  }
}
