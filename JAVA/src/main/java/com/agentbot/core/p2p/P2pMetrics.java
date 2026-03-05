package com.agentbot.core.p2p;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class P2pMetrics {
  private static final AtomicLong connectionsOpened = new AtomicLong();
  private static final AtomicLong connectionsClosed = new AtomicLong();
  private static final AtomicLong handshakesCompleted = new AtomicLong();
  private static final AtomicLong messagesReceived = new AtomicLong();
  private static final AtomicLong messagesSent = new AtomicLong();
  private static final AtomicLong ackCount = new AtomicLong();
  private static final AtomicLong nackCount = new AtomicLong();
  private static final AtomicLong retryCount = new AtomicLong();
  private static final AtomicLong getaddrSent = new AtomicLong();
  private static final AtomicLong getaddrSkipped = new AtomicLong();
  private static final AtomicLong addrReceived = new AtomicLong();
  private static final AtomicLong addrAccepted = new AtomicLong();
  private static final AtomicLong addrInvalid = new AtomicLong();
  private static final AtomicLong addrEmpty = new AtomicLong();
  private static final AtomicLong skillInvSent = new AtomicLong();
  private static final AtomicLong skillDataStored = new AtomicLong();
  private static final AtomicLong skillDataRejected = new AtomicLong();

  private P2pMetrics() {}



  public static void recordOpen() {
    connectionsOpened.incrementAndGet();
  }

  public static void recordClose() {
    connectionsClosed.incrementAndGet();
  }

  public static void recordHandshake() {
    handshakesCompleted.incrementAndGet();
  }

  public static void recordReceive() {
    messagesReceived.incrementAndGet();
  }

  public static void recordSend() {
    messagesSent.incrementAndGet();
  }

  public static void recordAck() {
    ackCount.incrementAndGet();
  }

  public static void recordNack() {
    nackCount.incrementAndGet();
  }

  public static void recordRetry() {
    retryCount.incrementAndGet();
  }

  public static void recordGetAddrSent() {
    getaddrSent.incrementAndGet();
  }

  public static void recordGetAddrSkipped() {
    getaddrSkipped.incrementAndGet();
  }

  public static void recordAddrReceived(int count) {
    if (count <= 0) return;
    addrReceived.addAndGet(count);
  }

  public static void recordAddrAccepted(int count) {
    if (count <= 0) return;
    addrAccepted.addAndGet(count);
  }

  public static void recordAddrInvalid(int count) {
    if (count <= 0) return;
    addrInvalid.addAndGet(count);
  }

  public static void recordAddrEmpty() {
    addrEmpty.incrementAndGet();
  }

  public static void recordSkillInvSent(int count) {
    if (count <= 0) return;
    skillInvSent.addAndGet(count);
  }

  public static void recordSkillDataStored() {
    skillDataStored.incrementAndGet();
  }

  public static void recordSkillDataRejected() {
    skillDataRejected.incrementAndGet();
  }

  public static Map<String, Object> snapshot() {
    return Map.ofEntries(
        Map.entry("connectionsOpened", connectionsOpened.get()),
        Map.entry("connectionsClosed", connectionsClosed.get()),
        Map.entry("handshakesCompleted", handshakesCompleted.get()),
        Map.entry("messagesReceived", messagesReceived.get()),
        Map.entry("messagesSent", messagesSent.get()),
        Map.entry("acks", ackCount.get()),
        Map.entry("nacks", nackCount.get()),
        Map.entry("retries", retryCount.get()),
        Map.entry("getaddrSent", getaddrSent.get()),
        Map.entry("getaddrSkipped", getaddrSkipped.get()),
        Map.entry("addrReceived", addrReceived.get()),
        Map.entry("addrAccepted", addrAccepted.get()),
        Map.entry("addrInvalid", addrInvalid.get()),
        Map.entry("addrEmpty", addrEmpty.get()),
        Map.entry("skillInvSent", skillInvSent.get()),
        Map.entry("skillDataStored", skillDataStored.get()),
        Map.entry("skillDataRejected", skillDataRejected.get())
    );
  }



}
