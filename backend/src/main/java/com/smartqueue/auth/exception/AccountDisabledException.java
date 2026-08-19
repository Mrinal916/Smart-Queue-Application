package com.smartqueue.auth.exception;

public class AccountDisabledException extends RuntimeException {

  public AccountDisabledException() {
    super("This account has been disabled. Please contact an administrator for assistance.");
  }
}
