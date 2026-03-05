package com.agentbot.core.protocol;

public class FrameDecodeException extends RuntimeException {
  public enum Reason {
    NULL_FRAME,
    SHORT_FRAME,
    NEGATIVE_LENGTH,
    LENGTH_OVERFLOW,
    LENGTH_EXCEEDED,
    LENGTH_MISMATCH,
    BUFFER_UNDERFLOW
  }


  private final Reason reason;

  public FrameDecodeException(Reason reason, String message) {
    super(message);
    this.reason = reason;
  }

  public FrameDecodeException(Reason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = reason;
  }

  public Reason getReason() {
    return reason;
  }
}
