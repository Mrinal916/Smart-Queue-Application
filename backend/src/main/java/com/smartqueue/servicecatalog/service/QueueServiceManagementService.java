package com.smartqueue.servicecatalog.service;

import com.smartqueue.common.exception.BusinessConflictException;
import com.smartqueue.common.exception.ResourceNotFoundException;
import com.smartqueue.department.service.DepartmentService;
import com.smartqueue.servicecatalog.dto.QueueServiceRequest;
import com.smartqueue.servicecatalog.dto.QueueServiceResponse;
import com.smartqueue.servicecatalog.entity.QueueService;
import com.smartqueue.servicecatalog.repository.QueueServiceRepository;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueueServiceManagementService {

  private final QueueServiceRepository repository;
  private final DepartmentService departments;

  public QueueServiceManagementService(
      QueueServiceRepository repository, DepartmentService departments) {
    this.repository = repository;
    this.departments = departments;
  }

  @Transactional
  public QueueServiceResponse create(QueueServiceRequest request) {
    validateTimes(request);
    QueueService queueService =
        new QueueService(
            departments.get(request.departmentId()),
            request.name().trim(),
            request.startTime(),
            request.endTime(),
            request.breakStartTime(),
            request.breakEndTime(),
            request.dailyCapacity(),
            calculatedServiceMinutes(request),
            openDays(request));
    return map(repository.save(queueService));
  }

  @Transactional
  public QueueServiceResponse update(UUID id, QueueServiceRequest request) {
    validateTimes(request);
    QueueService queueService = get(id);
    queueService.update(
        departments.get(request.departmentId()),
        request.name().trim(),
        request.startTime(),
        request.endTime(),
        request.breakStartTime(),
        request.breakEndTime(),
        request.dailyCapacity(),
        calculatedServiceMinutes(request),
        openDays(request),
        true);
    return map(queueService);
  }

  @Transactional
  public void delete(UUID id) {
    get(id).deactivate();
  }

  @Transactional(readOnly = true)
  public QueueServiceResponse getResponse(UUID id) {
    return map(get(id));
  }

  @Transactional(readOnly = true)
  public List<QueueServiceResponse> list(UUID departmentId) {
    departments.get(departmentId);
    return repository.findAllByDepartmentPublicIdAndActiveTrueOrderByName(departmentId).stream()
        .map(this::map)
        .toList();
  }

  public QueueService get(UUID id) {
    return repository
        .findByPublicIdAndActiveTrue(id)
        .orElseThrow(() -> new ResourceNotFoundException("Service", id));
  }

  private void validateTimes(QueueServiceRequest request) {
    if (!request.startTime().isBefore(request.endTime())) {
      throw new BusinessConflictException("Service start time must be before end time");
    }
    if ((request.breakStartTime() == null) != (request.breakEndTime() == null)) {
      throw new BusinessConflictException(
          "Set both break start and break end times, or leave both empty");
    }
    if (request.breakStartTime() != null
        && (!request.startTime().isBefore(request.breakStartTime())
            || !request.breakStartTime().isBefore(request.breakEndTime())
            || !request.breakEndTime().isBefore(request.endTime()))) {
      throw new BusinessConflictException(
          "Break time must fall completely within the service hours");
    }
  }

  /**
   * Divides the service window across capacity and rounds the slot interval to the nearest five
   * minutes.
   */
  private int calculatedServiceMinutes(QueueServiceRequest request) {
    long minutes = Duration.between(request.startTime(), request.endTime()).toMinutes();
    return Math.max(5, (int) (Math.round((minutes / (double) request.dailyCapacity()) / 5) * 5));
  }

  private Set<DayOfWeek> openDays(QueueServiceRequest request) {
    return request.openDays() == null ? EnumSet.allOf(DayOfWeek.class) : request.openDays();
  }

  private QueueServiceResponse map(QueueService queueService) {
    return new QueueServiceResponse(
        queueService.getPublicId(),
        queueService.getDepartment().getPublicId(),
        queueService.getName(),
        queueService.getStartTime(),
        queueService.getEndTime(),
        queueService.getBreakStartTime(),
        queueService.getBreakEndTime(),
        queueService.getDailyCapacity(),
        queueService.getAverageServiceMinutes(),
        queueService.getOpenDays(),
        queueService.isActive());
  }
}
