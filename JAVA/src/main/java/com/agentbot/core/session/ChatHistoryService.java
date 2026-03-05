package com.agentbot.core.session;

import com.agentbot.core.agent.AgentRegistry;
import com.agentbot.core.util.ConfigPathResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChatHistoryService {
  private static final Logger log = LoggerFactory.getLogger(ChatHistoryService.class);
  public record SessionInfo(String chatId, String channel, String agentId, Instant lastMessageAt, int messageCount) {}

  public record MessageInfo(String id, String role, String content, Instant timestamp) {}

  private record SessionKeyParts(String agentId, String channel, String accountId, String peerKind, String peerId) {}


  private final Path workspaceDir;
  private final AgentRegistry registry;
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  public ChatHistoryService(AgentRegistry registry) {
    this.workspaceDir = ConfigPathResolver.resolveUserDataDir().resolve("workspace").toAbsolutePath().normalize();
    this.registry = registry;
  }


  public List<SessionInfo> listSessions(String channel, String agentId, int limit) {
    String normalizedAgent = normalizeAgentId(agentId);
    Path dir = sessionsDir(normalizedAgent);
    if (!Files.exists(dir)) {
      log.debug("Sessions dir not found: {}", dir);
      return Collections.emptyList();
    }

    List<SessionInfo> sessions = new ArrayList<>();
    try {
      Files.list(dir)
          .filter(path -> path.getFileName().toString().endsWith(".jsonl"))
          .forEach(path -> {
            String decoded = decodeSessionKey(path.getFileName().toString().replace(".jsonl", ""));
            SessionKeyParts parts = parseSessionKey(decoded, normalizedAgent);
            if (channel != null && !channel.isBlank() && !channel.equalsIgnoreCase(parts.channel())) return;
            SessionInfo info = buildSessionInfo(path, parts.peerId(), parts.channel(), parts.agentId());
            if (info != null) sessions.add(info);
          });
    } catch (IOException error) {
      log.warn("List sessions failed: dir={}", dir, error);
      return Collections.emptyList();
    }


    sessions.sort(Comparator.comparing(SessionInfo::lastMessageAt, Comparator.nullsLast(Comparator.reverseOrder())));
    if (limit > 0 && sessions.size() > limit) {
      return sessions.subList(0, limit);
    }
    return sessions;
  }

  public List<MessageInfo> listMessages(String channel, String chatId, String agentId, int limit, Instant before) {
    String normalizedAgent = normalizeAgentId(agentId);
    String sessionKey = buildSessionKey(normalizedAgent, channel, "default", "dm", chatId);
    Path file = sessionFile(normalizedAgent, sessionKey);
    if (!Files.exists(file)) {
      try {
        Files.createDirectories(file.getParent());
        Files.createFile(file);
        log.info("Session file created: {}", file);
      } catch (FileAlreadyExistsException ignored) {
      } catch (IOException error) {
        log.warn("Create session file failed: {}", file, error);
        throw new RuntimeException("Create session file failed: " + file, error);
      }
      return Collections.emptyList();
    }



    List<SessionMessage> messages = readMessages(file);
    if (messages.isEmpty()) return Collections.emptyList();

    List<MessageInfo> result = new ArrayList<>();
    int index = 0;
    for (SessionMessage message : messages) {
      if (message == null || message.getTimestamp() == null) {
        index++;
        continue;
      }
      if (before != null && !message.getTimestamp().isBefore(before)) {
        index++;
        continue;
      }
      result.add(new MessageInfo(buildMessageId(message, index), message.getRole(), message.getContent(), message.getTimestamp()));
      index++;
    }

    if (limit > 0 && result.size() > limit) {
      return result.subList(result.size() - limit, result.size());
    }
    return result;
  }

  private SessionInfo buildSessionInfo(Path file, String chatId, String channel, String agentId) {
    List<SessionMessage> messages = readMessages(file);
    if (messages.isEmpty()) return null;
    SessionMessage last = messages.get(messages.size() - 1);
    return new SessionInfo(chatId, channel, agentId, last.getTimestamp(), messages.size());
  }

  private List<SessionMessage> readMessages(Path file) {
    try {
      List<String> lines = Files.readAllLines(file);
      List<SessionMessage> messages = new ArrayList<>();
      for (String line : lines) {
        if (line == null || line.isBlank()) continue;
        messages.add(mapper.readValue(line, SessionMessage.class));
      }
      return messages;
    } catch (IOException error) {
      log.warn("Read session messages failed: file={}", file, error);
      return Collections.emptyList();
    }
  }


  private Path sessionFile(String agentId, String sessionKey) {
    String safe = sessionKey.replace(":", "_");
    return sessionsDir(agentId).resolve(safe + ".jsonl");
  }

  private Path sessionsDir(String agentId) {
    return workspaceDir.resolve("agents").resolve(agentId).resolve("sessions");
  }

  private String normalizeAgentId(String agentId) {
    if (agentId == null || agentId.isBlank()) return "default";
    if (registry != null && registry.hasAgent(agentId)) return agentId;
    return "default";
  }

  private String normalizeToken(String value, String fallback) {
    String trimmed = value == null ? "" : value.trim();
    if (!trimmed.isBlank()) return trimmed.toLowerCase();
    return fallback == null ? "" : fallback;
  }

  private String normalizeId(String value, String fallback) {
    String trimmed = value == null ? "" : value.trim();
    if (!trimmed.isBlank()) return trimmed.toLowerCase();
    return fallback == null ? "" : fallback;
  }

  private String buildSessionKey(String agentId, String channel, String accountId, String peerKind, String peerId) {
    String safeChannel = normalizeToken(channel, "web");
    String safeAccount = normalizeToken(accountId, "default");
    String safePeerKind = normalizeToken(peerKind, "dm");
    String safePeerId = normalizeId(peerId, "unknown");
    return "agent:" + normalizeToken(agentId, "default") + ":" + safeChannel + ":" + safeAccount + ":" + safePeerKind + ":" + safePeerId;
  }

  private SessionKeyParts parseSessionKey(String sessionKey, String fallbackAgentId) {
    if (sessionKey == null || sessionKey.isBlank()) {
      return new SessionKeyParts(normalizeToken(fallbackAgentId, "default"), "web", "default", "dm", "unknown");
    }
    String[] parts = sessionKey.split(":");
    if (parts.length >= 6 && "agent".equalsIgnoreCase(parts[0])) {
      String agentId = normalizeToken(parts[1], fallbackAgentId);
      String channel = normalizeToken(parts[2], "web");
      String accountId = normalizeToken(parts[3], "default");
      String peerKind = normalizeToken(parts[4], "dm");
      String peerId = normalizeId(String.join(":", java.util.Arrays.copyOfRange(parts, 5, parts.length)), "unknown");
      return new SessionKeyParts(agentId, channel, accountId, peerKind, peerId);
    }
    if (parts.length >= 4) {
      String channel = normalizeToken(parts[0], "web");
      String accountId = normalizeToken(parts[1], "default");
      String peerKind = normalizeToken(parts[2], "dm");
      String peerId = normalizeId(String.join(":", java.util.Arrays.copyOfRange(parts, 3, parts.length)), "unknown");
      return new SessionKeyParts(normalizeToken(fallbackAgentId, "default"), channel, accountId, peerKind, peerId);
    }
    if (parts.length >= 2) {
      String channel = normalizeToken(parts[0], "web");
      String peerId = normalizeId(String.join(":", java.util.Arrays.copyOfRange(parts, 1, parts.length)), "unknown");
      return new SessionKeyParts(normalizeToken(fallbackAgentId, "default"), channel, "default", "dm", peerId);
    }
    String peerId = normalizeId(parts[0], "unknown");
    return new SessionKeyParts(normalizeToken(fallbackAgentId, "default"), "web", "default", "dm", peerId);
  }

  private String decodeSessionKey(String encoded) {
    return encoded.replace("_", ":");
  }


  private String buildMessageId(SessionMessage message, int index) {
    return (message.getTimestamp() == null ? "" : message.getTimestamp().toEpochMilli()) + "-" + index;
  }
}
