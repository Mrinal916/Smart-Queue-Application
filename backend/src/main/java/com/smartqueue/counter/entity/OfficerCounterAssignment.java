package com.smartqueue.counter.entity;

import com.smartqueue.common.entity.AuditableEntity;
import com.smartqueue.user.entity.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "officer_counter_assignments")
public class OfficerCounterAssignment extends AuditableEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "officer_id")
  private UserAccount officer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "counter_id")
  private Counter counter;

  @Column(name = "assigned_at", nullable = false)
  private Instant assignedAt;

  @Column(name = "released_at")
  private Instant releasedAt;

  protected OfficerCounterAssignment() {}

  public OfficerCounterAssignment(UserAccount officer, Counter counter) {
    this.officer = officer;
    this.counter = counter;
    this.assignedAt = Instant.now();
  }

  public UserAccount getOfficer() {
    return officer;
  }

  public Counter getCounter() {
    return counter;
  }

  public Instant getAssignedAt() {
    return assignedAt;
  }

  public Instant getReleasedAt() {
    return releasedAt;
  }

  public boolean isActive() {
    return releasedAt == null;
  }

  public void release() {
    releasedAt = Instant.now();
  }
}
