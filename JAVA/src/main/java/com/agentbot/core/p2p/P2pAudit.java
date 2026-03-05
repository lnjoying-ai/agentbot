package com.agentbot.core.p2p;

import org.slf4j.Logger;

public final class P2pAudit {
  private P2pAudit() {}

  public static void warn(Logger log, String reason, String detail) {
    if (log == null) return;
    log.warn("P2P audit: reason={}, detail={}", reason, detail);
  }

  public static void info(Logger log, String event, String detail) {
    if (log == null) return;
    log.info("P2P audit: event={}, detail={}", event, detail);
  }
}
