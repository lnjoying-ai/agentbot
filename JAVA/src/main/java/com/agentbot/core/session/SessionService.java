package com.agentbot.core.session;

import java.time.Instant;
import java.util.List;

public class SessionService {
  private final SessionStore store;

  public SessionService(SessionStore store) {
    this.store = store;
  }

  public void appendUserMessage(String sessionKey, String content) {
    store.append(sessionKey, new SessionMessage("user", content, Instant.now()));
  }

  public void appendAssistantMessage(String sessionKey, String content) {
    store.append(sessionKey, new SessionMessage("assistant", content, Instant.now()));
  }

  public List<SessionMessage> getRecent(String sessionKey, int limit) {
    return store.loadRecent(sessionKey, limit);
  }

  public void clear(String sessionKey) {
    store.clear(sessionKey);
  }
}
