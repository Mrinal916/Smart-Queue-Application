package com.smartqueue.token.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartqueue.queue.exception.QueueOperationException;
import com.smartqueue.token.enums.TokenStatus;
import org.junit.jupiter.api.Test;

class TokenStateMachineTests {

  private final TokenStateMachine stateMachine = new TokenStateMachine();

  @Test
  void permitsValidLifecycleTransition() {
    stateMachine.validate(TokenStatus.WAITING, TokenStatus.CALLED);
    stateMachine.validate(TokenStatus.CALLED, TokenStatus.COMPLETED);
  }

  @Test
  void rejectsInvalidOrTerminalTransitions() {
    assertThatThrownBy(() -> stateMachine.validate(TokenStatus.WAITING, TokenStatus.COMPLETED))
        .isInstanceOf(QueueOperationException.class);
    assertThatThrownBy(() -> stateMachine.validate(TokenStatus.COMPLETED, TokenStatus.CALLED))
        .isInstanceOf(QueueOperationException.class);
    assertThatThrownBy(() -> stateMachine.validate(TokenStatus.CANCELLED, TokenStatus.CALLED))
        .isInstanceOf(QueueOperationException.class);
  }
}
