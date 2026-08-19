package com.smartqueue.analytics.service;

import com.smartqueue.analytics.dto.CounterAnalyticsResponse;
import com.smartqueue.analytics.dto.DashboardSummaryResponse;
import com.smartqueue.analytics.dto.OfficeAnalyticsResponse;
import com.smartqueue.analytics.dto.OfficerPerformanceResponse;
import com.smartqueue.analytics.dto.QueueStatisticsResponse;
import com.smartqueue.analytics.dto.ReportResponse;
import com.smartqueue.analytics.dto.ServicePerformanceResponse;
import com.smartqueue.analytics.repository.AnalyticsRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
  private final AnalyticsRepository repository;

  public AnalyticsService(AnalyticsRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public DashboardSummaryResponse dashboard() {
    return new DashboardSummaryResponse(
        repository.activeOffices(),
        repository.activeCounters(),
        repository.activeQueues(),
        repository.activeOfficers(),
        repository.citizens(),
        repository.bookingsToday(),
        repository.statusToday("WAITING"),
        repository.statusToday("COMPLETED"),
        repository.statusToday("CANCELLED"),
        repository.statusToday("NO_SHOW"));
  }

  @Transactional(readOnly = true)
  public ReportResponse report(LocalDate from, LocalDate to) {
    return report(from, to, null, null, null, null, null);
  }

  @Transactional(readOnly = true)
  public ReportResponse report(
      LocalDate from,
      LocalDate to,
      Long officeId,
      Long departmentId,
      Long serviceId,
      Long counterId,
      Long officerId) {
    if (to.isBefore(from))
      throw new IllegalArgumentException("The report end date must not precede its start date");
    long bookings =
        repository.countForPeriod(
            from, to, null, officeId, departmentId, serviceId, counterId, officerId);
    long waiting =
        repository.countForPeriod(
            from, to, "WAITING", officeId, departmentId, serviceId, counterId, officerId);
    long completed =
        repository.countForPeriod(
            from, to, "COMPLETED", officeId, departmentId, serviceId, counterId, officerId);
    long cancelled =
        repository.countForPeriod(
            from, to, "CANCELLED", officeId, departmentId, serviceId, counterId, officerId);
    long noShows =
        repository.countForPeriod(
            from, to, "NO_SHOW", officeId, departmentId, serviceId, counterId, officerId);
    return new ReportResponse(
        from,
        to,
        bookings,
        waiting,
        completed,
        cancelled,
        noShows,
        rate(cancelled, bookings),
        rate(noShows, bookings),
        rate(completed, bookings));
  }

  private double rate(long part, long total) {
    return total == 0 ? 0d : Math.round(part * 10000d / total) / 100d;
  }

  @Transactional(readOnly = true)
  public OfficerPerformanceResponse officerPerformance(
      Long officerId, LocalDate from, LocalDate to) {
    long served =
        repository.countForPeriod(from, to, "COMPLETED", null, null, null, null, officerId);
    long skipped =
        repository.countForPeriod(from, to, "SKIPPED", null, null, null, null, officerId);
    long noShows =
        repository.countForPeriod(from, to, "NO_SHOW", null, null, null, null, officerId);
    double average =
        repository.averageMinutes(
            "select coalesce(avg(extract(epoch from (t.completed_at - h.created_at))/60),0) from"
                + " tokens t join queue_history h on h.token_id=t.id and h.new_status='CALLED'"
                + " where t.status='COMPLETED' and h.performed_by=?1 and t.queue_date between ?2"
                + " and ?3",
            officerId,
            from,
            to);
    return new OfficerPerformanceResponse(officerId, served, skipped, noShows, average);
  }

  @Transactional(readOnly = true)
  public CounterAnalyticsResponse counterPerformance(Long counterId, LocalDate from, LocalDate to) {
    long completed =
        repository.countForPeriod(from, to, "COMPLETED", null, null, null, counterId, null);
    double average =
        repository.averageMinutes(
            "select coalesce(avg(extract(epoch from (completed_at - checked_in_at))/60),0) from"
                + " tokens where counter_id=?1 and status='COMPLETED' and queue_date between ?2 and"
                + " ?3",
            counterId,
            from,
            to);
    double openMinutes =
        repository.averageMinutes(
            "with events as (select occurred_at, status, lead(occurred_at) over(order by"
                + " occurred_at) as next_at from counter_status_history where counter_id=?1),"
                + " intervals as (select greatest(occurred_at, cast(?2 as timestamp)) as start_at,"
                + " least(coalesce(next_at, cast(?3 as timestamp) + interval '1 day'), cast(?3 as"
                + " timestamp) + interval '1 day') as end_at from events where status='OPEN')"
                + " select coalesce(sum(greatest(extract(epoch from (end_at-start_at))/60,0)),0)"
                + " from intervals",
            counterId,
            from,
            to);
    double periodMinutes =
        Math.max(1d, java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1) * 24d * 60d;
    return new CounterAnalyticsResponse(
        counterId,
        completed,
        average,
        Math.round(openMinutes * 10000d / periodMinutes) / 100d,
        Math.max(0d, periodMinutes - openMinutes));
  }

  @Transactional(readOnly = true)
  public ServicePerformanceResponse servicePerformance(
      Long serviceId, LocalDate from, LocalDate to) {
    long volume = repository.countForPeriod(from, to, null, null, null, serviceId, null, null);
    double wait =
        repository.averageMinutes(
            "select coalesce(avg(extract(epoch from (checked_in_at - created_at))/60),0) from"
                + " tokens where service_id=?1 and queue_date between ?2 and ?3",
            serviceId,
            from,
            to);
    double serviceTime =
        repository.averageMinutes(
            "select coalesce(avg(extract(epoch from (completed_at - checked_in_at))/60),0) from"
                + " tokens where service_id=?1 and status='COMPLETED' and queue_date between ?2 and"
                + " ?3",
            serviceId,
            from,
            to);
    return new ServicePerformanceResponse(serviceId, volume, wait, serviceTime);
  }

  @Transactional(readOnly = true)
  public OfficeAnalyticsResponse officePerformance(Long officeId, LocalDate from, LocalDate to) {
    long total = repository.countForPeriod(from, to, null, officeId, null, null, null, null);
    long completed =
        repository.countForPeriod(from, to, "COMPLETED", officeId, null, null, null, null);
    long counters =
        repository.count(
            "select count(*) from counters where office_id=?1 and active=true", officeId);
    return new OfficeAnalyticsResponse(officeId, total, counters, rate(completed, total));
  }

  @Transactional(readOnly = true)
  public QueueStatisticsResponse statistics(LocalDate from, LocalDate to) {
    if (to.isBefore(from))
      throw new IllegalArgumentException("The report end date must not precede its start date");
    long total = repository.countForPeriod(from, to, null);
    long completed = repository.countForPeriod(from, to, "COMPLETED");
    long cancelled = repository.countForPeriod(from, to, "CANCELLED");
    long noShows = repository.countForPeriod(from, to, "NO_SHOW");
    double wait =
        repository.averageMinutes(
            "select coalesce(avg(extract(epoch from (checked_in_at-created_at))/60),0) from tokens"
                + " where queue_date between ?1 and ?2",
            from,
            to);
    double serviceTime =
        repository.averageMinutes(
            "select coalesce(avg(extract(epoch from (completed_at-checked_in_at))/60),0) from"
                + " tokens where status='COMPLETED' and queue_date between ?1 and ?2",
            from,
            to);
    long days = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1);
    Integer peakHour =
        number(
            repository.scalar(
                "select extract(hour from created_at) from tokens where queue_date between ?1 and"
                    + " ?2 group by extract(hour from created_at) order by count(*) desc limit 1",
                from,
                to));
    Integer peakDow =
        number(
            repository.scalar(
                "select extract(isodow from queue_date) from tokens where queue_date between ?1 and"
                    + " ?2 group by extract(isodow from queue_date) order by count(*) desc limit 1",
                from,
                to));
    return new QueueStatisticsResponse(
        wait,
        serviceTime,
        total / (double) days,
        peakHour,
        peakDow == null ? null : java.time.DayOfWeek.of(peakDow),
        completed / (double) days,
        rate(cancelled, total),
        rate(noShows, total),
        rate(completed, total));
  }

  private Integer number(Object value) {
    return value == null ? null : ((Number) value).intValue();
  }
}
