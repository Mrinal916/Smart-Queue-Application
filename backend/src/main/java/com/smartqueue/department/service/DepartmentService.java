package com.smartqueue.department.service;

import com.smartqueue.common.exception.*;
import com.smartqueue.department.dto.*;
import com.smartqueue.department.entity.Department;
import com.smartqueue.department.repository.DepartmentRepository;
import com.smartqueue.office.service.OfficeService;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {
  private final DepartmentRepository repository;
  private final OfficeService offices;

  public DepartmentService(DepartmentRepository repository, OfficeService offices) {
    this.repository = repository;
    this.offices = offices;
  }

  @Transactional
  public DepartmentResponse create(DepartmentRequest r) {
    return map(repository.save(new Department(offices.get(r.officeId()), r.name().trim())));
  }

  @Transactional
  public DepartmentResponse update(UUID id, DepartmentRequest r) {
    Department d = get(id);
    d.update(offices.get(r.officeId()), r.name().trim(), true);
    return map(d);
  }

  @Transactional
  public void delete(UUID id) {
    Department d = get(id);
    if (repository.existsByOfficePublicIdAndActiveTrue(id))
      throw new BusinessConflictException(
          "Department cannot be deleted while active services exist");
    d.deactivate();
  }

  @Transactional(readOnly = true)
  public DepartmentResponse getResponse(UUID id) {
    return map(get(id));
  }

  @Transactional(readOnly = true)
  public List<DepartmentResponse> list(UUID officeId) {
    offices.get(officeId);
    return repository.findAllByOfficePublicIdAndActiveTrueOrderByName(officeId).stream()
        .map(this::map)
        .toList();
  }

  public Department get(UUID id) {
    return repository
        .findByPublicIdAndActiveTrue(id)
        .orElseThrow(() -> new ResourceNotFoundException("Department", id));
  }

  private DepartmentResponse map(Department d) {
    return new DepartmentResponse(
        d.getPublicId(), d.getOffice().getPublicId(), d.getName(), d.isActive());
  }
}
