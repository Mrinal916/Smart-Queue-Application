package com.smartqueue.servicecatalog.entity;

import com.smartqueue.common.entity.AuditableEntity;
import com.smartqueue.department.entity.Department;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "services")
public class QueueService extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "department_id")
  private Department department;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  @Column(name = "break_start_time")
  private LocalTime breakStartTime;

  @Column(name = "break_end_time")
  private LocalTime breakEndTime;

  @Column(name = "daily_capacity", nullable = false)
  private int dailyCapacity;

  @Column(name = "average_service_minutes", nullable = false)
  private int averageServiceMinutes;

  @ElementCollection
  @CollectionTable(name = "service_open_days", joinColumns = @JoinColumn(name = "service_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week", nullable = false, length = 9)
  private Set<DayOfWeek> openDays = EnumSet.noneOf(DayOfWeek.class);

  @Column(nullable = false)
  private boolean active = true;

  protected QueueService() {}

  public QueueService(
      Department department,
      String name,
      LocalTime startTime,
      LocalTime endTime,
      LocalTime breakStartTime,
      LocalTime breakEndTime,
      int dailyCapacity,
      int averageServiceMinutes,
      Set<DayOfWeek> openDays) {
    this.department = department;
    this.name = name;
    this.startTime = startTime;
    this.endTime = endTime;
    this.breakStartTime = breakStartTime;
    this.breakEndTime = breakEndTime;
    this.dailyCapacity = dailyCapacity;
    this.averageServiceMinutes = averageServiceMinutes;
    this.openDays = EnumSet.copyOf(openDays);
  }

  public Department getDepartment() {
    return department;
  }

  public String getName() {
    return name;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public LocalTime getBreakStartTime() {
    return breakStartTime;
  }

  public LocalTime getBreakEndTime() {
    return breakEndTime;
  }

  public int getDailyCapacity() {
    return dailyCapacity;
  }

  public int getAverageServiceMinutes() {
    return averageServiceMinutes;
  }

  public Set<DayOfWeek> getOpenDays() {
    return Set.copyOf(openDays);
  }

  public boolean isActive() {
    return active;
  }

  public void update(
      Department department,
      String name,
      LocalTime startTime,
      LocalTime endTime,
      LocalTime breakStartTime,
      LocalTime breakEndTime,
      int dailyCapacity,
      int averageServiceMinutes,
      Set<DayOfWeek> openDays,
      boolean active) {
    this.department = department;
    this.name = name;
    this.startTime = startTime;
    this.endTime = endTime;
    this.breakStartTime = breakStartTime;
    this.breakEndTime = breakEndTime;
    this.dailyCapacity = dailyCapacity;
    this.averageServiceMinutes = averageServiceMinutes;
    this.openDays = EnumSet.copyOf(openDays);
    this.active = active;
  }

  public void deactivate() {
    active = false;
  }
}
