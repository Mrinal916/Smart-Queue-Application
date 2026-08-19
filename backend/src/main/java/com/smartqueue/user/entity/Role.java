package com.smartqueue.user.entity;

import com.smartqueue.user.enums.RoleName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, unique = true, updatable = false)
  private UUID publicId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, unique = true, length = 30)
  private RoleName name;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Role() {}

  public Role(UUID publicId, RoleName name) {
    this.publicId = publicId;
    this.name = name;
  }

  @PrePersist
  void initializeTimestamps() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  public Long getId() {
    return id;
  }

  public RoleName getName() {
    return name;
  }
}
