package com.smartqueue.counter.entity;

import com.smartqueue.common.entity.AuditableEntity;
import com.smartqueue.counter.enums.CounterStatus;
import com.smartqueue.office.entity.Office;
import jakarta.persistence.*;

@Entity
@Table(name = "counters")
public class Counter extends AuditableEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "office_id")
  private Office office;

  @Column(nullable = false, length = 30)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CounterStatus status = CounterStatus.CLOSED;

  @Column(nullable = false)
  private boolean active = true;

  protected Counter() {}

  public Counter(Office office, String code) {
    this.office = office;
    this.code = code;
  }

  public Office getOffice() {
    return office;
  }

  public String getCode() {
    return code;
  }

  public CounterStatus getStatus() {
    return status;
  }

  public boolean isActive() {
    return active;
  }

  public void update(Office office, String code, boolean active) {
    this.office = office;
    this.code = code;
    this.active = active;
    if (!active) status = CounterStatus.CLOSED;
  }

  public void setStatus(CounterStatus status) {
    this.status = status;
  }

  public void deactivate() {
    active = false;
    status = CounterStatus.CLOSED;
  }
}
