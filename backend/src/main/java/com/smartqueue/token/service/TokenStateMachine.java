package com.smartqueue.token.service;

import com.smartqueue.queue.exception.QueueOperationException;
import com.smartqueue.token.enums.TokenStatus;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TokenStateMachine {

  private static final Map<TokenStatus, Set<TokenStatus>> TRANSITIONS =
      Map.of(
          TokenStatus.WAITING,
              Set.of(TokenStatus.CALLED, TokenStatus.CANCELLED, TokenStatus.NO_SHOW),
          TokenStatus.CALLED,
              Set.of(TokenStatus.SKIPPED, TokenStatus.COMPLETED, TokenStatus.NO_SHOW),
          TokenStatus.SKIPPED, Set.of(TokenStatus.CALLED, TokenStatus.NO_SHOW));

  public void validate(TokenStatus current, TokenStatus next) {
    if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
      throw new QueueOperationException("Token cannot transition from " + current + " to " + next);
    }
  }
}
