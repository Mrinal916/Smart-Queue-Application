package com.smartqueue.office.entity;

import com.smartqueue.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "offices")
public class Office extends AuditableEntity {

  @Column(nullable = false, unique = true, length = 30)
  private String code;

  @Column(nullable = false, unique = true, length = 150)
  private String name;

  @Column(nullable = false, length = 500)
  private String address;

  @Column(nullable = false, length = 30)
  private String category;

  @Column(nullable = false)
  private boolean active = true;

  protected Office() {}

  public Office(String code, String name, String address, String category) {
    this.code = code;
    this.name = name;
    this.address = address;
    this.category = category;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getAddress() {
    return address;
  }

  public String getCategory() {
    return category;
  }

  public boolean isActive() {
    return active;
  }

  public void update(String code, String name, String address, String category, boolean active) {
    this.code = code;
    this.name = name;
    this.address = address;
    this.category = category;
    this.active = active;
  }

  public void deactivate() {
    this.active = false;
  }
}
