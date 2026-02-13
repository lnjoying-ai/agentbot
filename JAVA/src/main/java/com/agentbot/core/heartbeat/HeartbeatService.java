package com.agentbot.core.heartbeat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class HeartbeatService {
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final Path heartbeatFile;
  private final Consumer<String> onHeartbeat;

  public HeartbeatService(Path heartbeatFile, Consumer<String> onHeartbeat) {
    this.heartbeatFile = heartbeatFile;
    this.onHeartbeat = onHeartbeat;
  }

  public void start(long intervalSeconds) {
    scheduler.scheduleAtFixedRate(this::tick, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
  }

  public void stop() {
    scheduler.shutdownNow();
  }

  private void tick() {
    if (!Files.exists(heartbeatFile)) return;
    try {
      String content = Files.readString(heartbeatFile).trim();
      if (content.isEmpty()) return;
      onHeartbeat.accept(content);
    } catch (Exception ignored) {
      // ignore
    }
  }
}

