package com.agentbot.core.mesh;

public class FlowControl {
  private final int maxInFlight;

  public FlowControl(int maxInFlight) {
    this.maxInFlight = Math.max(1, maxInFlight);
  }

  public int limit(int desired) {
    if (desired <= 0) return 0;
    return Math.min(maxInFlight, desired);
  }

  public int getMaxInFlight() {
    return maxInFlight;
  }
}
