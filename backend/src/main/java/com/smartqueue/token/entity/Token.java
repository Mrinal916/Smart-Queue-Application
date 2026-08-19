package com.smartqueue.token.entity;

import com.smartqueue.common.entity.AuditableEntity;
import com.smartqueue.counter.entity.Counter;
import com.smartqueue.office.entity.Office;
import com.smartqueue.servicecatalog.entity.QueueService;
import com.smartqueue.token.enums.TokenStatus;
import com.smartqueue.user.entity.UserAccount;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "tokens")
public class Token extends AuditableEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "citizen_id")
  private UserAccount citizen;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "office_id")
  private Office office;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "service_id")
  private QueueService service;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "counter_id")
  private Counter counter;

  @Column(name = "token_number")
  private int tokenNumber;

  @Column(name = "queue_date")
  private LocalDate queueDate;

  @Column(name = "appointment_time")
  private LocalTime appointmentTime;

  @Column(name = "visitor_name", length = 150)
  private String visitorName;

  @Column(name = "visitor_phone", length = 30)
  private String visitorPhone;

  @Column(name = "visitor_age")
  private Integer visitorAge;

  @Column(name = "visitor_gender", length = 30)
  private String visitorGender;

  @Column(name = "age_priority", nullable = false)
  private boolean agePriority;

  @Column(name = "booking_key", length = 100)
  private String bookingKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TokenStatus status;

  @Column(name = "checked_in_at")
  private Instant checkedInAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(nullable = false)
  private boolean appeared;

  @Column(name = "appeared_at")
  private Instant appearedAt;

  protected Token() {}

  public Token(
      UserAccount citizen,
      Office office,
      QueueService service,
      int n,
      String key,
      LocalDate appointmentDate,
      LocalTime appointmentTime,
      String visitorName,
      String visitorPhone,
      Integer visitorAge,
      String visitorGender) {
    this.citizen = citizen;
    this.office = office;
    this.service = service;
    tokenNumber = n;
    bookingKey = key;
    queueDate = appointmentDate;
    this.appointmentTime = appointmentTime;
    this.visitorName = visitorName;
    this.visitorPhone = visitorPhone;
    this.visitorAge = visitorAge;
    this.visitorGender = visitorGender;
    this.agePriority = visitorAge != null && visitorAge >= 54;
    status = TokenStatus.WAITING;
  }

  public UserAccount getCitizen() {
    return citizen;
  }

  public Office getOffice() {
    return office;
  }

  public QueueService getService() {
    return service;
  }

  public Counter getCounter() {
    return counter;
  }

  public int getTokenNumber() {
    return tokenNumber;
  }

  public void assignSerialNumber(long serialNumber) {
    tokenNumber = Math.toIntExact(serialNumber);
  }

  public LocalDate getQueueDate() {
    return queueDate;
  }

  public LocalTime getAppointmentTime() {
    return appointmentTime;
  }

  public String getVisitorName() {
    return visitorName;
  }

  public String getVisitorPhone() {
    return visitorPhone;
  }

  public Integer getVisitorAge() {
    return visitorAge;
  }

  public String getVisitorGender() {
    return visitorGender;
  }

  public boolean hasAgePriority() {
    return agePriority;
  }

  public boolean hasAppeared() {
    return appeared;
  }

  public Instant getAppearedAt() {
    return appearedAt;
  }

  public void markAppeared() {
    if (!appeared) {
      appeared = true;
      appearedAt = Instant.now();
    }
  }

  public TokenStatus getStatus() {
    return status;
  }

  public Instant getCheckedInAt() {
    return checkedInAt;
  }

  public void transition(TokenStatus next, Counter c) {
    if (status == TokenStatus.COMPLETED)
      throw new IllegalStateException("Completed tokens cannot change");
    status = next;
    counter = c;
    if (next == TokenStatus.CALLED) checkedInAt = Instant.now();
    if (next == TokenStatus.COMPLETED) completedAt = Instant.now();
  }
}
