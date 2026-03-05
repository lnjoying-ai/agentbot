package com.agentbot.core.channel.impl;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.bus.ExternalMessageBus;
import com.agentbot.core.bus.MessageEnvelope;
import com.agentbot.core.bus.events.OutboundMessage;
import com.agentbot.core.channel.ChannelAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.CopyOnWriteArrayList;

public class MaixCamChannelAdapter implements ChannelAdapter {
  private static final Logger log = LoggerFactory.getLogger(MaixCamChannelAdapter.class);

  private final ExternalMessageBus messageBus;
  private final AgentbotProperties.MaixCam config;
  private final ObjectMapper mapper = new ObjectMapper();
  private final List<Socket> connections = new CopyOnWriteArrayList<>();
  private volatile boolean running = false;
  private Thread acceptThread;

  public MaixCamChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    this.messageBus = messageBus;
    this.config = properties.getChannels().getMaixcam();
  }

  @Override
  public String name() {
    return "maixcam";
  }

  @Override
  public void start() {
    if (!config.isEnabled()) {
      log.info("maixcam channel disabled");
      return;
    }
    running = true;
    acceptThread = new Thread(this::acceptLoop, "maixcam-accept");
    acceptThread.setDaemon(true);
    acceptThread.start();
    log.info("maixcam channel started: {}:{}", config.getHost(), config.getPort());
  }

  @Override
  public void stop() {
    running = false;
    if (acceptThread != null) {
      acceptThread.interrupt();
    }
    for (Socket socket : connections) {
      try {
        socket.close();
      } catch (Exception ignored) {
      }
    }
    connections.clear();
    log.info("maixcam channel stopped");
  }

  @Override
  public void send(OutboundMessage message) {
    if (!config.isEnabled()) return;
    if (connections.isEmpty()) {
      log.warn("maixcam no active device connection, skip send");
      return;
    }
    String payload;
    try {
      payload = mapper.writeValueAsString(Map.of("type", "command", "text", message.getContent()));
    } catch (Exception e) {
      log.warn("maixcam encode command failed", e);
      return;
    }
    for (Socket socket : connections) {
      try {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        writer.println(payload);
      } catch (Exception ignored) {
      }
    }
    log.info("maixcam send: targets={} length={}", connections.size(),
        message.getContent() == null ? 0 : message.getContent().length());
  }


  private void acceptLoop() {
    try (ServerSocket server = new ServerSocket()) {
      server.bind(new InetSocketAddress(config.getHost(), config.getPort()));
      while (running) {
        Socket socket = server.accept();
        connections.add(socket);
        log.info("maixcam device connected: remote={}", socket.getRemoteSocketAddress());
        Thread t = new Thread(() -> handleConnection(socket), "maixcam-conn");
        t.setDaemon(true);
        t.start();
      }

    } catch (Exception e) {
      if (running) {
        log.warn("maixcam accept failed", e);
      }
    }
  }

  private void handleConnection(Socket socket) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        handleMessage(line);
      }
    } catch (Exception ignored) {
    } finally {
      connections.remove(socket);
      log.info("maixcam device disconnected: remote={}", socket.getRemoteSocketAddress());
      try {
        socket.close();
      } catch (Exception ignored) {
      }
    }
  }


  private void handleMessage(String payload) {
    try {
      JsonNode root = mapper.readTree(payload);
      String type = root.path("type").asText("");
      String senderId = root.path("deviceId").asText("device");
      String chatId = root.path("chatId").asText(senderId);
      String content = root.path("content").asText("");
      if (content.isBlank()) {
        if ("person_detected".equals(type)) {
          content = "person_detected";
        } else if ("status".equals(type)) {
          content = root.path("status").asText("status");
        }
      }
      if (content.isBlank()) return;
      log.info("maixcam inbound: chatId={} senderId={} type={} length={}", chatId, senderId, type, content.length());
      HashMap<String, Object> metadata = new HashMap<>();
      metadata.put(MessageEnvelope.META_ACCOUNT_ID, "default");
      metadata.put(MessageEnvelope.META_PEER_KIND, "dm");
      metadata.put(MessageEnvelope.META_PEER_ID, chatId);
      metadata.put("raw", root);
      MessageEnvelope inbound = MessageEnvelope.externalInbound("maixcam", senderId, chatId, content, metadata);
      messageBus.publish(inbound);

    } catch (Exception e) {
      log.warn("maixcam message parse failed", e);
    }
  }

}
