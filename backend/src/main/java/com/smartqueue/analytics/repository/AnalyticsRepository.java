package com.smartqueue.analytics.repository;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.springframework.stereotype.Repository;

@Repository
public class AnalyticsRepository {
  private final EntityManager entityManager;

  public AnalyticsRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public long count(String sql, Object... values) {
    var query = entityManager.createNativeQuery(sql);
    for (int index = 0; index < values.length; index++)
      query.setParameter(index + 1, values[index]);
    return ((Number) query.getSingleResult()).longValue();
  }

  public long activeOffices() {
    return count("select count(*) from offices where active = true");
  }

  public long activeCounters() {
    return count("select count(*) from counters where active = true and status = 'OPEN'");
  }

  public long activeQueues() {
    return count(
        "select count(distinct service_id) from tokens where queue_date = ?1 and status in"
            + " ('WAITING','CALLED','SKIPPED')",
        LocalDate.now());
  }

  public long activeOfficers() {
    return count("select count(*) from officer_counter_assignments where released_at is null");
  }

  public long citizens() {
    return count(
        "select count(*) from users u join roles r on r.id=u.role_id where r.name='CITIZEN'");
  }

  public long bookingsToday() {
    return count("select count(*) from tokens where queue_date = ?1", LocalDate.now());
  }

  public long statusToday(String status) {
    return count(
        "select count(*) from tokens where queue_date = ?1 and status = ?2",
        LocalDate.now(),
        status);
  }

  public long countForPeriod(LocalDate from, LocalDate to, String status) {
    return status == null
        ? count("select count(*) from tokens where queue_date between ?1 and ?2", from, to)
        : count(
            "select count(*) from tokens where queue_date between ?1 and ?2 and status=?3",
            from,
            to,
            status);
  }

  public long countForPeriod(
      LocalDate from,
      LocalDate to,
      String status,
      Long officeId,
      Long departmentId,
      Long serviceId,
      Long counterId,
      Long officerId) {
    StringBuilder sql =
        new StringBuilder(
            "select count(*) from tokens t join services s on s.id=t.service_id join departments d"
                + " on d.id=s.department_id where t.queue_date between ?1 and ?2");
    java.util.List<Object> args = new java.util.ArrayList<>(java.util.List.of(from, to));
    if (status != null) {
      sql.append(" and t.status=?").append(args.size() + 1);
      args.add(status);
    }
    if (officeId != null) {
      sql.append(" and t.office_id=?").append(args.size() + 1);
      args.add(officeId);
    }
    if (departmentId != null) {
      sql.append(" and d.id=?").append(args.size() + 1);
      args.add(departmentId);
    }
    if (serviceId != null) {
      sql.append(" and t.service_id=?").append(args.size() + 1);
      args.add(serviceId);
    }
    if (counterId != null) {
      sql.append(" and t.counter_id=?").append(args.size() + 1);
      args.add(counterId);
    }
    if (officerId != null) {
      sql.append(
              " and exists (select 1 from queue_history h where h.token_id=t.id and"
                  + " h.performed_by=?")
          .append(args.size() + 1)
          .append(")");
      args.add(officerId);
    }
    return count(sql.toString(), args.toArray());
  }

  public double averageMinutes(String sql, Object... values) {
    var query = entityManager.createNativeQuery(sql);
    for (int index = 0; index < values.length; index++)
      query.setParameter(index + 1, values[index]);
    Object result = query.getSingleResult();
    return result == null ? 0d : ((Number) result).doubleValue();
  }

  public Object scalar(String sql, Object... values) {
    var query = entityManager.createNativeQuery(sql);
    for (int index = 0; index < values.length; index++)
      query.setParameter(index + 1, values[index]);
    try {
      return query.getSingleResult();
    } catch (jakarta.persistence.NoResultException ignored) {
      return null;
    }
  }
}
