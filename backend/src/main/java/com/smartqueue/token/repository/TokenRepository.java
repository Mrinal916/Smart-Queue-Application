package com.smartqueue.token.repository;

import com.smartqueue.token.entity.Token;
import com.smartqueue.token.enums.TokenStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRepository extends JpaRepository<Token, Long> {
  Optional<Token> findByPublicId(UUID id);

  Optional<Token> findByCitizenIdAndBookingKey(Long citizenId, String bookingKey);

  Optional<Token> findTopByCitizenIdAndStatusInOrderByCreatedAtDesc(
      Long citizenId, Collection<TokenStatus> statuses);

  Page<Token> findAllByCitizenIdOrderByCreatedAtDesc(Long citizenId, Pageable pageable);

  Page<Token> findAllByStatusIn(Collection<TokenStatus> statuses, Pageable pageable);

  boolean existsByCitizenIdAndOfficeIdAndQueueDateAndStatusIn(
      Long citizenId, Long officeId, LocalDate queueDate, Collection<TokenStatus> statuses);

  boolean existsByServiceIdAndQueueDateAndAppointmentTimeAndStatusIn(
      Long serviceId,
      LocalDate queueDate,
      LocalTime appointmentTime,
      Collection<TokenStatus> statuses);

  List<Token> findAllByServiceIdAndQueueDateAndStatusIn(
      Long serviceId, LocalDate queueDate, Collection<TokenStatus> statuses);

  long countByServiceIdAndQueueDate(Long serviceId, LocalDate queueDate);

  long countByServicePublicIdAndQueueDateAndStatus(
      UUID serviceId, LocalDate date, TokenStatus status);

  Optional<Token> findTopByServiceIdAndQueueDateOrderByTokenNumberDesc(
      Long serviceId, LocalDate queueDate);

  Optional<Token> findByCounterPublicIdAndStatus(UUID counterId, TokenStatus status);

  Optional<Token> findTopByServicePublicIdAndQueueDateAndStatusOrderByTokenNumber(
      UUID serviceId, LocalDate date, TokenStatus status);

  List<Token> findAllByServicePublicIdAndQueueDateAndStatusOrderByAgePriorityDescTokenNumberAsc(
      UUID serviceId, LocalDate queueDate, TokenStatus status);

  List<Token> findAllByServicePublicIdAndQueueDateOrderByAgePriorityDescTokenNumberAsc(
      UUID serviceId, LocalDate queueDate);

  List<Token> findAllByServiceIdAndQueueDateAndStatusInOrderByAgePriorityDescTokenNumberAsc(
      Long serviceId, LocalDate queueDate, Collection<TokenStatus> statuses);

  List<Token> findAllByQueueDateAndStatusIn(LocalDate queueDate, Collection<TokenStatus> statuses);

  List<Token> findAllByStatusIn(Collection<TokenStatus> statuses);

  List<Token> findAllByOfficePublicIdAndStatus(UUID officeId, TokenStatus status);

  void deleteAllByCitizenId(Long citizenId);
}
