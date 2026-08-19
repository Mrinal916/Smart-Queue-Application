package com.smartqueue.queue.repository;

import com.smartqueue.queue.entity.QueueHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QueueHistoryRepository extends JpaRepository<QueueHistory, Long> {
  @Modifying
  @Query("DELETE FROM QueueHistory qh WHERE qh.performedBy.id = :actorId")
  void deleteAllByActorId(@Param("actorId") Long actorId);

  @Modifying
  @Query("DELETE FROM QueueHistory qh WHERE qh.token.citizen.id = :citizenId")
  void deleteAllByTokenCitizenId(@Param("citizenId") Long citizenId);
}
