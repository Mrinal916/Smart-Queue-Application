package com.smartqueue.department.entity;

import com.smartqueue.common.entity.AuditableEntity;
import com.smartqueue.office.entity.Office;
import jakarta.persistence.*;

@Entity
@Table(name = "departments")
public class Department extends AuditableEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "office_id")
  private Office office;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false)
  private boolean active = true;

  protected Department() {}

  public Department(Office office, String name) {
    this.office = office;
    this.name = name;
  }

  public Office getOffice() {
    return office;
  }

  public String getName() {
    return name;
  }

  public boolean isActive() {
    return active;
  }

  public void update(Office office, String name, boolean active) {
    this.office = office;
    this.name = name;
    this.active = active;
  }

  public void deactivate() {
    active = false;
  }
}
