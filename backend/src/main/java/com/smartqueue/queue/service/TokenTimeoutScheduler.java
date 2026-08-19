package com.smartqueue.queue.service;

import com.smartqueue.token.entity.Token;
import com.smartqueue.token.enums.TokenStatus;
import com.smartqueue.token.repository.TokenRepository;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Schedules each token's automatic actions at its exact deadline; no polling is used. */
@Component
public class TokenTimeoutScheduler {
  private final QueueEngineService queueEngine;
  private final TokenRepository tokens;
  private final ThreadPoolTaskScheduler taskScheduler;
  private final ConcurrentHashMap<UUID, ScheduledFuture<?>> cancellationTasks =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, ScheduledFuture<?>> completionTasks =
      new ConcurrentHashMap<>();

  public TokenTimeoutScheduler(@Lazy QueueEngineService queueEngine, TokenRepository tokens) {
    this.queueEngine = queueEngine;
    this.tokens = tokens;
    this.taskScheduler = new ThreadPoolTaskScheduler();
    this.taskScheduler.setPoolSize(2);
    this.taskScheduler.setThreadNamePrefix("smartqueue-token-timer-");
    this.taskScheduler.initialize();
  }

  public void scheduleCancellation(Token token) {
    if (token.getAppointmentTime() == null
        || token.getService().getAverageServiceMinutes() < 10
        || token.hasAppeared()) return;
    Instant deadline = appointmentDeadline(token);
    cancelCancellation(token.getPublicId());
    ScheduledFuture<?> task =
        taskScheduler.schedule(
            () -> {
              cancellationTasks.remove(token.getPublicId());
              queueEngine.cancelIfGracePeriodElapsed(token.getPublicId());
            },
            deadline);
    if (task != null) cancellationTasks.put(token.getPublicId(), task);
  }

  public void scheduleCompletion(Token token) {
    if (token.getCheckedInAt() == null) return;
    Instant deadline =
        token.getCheckedInAt().plusSeconds(token.getService().getAverageServiceMinutes() * 60L);
    cancelCompletion(token.getPublicId());
    ScheduledFuture<?> task =
        taskScheduler.schedule(
            () -> {
              completionTasks.remove(token.getPublicId());
              queueEngine.completeIfServiceTimeElapsed(token.getPublicId());
            },
            deadline);
    if (task != null) completionTasks.put(token.getPublicId(), task);
  }

  public void cancelCancellation(UUID tokenId) {
    cancel(cancellationTasks.remove(tokenId));
  }

  public void cancelCompletion(UUID tokenId) {
    cancel(completionTasks.remove(tokenId));
  }

  public void cancelAll(UUID tokenId) {
    cancelCancellation(tokenId);
    cancelCompletion(tokenId);
  }

  @EventListener(ApplicationReadyEvent.class)
  @Transactional(readOnly = true)
  public void restoreTimersAfterRestart() {
    tokens
        .findAllByStatusIn(EnumSet.of(TokenStatus.WAITING, TokenStatus.SKIPPED))
        .forEach(this::scheduleCancellation);
    tokens.findAllByStatusIn(EnumSet.of(TokenStatus.CALLED)).forEach(this::scheduleCompletion);
  }

  private Instant appointmentDeadline(Token token) {
    long graceMinutes = (long) Math.ceil(token.getService().getAverageServiceMinutes() / 2.0);
    return LocalDateTime.of(token.getQueueDate(), token.getAppointmentTime())
        .plusMinutes(graceMinutes)
        .atZone(ZoneId.systemDefault())
        .toInstant();
  }

  private void cancel(ScheduledFuture<?> task) {
    if (task != null) task.cancel(false);
  }

  @PreDestroy
  void shutdown() {
    taskScheduler.shutdown();
  }
}
