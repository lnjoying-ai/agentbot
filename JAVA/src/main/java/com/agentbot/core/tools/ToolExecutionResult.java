package com.agentbot.core.tools;

public class ToolExecutionResult {
  private final boolean ok;
  private final String output;

  public ToolExecutionResult(boolean ok, String output) {
    this.ok = ok;
    this.output = output;
  }

  public boolean isOk() {
    return ok;
  }

  public String getOutput() {
    return output;
  }
}
