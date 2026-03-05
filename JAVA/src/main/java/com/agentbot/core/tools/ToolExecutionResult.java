package com.agentbot.core.tools;

public class ToolExecutionResult {
  public enum Status {
    OK, FAILED, PENDING_APPROVAL
  }

  private final boolean ok;
  private final String output;
  private final Status status;

  public ToolExecutionResult(boolean ok, String output) {
    this.ok = ok;
    this.output = output;
    this.status = ok ? Status.OK : Status.FAILED;
  }

  public ToolExecutionResult(Status status, String output) {
    this.status = status;
    this.ok = status == Status.OK;
    this.output = output;
  }

  public boolean isOk() {
    return ok;
  }

  public String getOutput() {
    return output;
  }

  public Status getStatus() {
    return status;
  }

  public boolean isPendingApproval() {
    return status == Status.PENDING_APPROVAL;
  }
}
